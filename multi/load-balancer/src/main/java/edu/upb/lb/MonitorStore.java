package edu.upb.lb;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MonitorStore {

    private static final MonitorStore INSTANCE = new MonitorStore();

    private final long startTimeMillis = System.currentTimeMillis();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private final Map<String, AtomicLong> requestsByRoute = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestsByBackend = new ConcurrentHashMap<>();

    private MonitorStore() {
    }

    public static MonitorStore getInstance() {
        return INSTANCE;
    }

    public void record(String route, String backend, long startNano, boolean error) {
        totalRequests.incrementAndGet();
        if (error) {
            totalErrors.incrementAndGet();
        }
        totalLatencyNanos.addAndGet(System.nanoTime() - startNano);
        requestsByRoute.computeIfAbsent(route, r -> new AtomicLong(0)).incrementAndGet();
        requestsByBackend.computeIfAbsent(backend, r -> new AtomicLong(0)).incrementAndGet();
    }

    public JsonObject health(String serviceName, int port) {
        JsonObject out = new JsonObject();
        out.addProperty("service", serviceName);
        out.addProperty("status", "UP");
        out.addProperty("port", port);
        out.addProperty("uptime_ms", System.currentTimeMillis() - startTimeMillis);
        return out;
    }

    public JsonObject metrics(String serviceName, int port) {
        JsonObject out = health(serviceName, port);
        long requests = totalRequests.get();
        long errors = totalErrors.get();
        long avgLatencyMs = requests == 0 ? 0 : (totalLatencyNanos.get() / requests) / 1_000_000;

        out.addProperty("requests_total", requests);
        out.addProperty("errors_total", errors);
        out.addProperty("avg_latency_ms", avgLatencyMs);

        JsonObject routes = new JsonObject();
        for (Map.Entry<String, AtomicLong> entry : requestsByRoute.entrySet()) {
            routes.addProperty(entry.getKey(), entry.getValue().get());
        }
        out.add("requests_by_route", routes);

        JsonObject backends = new JsonObject();
        for (Map.Entry<String, AtomicLong> entry : requestsByBackend.entrySet()) {
            backends.addProperty(entry.getKey(), entry.getValue().get());
        }
        out.add("requests_by_backend", backends);
        return out;
    }
}
