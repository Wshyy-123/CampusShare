package com.campusshare.common;

import java.util.UUID;

public final class RequestIdContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestIdContext() {
    }

    public static void set(String requestId) {
        CURRENT.set(requestId);
    }

    public static String get() {
        String requestId = CURRENT.get();
        return requestId == null ? UUID.randomUUID().toString() : requestId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
