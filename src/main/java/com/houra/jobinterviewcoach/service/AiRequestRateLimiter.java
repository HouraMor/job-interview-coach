package com.houra.jobinterviewcoach.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AiRequestRateLimiter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int requestLimit;
    private final long windowMillis;
    private final Map<String, RequestWindow> requestWindows = new HashMap<>();
    private long nextCleanupMillis;

    public AiRequestRateLimiter(
            @Value("${app.rate-limit.request-limit:10}") int requestLimit,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        if (requestLimit <= 0 || windowSeconds <= 0) {
            throw new IllegalStateException("Rate-limit values must be greater than zero.");
        }

        this.requestLimit = requestLimit;
        this.windowMillis = Math.multiplyExact(windowSeconds, 1000L);
    }

    public synchronized boolean tryAcquire(String clientAddress) {
        long now = System.currentTimeMillis();
        removeExpiredWindows(now);

        String clientKey = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        RequestWindow currentWindow = requestWindows.get(clientKey);

        if (currentWindow == null || isExpired(currentWindow, now)) {
            if (currentWindow == null && requestWindows.size() >= MAX_TRACKED_CLIENTS) {
                return false;
            }
            requestWindows.put(clientKey, new RequestWindow(now, 1));
            return true;
        }

        if (currentWindow.requestCount() >= requestLimit) {
            return false;
        }

        requestWindows.put(
                clientKey,
                new RequestWindow(currentWindow.startedAtMillis(), currentWindow.requestCount() + 1)
        );
        return true;
    }

    public long retryAfterSeconds() {
        return Math.max(1, windowMillis / 1000L);
    }

    private void removeExpiredWindows(long now) {
        if (now < nextCleanupMillis) {
            return;
        }

        requestWindows.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        nextCleanupMillis = now + windowMillis;
    }

    private boolean isExpired(RequestWindow window, long now) {
        return now - window.startedAtMillis() >= windowMillis;
    }

    private record RequestWindow(long startedAtMillis, int requestCount) {
    }
}
