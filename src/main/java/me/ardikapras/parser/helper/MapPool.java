package me.ardikapras.parser.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MapPool {
    private final Queue<Map<String, String>> pool;
    private final int maxSize;
    private final AtomicInteger currentSize;
    private static final int DEFAULT_MAP_CAPACITY = 32;

    public MapPool(int maxSize) {
        this.maxSize = maxSize;
        this.pool = new ConcurrentLinkedQueue<>();
        this.currentSize = new AtomicInteger(0);
        preallocate(Math.min(1000, maxSize / 2));
    }

    private void preallocate(int count) {
        for (int i = 0; i < count; i++) {
            pool.offer(new HashMap<>(DEFAULT_MAP_CAPACITY));
            currentSize.incrementAndGet();
        }
    }

    public Map<String, String> borrowMap() {
        Map<String, String> map = pool.poll();
        if (map != null) {
            currentSize.decrementAndGet();
            return map;
        }
        return new HashMap<>(DEFAULT_MAP_CAPACITY);
    }

    public void returnMap(Map<String, String> map) {
        if (map != null && currentSize.get() < maxSize) {
            map.clear();
            pool.offer(map);
            currentSize.incrementAndGet();
        }
    }

    public void clear() {
        pool.clear();
        currentSize.set(0);
    }
}
