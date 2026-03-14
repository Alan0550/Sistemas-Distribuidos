package edu.upb.lb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Esclavo extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Esclavo.class);

    private final BackendRegistry registry;
    private final long okIntervalMs;
    private final long retryIntervalMs;
    private final long loopSleepMs;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int failThreshold;
    private volatile boolean running = true;

    public Esclavo(BackendRegistry registry) {
        this.registry = registry;
        this.okIntervalMs = Long.parseLong(System.getenv().getOrDefault("LB_WORKER_OK_INTERVAL_MS", "30000"));
        this.retryIntervalMs = Long.parseLong(System.getenv().getOrDefault("LB_WORKER_RETRY_INTERVAL_MS", "10000"));
        this.loopSleepMs = Long.parseLong(System.getenv().getOrDefault("LB_WORKER_LOOP_SLEEP_MS", "1000"));
        this.connectTimeoutMs = Integer.parseInt(System.getenv().getOrDefault("LB_WORKER_CONNECT_TIMEOUT_MS", "1500"));
        this.readTimeoutMs = Integer.parseInt(System.getenv().getOrDefault("LB_WORKER_READ_TIMEOUT_MS", "1500"));
        int configuredThreshold = Integer.parseInt(System.getenv().getOrDefault("LB_WORKER_FAIL_THRESHOLD", "3"));
        this.failThreshold = Math.max(1, configuredThreshold);
        setName("backend-health-worker");
        setDaemon(true);
    }

    @Override
    public void run() {
        log.info("Esclavo started (okIntervalMs={}, retryIntervalMs={}, failThreshold={})",
                okIntervalMs, retryIntervalMs, failThreshold);
        while (true) {
            if (!running) {
                break;
            }

            long nowMs = System.currentTimeMillis();
            List<String> backends = registry.list();
            for (String backend : backends) {
                if (!registry.shouldCheckNow(backend, nowMs)) {
                    continue;
                }

                JsonObject metrics = consultarSalud(backend);
                if (metrics != null) {
                    String status = metrics.has("status") ? metrics.get("status").getAsString() : "UNKNOWN";
                    if ("UP".equalsIgnoreCase(status)) {
                        registry.markHealthSuccess(backend, metrics, okIntervalMs);
                    } else {
                        registry.markHealthFailure(backend, "status_" + status, failThreshold, retryIntervalMs);
                        log.warn("Health check reported non-UP for {} (status={})", backend, status);
                    }
                }
            }

            try {
                Thread.sleep(loopSleepMs);
            } catch (InterruptedException e) {
                if (!running) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("Esclavo stopped");
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    private JsonObject consultarSalud(String backend) {
        String target = backend + "/health";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(target).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                registry.markHealthFailure(backend, "http_" + statusCode, failThreshold, retryIntervalMs);
                log.warn("Health check failed for {} (status={})", backend, statusCode);
                return null;
            }

            String body = readAll(conn.getInputStream());
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            registry.markHealthFailure(backend, e.getClass().getSimpleName(), failThreshold, retryIntervalMs);
            log.warn("Health check exception for {}: {}", backend, e.getClass().getSimpleName());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readAll(InputStream is) throws Exception {
        try (InputStream in = is;
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
