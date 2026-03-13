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

    private final CopyOnWriteArrayList<String> backends = new CopyOnWriteArrayList<>();
    private final Map<String, BackendState> states = new ConcurrentHashMap<>();
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
        if (!backends.contains(normalized)) {
            backends.add(normalized);
        }
        states.putIfAbsent(normalized, new BackendState());
        return normalized;
    }

    public String nextBackend() {
        if (backends.isEmpty()) {
            return null;
        }
        int size = backends.size();
        for (int attempt = 0; attempt < size; attempt++) {
            int i = Math.floorMod(idx.getAndIncrement(), size);
            String candidate = backends.get(i);
            BackendState state = states.get(candidate);
            if (state == null || state.inService) {
                return candidate;
            }
        }
        return null;
    }

    public List<String> list() {
        return new ArrayList<>(backends);
    }

    public int countInService() {
        int count = 0;
        for (String backend : backends) {
            BackendState state = states.get(backend);
            if (state == null || state.inService) {
                count++;
            }
        }
        return count;
    }

    public void markSuccess(String backend, JsonObject metrics) {
        BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
        state.inService = true;
        state.failCount = 0;
        state.lastError = "";
        state.lastCheckMs = System.currentTimeMillis();
        state.lastMetricsJson = metrics != null ? metrics.toString() : null;
    }

    public void markFailure(String backend, String reason, int failThreshold) {
        BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
        state.failCount = state.failCount + 1;
        if (state.failCount >= failThreshold) {
            state.inService = false;
        }
        state.lastError = reason == null ? "unknown_error" : reason;
        state.lastCheckMs = System.currentTimeMillis();
    }

    public JsonArray statusSnapshot() {
        JsonArray arr = new JsonArray();
        for (String backend : backends) {
            BackendState state = states.computeIfAbsent(backend, b -> new BackendState());
            JsonObject item = new JsonObject();
            item.addProperty("backend", backend);
            item.addProperty("status", state.inService ? "UP" : "DOWN");
            item.addProperty("in_service", state.inService);
            item.addProperty("fail_count", state.failCount);
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
        private volatile boolean inService = true;
        private volatile int failCount = 0;
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
