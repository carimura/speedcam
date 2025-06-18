package com.pinealpha.util;

import java.util.HashMap;
import java.util.Map;

public class Timer {
    private final Map<String, Long> timings = new HashMap<>();
    private final Map<String, Long> callCounts = new HashMap<>();

    public void start(String name) {
        timings.put(name + "_start", System.nanoTime());
    }

    public void stop(String name) {
        long endTime = System.nanoTime();
        long startTime = timings.getOrDefault(name + "_start", endTime);
        long duration = endTime - startTime;

        timings.put(name, timings.getOrDefault(name, 0L) + duration);
        callCounts.put(name, callCounts.getOrDefault(name, 0L) + 1);
    }

    public void print() {
        System.out.println("\n--- Performance Timers (ms) ---");
        timings.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String name = entry.getKey();
                    if (name.endsWith("_start")) return;
                    long totalNanos = entry.getValue();
                    long count = callCounts.getOrDefault(name, 1L);
                    double totalMs = (double) totalNanos / 1_000_000.0;
                    double avgMs = totalMs / count;
                    System.out.printf("%-30s | Total: %,12.2f ms | Count: %,6d | Avg: %,12.4f ms%n",
                            name, totalMs, count, avgMs);
                });
        System.out.println("--------------------------------------\n");
    }

    public void reset() {
        timings.clear();
        callCounts.clear();
    }
} 