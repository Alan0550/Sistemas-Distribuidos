package edu.upb.tmservice;

import com.google.gson.JsonObject;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    private MonitorStore() {
    }

    public static MonitorStore getInstance() {
        return INSTANCE;
    }

    public void record(String route, long startNano, boolean error) {
        totalRequests.incrementAndGet();
        if (error) {
            totalErrors.incrementAndGet();
        }
        totalLatencyNanos.addAndGet(System.nanoTime() - startNano);
        requestsByRoute.computeIfAbsent(route, r -> new AtomicLong(0)).incrementAndGet();
    }

    public JsonObject health(String serviceName, int port) {
        HealthSnapshot snapshot = evaluateHealth();
        return buildHealthJson(serviceName, port, snapshot);
    }

    public JsonObject metrics(String serviceName, int port) {
        HealthSnapshot snapshot = evaluateHealth();
        JsonObject out = buildHealthJson(serviceName, port, snapshot);
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

        // JVM memory metrics
        Runtime rt = Runtime.getRuntime();
        long heapUsed = rt.totalMemory() - rt.freeMemory();
        out.addProperty("jvm_heap_used_bytes", heapUsed);
        out.addProperty("jvm_heap_free_bytes", rt.freeMemory());
        out.addProperty("jvm_heap_total_bytes", rt.totalMemory());
        out.addProperty("jvm_heap_max_bytes", rt.maxMemory());
        out.addProperty("jvm_thread_count", Thread.activeCount());

        // System metrics
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        out.addProperty("system_load_average", osBean.getSystemLoadAverage());
        out.addProperty("available_processors", osBean.getAvailableProcessors());

        // Disk metrics
        out.addProperty("disk_total_bytes", snapshot.diskTotalBytes);
        out.addProperty("disk_free_bytes", snapshot.diskFreeBytes);
        out.addProperty("disk_usable_bytes", snapshot.diskUsableBytes);

        return out;
    }

    private JsonObject buildHealthJson(String serviceName, int port, HealthSnapshot snapshot) {
        JsonObject out = new JsonObject();
        out.addProperty("service", serviceName);
        out.addProperty("status", snapshot.status);
        out.addProperty("port", port);
        out.addProperty("uptime_ms", System.currentTimeMillis() - startTimeMillis);

        JsonObject db = new JsonObject();
        db.addProperty("up", snapshot.dbUp);
        db.addProperty("latency_ms", snapshot.dbLatencyMs);
        db.addProperty("error", snapshot.dbError);
        out.add("database", db);

        JsonObject disk = new JsonObject();
        disk.addProperty("total_bytes", snapshot.diskTotalBytes);
        disk.addProperty("free_bytes", snapshot.diskFreeBytes);
        disk.addProperty("usable_bytes", snapshot.diskUsableBytes);
        disk.addProperty("full", snapshot.diskFull);
        out.add("disk", disk);

        return out;
    }

    private HealthSnapshot evaluateHealth() {
        File disk = new File(".");
        long diskTotal = disk.getTotalSpace();
        long diskFree = disk.getFreeSpace();
        long diskUsable = disk.getUsableSpace();
        boolean diskFull = diskUsable <= 0L;

        long dbStart = System.nanoTime();
        boolean dbUp = false;
        String dbError = "";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT 1");
                ResultSet rs = ps.executeQuery()) {
            dbUp = rs.next();
        } catch (Exception e) {
            dbError = e.getClass().getSimpleName();
        }
        long dbLatencyMs = (System.nanoTime() - dbStart) / 1_000_000;
        String status = (dbUp && !diskFull) ? "UP" : "DOWN";
        return new HealthSnapshot(status, dbUp, dbLatencyMs, dbError, diskTotal, diskFree, diskUsable, diskFull);
    }

    private static class HealthSnapshot {
        private final String status;
        private final boolean dbUp;
        private final long dbLatencyMs;
        private final String dbError;
        private final long diskTotalBytes;
        private final long diskFreeBytes;
        private final long diskUsableBytes;
        private final boolean diskFull;

        private HealthSnapshot(String status, boolean dbUp, long dbLatencyMs, String dbError,
                long diskTotalBytes, long diskFreeBytes, long diskUsableBytes, boolean diskFull) {
            this.status = status;
            this.dbUp = dbUp;
            this.dbLatencyMs = dbLatencyMs;
            this.dbError = dbError;
            this.diskTotalBytes = diskTotalBytes;
            this.diskFreeBytes = diskFreeBytes;
            this.diskUsableBytes = diskUsableBytes;
            this.diskFull = diskFull;
        }
    }
}
