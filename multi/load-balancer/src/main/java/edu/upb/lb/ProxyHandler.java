package edu.upb.lb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProxyHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);
    private static final int LB_PORT = Integer.parseInt(System.getenv().getOrDefault("LB_PORT", "1915"));
    private static final int CONNECT_TIMEOUT_MS = Integer
            .parseInt(System.getenv().getOrDefault("LB_CONNECT_TIMEOUT_MS", "10000"));
    private static final int READ_TIMEOUT_MS = Integer
            .parseInt(System.getenv().getOrDefault("LB_READ_TIMEOUT_MS", "5000"));
    private static final long WORKER_RETRY_INTERVAL_MS = Long
            .parseLong(System.getenv().getOrDefault("LB_WORKER_RETRY_INTERVAL_MS", "10000"));

    @Override
    public void handle(HttpExchange he) throws IOException {
        addBaseHeaders(he.getResponseHeaders());
        String path = he.getRequestURI().getPath();

        if ("/monitor/health".equals(path)) {
            handleMonitor(he, false);
            return;
        }
        if ("/monitor/metrics".equals(path)) {
            handleMonitor(he, true);
            return;
        }
        if ("/register".equals(path)) {
            handleRegister(he);
            return;
        }
        if ("/tickets".equals(path)) {
            handleProxy(he, false);
            return;
        }
        if (path.startsWith("/tm")) {
            handleProxy(he, true);
            return;
        }

        sendResponse(he, 404,
                "{\"status\":\"NOK\",\"message\":\"Ruta no encontrada\"}".getBytes(StandardCharsets.UTF_8));
    }

    private void handleMonitor(HttpExchange he, boolean metricsMode) throws IOException {
        JsonObject out;
        if (metricsMode) {
            out = MonitorStore.getInstance().metrics("load-balancer", LB_PORT);
        } else {
            out = MonitorStore.getInstance().health("load-balancer", LB_PORT);
        }
        out.addProperty("registered_backends", BackendRegistry.getInstance().list().size());
        out.addProperty("in_service_backends", BackendRegistry.getInstance().countInService());
        out.add("balancing_backends", toJsonArray(BackendRegistry.getInstance().listBalancing()));
        out.add("verification_backends", toJsonArray(BackendRegistry.getInstance().list()));
        out.add("backend_status", BackendRegistry.getInstance().statusSnapshot());
        sendResponse(he, 200, out.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void handleRegister(HttpExchange he) throws IOException {
        String method = he.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            sendResponse(he, 200, "{}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        if ("GET".equals(method)) {
            List<String> verificationList = BackendRegistry.getInstance().list();
            List<String> balancingList = BackendRegistry.getInstance().listBalancing();
            JsonObject out = new JsonObject();
            out.add("backends", toJsonArray(verificationList));
            out.add("verification_backends", toJsonArray(verificationList));
            out.add("balancing_backends", toJsonArray(balancingList));
            out.addProperty("count", verificationList.size());
            out.addProperty("in_service_count", BackendRegistry.getInstance().countInService());
            out.add("backend_status", BackendRegistry.getInstance().statusSnapshot());
            sendResponse(he, 200, out.toString().getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (!"POST".equals(method)) {
            sendResponse(he, 405,
                    "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        try {
            JsonObject body = JsonParser.parseReader(
                    new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();
            String backend;
            if (body.has("url")) {
                String rawUrl = body.get("url").getAsString();
                backend = BackendRegistry.getInstance().registerUrl(rawUrl);
            } else {
                String ip = body.has("ip") ? body.get("ip").getAsString() : "localhost";
                int port = body.has("port") ? body.get("port").getAsInt() : -1;
                if (port < 1 || port > 65535) {
                    sendResponse(he, 400,
                            "{\"status\":\"NOK\",\"message\":\"Puerto invalido\"}".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                backend = BackendRegistry.getInstance().register(ip, port);
            }

            JsonObject out = new JsonObject();
            out.addProperty("status", "OK");
            out.addProperty("backend", backend);
            out.addProperty("count", BackendRegistry.getInstance().list().size());
            out.addProperty("in_service_count", BackendRegistry.getInstance().countInService());
            sendResponse(he, 200, out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            sendResponse(he, 400, "{\"status\":\"NOK\",\"message\":\"URL invalida\"}".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            sendResponse(he, 400,
                    "{\"status\":\"NOK\",\"message\":\"Body invalido\"}".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleProxy(HttpExchange he, boolean stripTmPrefix) throws IOException {
        long start = System.nanoTime();
        boolean error = false;
        String backend = BackendRegistry.getInstance().nextBackend();
        String method = he.getRequestMethod();
        String requestPath = he.getRequestURI().toString();
        String path = he.getRequestURI().getPath();
        if (stripTmPrefix) {
            path = path.replaceFirst("^/tm", "");
        }
        if (path.isEmpty()) {
            path = "/";
        }
        String query = he.getRequestURI().getRawQuery();
        String monitorRoute = stripTmPrefix ? "/tm" : path;

        if (backend == null) {
            error = true;
            byte[] b = "{\"status\":\"NOK\",\"message\":\"No hay backends en servicio\"}"
                    .getBytes(StandardCharsets.UTF_8);
            sendResponse(he, 503, b);
            MonitorStore.getInstance().record(monitorRoute, "none", start, true);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.warn("Finished {} in {} ms (backend=none, error=true)", requestPath, elapsedMs);
            return;
        }

        String target = backend + path + (query != null ? "?" + query : "");
        byte[] requestBody = readBodyIfNeeded(he, method);
        String requestUsername = "POST".equals(method) ? extractUsername(requestBody) : null;
        log.info("Request {} {} routed to {}", method, requestPath, target);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(READ_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .build();

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            try {
                ForwardResult first = executeOnce(client, he.getRequestHeaders(), method, target, requestBody);
                if (first.statusCode >= 400) {
                    error = true;
                }
                sendResponse(he, first.statusCode, first.body);
                log.info("Response {} for {}", first.statusCode, requestPath);
            } catch (Exception firstError) {
                String firstErrorType = classifyErrorType(firstError);

                if ("POST".equals(method) && "READ".equals(firstErrorType)) {
                    registryRuntimeFailure(backend, firstErrorType);
                    log.warn("Read timeout for {}. Checking if first attempt was saved.", requestPath);

                    if (wasSavedAfterTimeout(client, backend, requestUsername)) {
                        byte[] ok = "{\"status\":\"OK\",\"message\":\"Guardado confirmado tras timeout\"}"
                                .getBytes(StandardCharsets.UTF_8);
                        sendResponse(he, 201, ok);
                        log.info("Recovered {} as success without retry (already saved).", requestPath);
                        return;
                    }

                    log.warn("Not found after first timeout. Retrying {} once.", requestPath);
                    try {
                        ForwardResult second = executeOnce(client, he.getRequestHeaders(), method, target, requestBody);
                        if (second.statusCode >= 400) {
                            error = true;
                        }
                        sendResponse(he, second.statusCode, second.body);
                        log.info("Retry response {} for {}", second.statusCode, requestPath);
                        return;
                    } catch (Exception secondError) {
                        String secondErrorType = classifyErrorType(secondError);
                        registryRuntimeFailure(backend, secondErrorType);
                        if (wasSavedAfterTimeout(client, backend, requestUsername)) {
                            byte[] ok = "{\"status\":\"OK\",\"message\":\"Guardado confirmado tras reintento\"}"
                                    .getBytes(StandardCharsets.UTF_8);
                            sendResponse(he, 201, ok);
                            log.info("Recovered {} as success after second failure (saved detected).", requestPath);
                            return;
                        }
                        error = true;
                        sendProxyError(he, secondErrorType, secondError, requestPath, backend, start);
                        return;
                    }
                }

                registryRuntimeFailure(backend, firstErrorType);
                error = true;
                sendProxyError(he, firstErrorType, firstError, requestPath, backend, start);
            }
        } finally {
            MonitorStore.getInstance().record(monitorRoute, backend, start, error);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Finished {} in {} ms (backend={}, error={})", requestPath, elapsedMs, backend, error);
        }
    }

    private ForwardResult executeOnce(CloseableHttpClient client, Headers in, String method, String target,
            byte[] requestBody) throws IOException {
        HttpRequestBase req;
        switch (method) {
            case "POST":
                HttpPost post = new HttpPost(target);
                post.setEntity(new ByteArrayEntity(requestBody));
                req = post;
                break;
            case "PUT":
                HttpPut put = new HttpPut(target);
                put.setEntity(new ByteArrayEntity(requestBody));
                req = put;
                break;
            case "DELETE":
                req = new HttpDelete(target);
                break;
            default:
                req = new HttpGet(target);
        }

        in.forEach((k, v) -> {
            String key = k == null ? "" : k.toLowerCase();
            if ("content-length".equals(key) || "host".equals(key)
                    || "connection".equals(key) || "transfer-encoding".equals(key)) {
                return;
            }
            req.setHeader(k, String.join(",", v));
        });

        HttpResponse resp = client.execute(req);
        int code = resp.getStatusLine().getStatusCode();
        byte[] body = resp.getEntity() != null ? EntityUtils.toByteArray(resp.getEntity()) : new byte[0];
        return new ForwardResult(code, body);
    }

    private byte[] readBodyIfNeeded(HttpExchange he, String method) throws IOException {
        if (!"POST".equals(method) && !"PUT".equals(method)) {
            return new byte[0];
        }
        try (InputStream is = he.getRequestBody();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private String extractUsername(byte[] requestBody) {
        if (requestBody == null || requestBody.length == 0) {
            return null;
        }
        try {
            JsonObject body = JsonParser.parseString(new String(requestBody, StandardCharsets.UTF_8)).getAsJsonObject();
            if (body.has("username") && !body.get("username").isJsonNull()) {
                String username = body.get("username").getAsString().trim();
                return username.isEmpty() ? null : username;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean wasSavedAfterTimeout(CloseableHttpClient client, String backend, String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            HttpGet checkReq = new HttpGet(backend + "/usuarios?username=" + encodedUsername);
            HttpResponse checkResp = client.execute(checkReq);
            int code = checkResp.getStatusLine().getStatusCode();
            if (checkResp.getEntity() != null) {
                EntityUtils.consume(checkResp.getEntity());
            }
            return code == 200;
        } catch (Exception e) {
            log.warn("Could not verify saved user by username={}", username, e);
            return false;
        }
    }

    private void registryRuntimeFailure(String backend, String errorType) {
        if (backend == null || backend.trim().isEmpty()) {
            return;
        }
        BackendRegistry.getInstance().markRuntimeFailure(backend, "request_" + errorType, WORKER_RETRY_INTERVAL_MS);
    }

    private void sendProxyError(HttpExchange he, String errorType, Exception e, String requestPath,
            String backend, long start) throws IOException {
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        int statusCode = "CONNECT".equals(errorType) ? 502 : ("READ".equals(errorType) ? 504 : 500);
        log.error("Balancer errorType={} elapsedMs={} path={} backend={}",
                errorType, elapsedMs, requestPath, backend, e);

        String msg;
        if (statusCode == 502) {
            msg = "{\"status\":\"NOK\",\"message\":\"502 - bad gateway\"}";
        } else if (statusCode == 504) {
            msg = "{\"status\":\"NOK\",\"message\":\"504 - gateway timeout\"}";
        } else {
            msg = "{\"status\":\"NOK\",\"message\":\"Balancer error\"}";
        }
        sendResponse(he, statusCode, msg.getBytes(StandardCharsets.UTF_8));
    }

    private String classifyErrorType(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return "READ";
            }
            if (cause instanceof ConnectTimeoutException || cause instanceof ConnectException
                    || cause instanceof HttpHostConnectException) {
                return "CONNECT";
            }
            cause = cause.getCause();
        }
        return "OTHER";
    }

    private void addBaseHeaders(Headers headers) {
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Content-Type", "application/json");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange he, int statusCode, byte[] body) throws IOException {
        he.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(body);
        }
    }

    private JsonArray toJsonArray(List<String> items) {
        JsonArray arr = new JsonArray();
        for (String item : items) {
            arr.add(item);
        }
        return arr;
    }

    private static class ForwardResult {
        private final int statusCode;
        private final byte[] body;

        private ForwardResult(int statusCode, byte[] body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
