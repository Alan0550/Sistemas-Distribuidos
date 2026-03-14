package edu.upb.lb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BackendRegistry {
    private static final BackendRegistry INSTANCE = new BackendRegistry();

    private final CopyOnWriteArrayList<String> balancingBackends = new CopyOnWriteArrayList<String>();
    private final CopyOnWriteArrayList<String> verificationBackends = new CopyOnWriteArrayList<String>();
    private final Map<String, BackendState> states = new ConcurrentHashMap<String, BackendState>();
    private final AtomicInteger idx = new AtomicInteger(0);

    private BackendRegistry() {
    }

    public static BackendRegistry getInstance() {
        return INSTANCE;
    }

    public String register(String ip, int port) {
        String host = (ip == null || ip.trim().isEmpty()) ? "localhost" : ip.trim();
        return registerUrl("http://" + host + ":" + port + "/");
    }

    public String registerUrl(String rawUrl) {
        String normalized = normalizeUrl(rawUrl);
        if (!verificationBackends.contains(normalized)) {
            verificationBackends.add(normalized);
        }
        if (!balancingBackends.contains(normalized)) {
            balancingBackends.add(normalized);
        }

        BackendState state = states.computeIfAbsent(normalized, b -> new BackendState());
        state.inBalancing = true;
        state.inVerification = true;
        state.verificationFailCount = 0;
        state.lastError = "";
        state.nextHealthCheckMs = 0L;
        return normalized;
    }

    public String nextBackend() {
        if (balancingBackends.isEmpty()) {
            return null;
        }
        int size = balancingBackends.size();
        for (int attempt = 0; attempt < size; attempt++) {
            int i = Math.floorMod(idx.getAndIncrement(), size);
            String candidate = balancingBackends.get(i);
            BackendState state = states.get(candidate);
            if (state != null && state.inBalancing) {
                return candidate;
            }
        }
        return null;
    }

    public List<String> list() {
        return new ArrayList<String>(verificationBackends);
    }

    public List<String> listBalancing() {
        return new ArrayList<String>(balancingBackends);
    }

    public int countInService() {
        return balancingBackends.size();
    }

    public boolean shouldCheckNow(String backend, long nowMs) {
        BackendState state = states.get(backend);
        return state != null && state.inVerification && nowMs >= state.nextHealthCheckMs;
    }

    public void markRuntimeFailure(String backend, String reason, long retryIntervalMs) {
        BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
        balancingBackends.remove(backend);
        state.inBalancing = false;
        state.lastError = reason == null ? "runtime_failure" : reason;
        state.nextHealthCheckMs = System.currentTimeMillis() + retryIntervalMs;
    }

    public void markHealthSuccess(String backend, JsonObject metrics, long okIntervalMs) {
        BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
        if (!verificationBackends.contains(backend)) {
            verificationBackends.add(backend);
        }
        if (!balancingBackends.contains(backend)) {
            balancingBackends.add(backend);
        }

        state.inVerification = true;
        state.inBalancing = true;
        state.verificationFailCount = 0;
        state.lastError = "";
        state.lastCheckMs = System.currentTimeMillis();
        state.nextHealthCheckMs = state.lastCheckMs + okIntervalMs;
        state.lastMetricsJson = metrics != null ? metrics.toString() : null;
    }

    public void markHealthFailure(String backend, String reason, int failThreshold, long retryIntervalMs) {
        BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
        balancingBackends.remove(backend);
        state.inBalancing = false;
        state.inVerification = true;
        state.verificationFailCount = state.verificationFailCount + 1;
        state.lastError = reason == null ? "health_failure" : reason;
        state.lastCheckMs = System.currentTimeMillis();
        state.nextHealthCheckMs = state.lastCheckMs + retryIntervalMs;

        if (state.verificationFailCount >= failThreshold) {
            verificationBackends.remove(backend);
            state.inVerification = false;
        }
    }

    public JsonArray statusSnapshot() {
        JsonArray arr = new JsonArray();
        for (String backend : verificationBackends) {
            BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
            JsonObject item = new JsonObject();
            item.addProperty("backend", backend);
            item.addProperty("status", state.inBalancing ? "UP" : "DOWN");
            item.addProperty("in_service", state.inBalancing);
            item.addProperty("in_balancing", state.inBalancing);
            item.addProperty("in_verification", state.inVerification);
            item.addProperty("fail_count", state.verificationFailCount);
            item.addProperty("next_check_ms", state.nextHealthCheckMs);
            item.addProperty("last_check_ms", state.lastCheckMs);
            item.addProperty("last_error", state.lastError);
            if (state.lastMetricsJson != null && !state.lastMetricsJson.isEmpty()) {
                try {
                    item.add("last_metrics", JsonParser.parseString(state.lastMetricsJson));
                } catch (Exception ignored) {
                }
            }
            arr.add(item);
        }
        return arr;
    }

    private static class BackendState {
        private volatile boolean inBalancing = true;
        private volatile boolean inVerification = true;
        private volatile int verificationFailCount = 0;
        private volatile long nextHealthCheckMs = 0L;
        private volatile long lastCheckMs = 0L;
        private volatile String lastError = "";
        private volatile String lastMetricsJson = null;
    }

    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL vacia");
        }
        URI uri = URI.create(rawUrl.trim());
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if (scheme == null || host == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("URL invalida");
        }
        return scheme.toLowerCase() + "://" + host + ":" + port;
    }
}
