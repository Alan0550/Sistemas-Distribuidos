package edu.upb.lb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BackendRegistry {
    private static final BackendRegistry INSTANCE = new BackendRegistry();

    private final CopyOnWriteArrayList<String> backends = new CopyOnWriteArrayList<>();
    private final AtomicInteger idx = new AtomicInteger(0);

    private BackendRegistry() {
    }

    public static BackendRegistry getInstance() {
        return INSTANCE;
    }

    public String register(String ip, int port) {
        String host = (ip == null || ip.trim().isEmpty()) ? "localhost" : ip.trim();
        String url = "http://" + host + ":" + port;
        if (!backends.contains(url)) {
            backends.add(url);
        }
        return url;
    }

    public String nextBackend() {
        if (backends.isEmpty()) {
            return null;
        }
        int i = Math.floorMod(idx.getAndIncrement(), backends.size());
        return backends.get(i);
    }

    public List<String> list() {
        return new ArrayList<>(backends);
    }
}
