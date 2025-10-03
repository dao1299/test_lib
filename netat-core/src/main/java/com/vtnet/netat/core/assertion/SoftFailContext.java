// com/vtnet/netat/core/assertion/SoftFailContext.java
package com.vtnet.netat.core.assertion;

public final class SoftFailContext {
    private static final ThreadLocal<Boolean> HAS_FAIL = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<StringBuilder> MSG = ThreadLocal.withInitial(StringBuilder::new);

    private SoftFailContext() {}

    public static void markFailed(String message) {
        HAS_FAIL.set(true);
        if (message != null && !message.isBlank()) {
            if (MSG.get().length() > 0) MSG.get().append('\n');
            MSG.get().append(message);
        }
    }

    /** Trả về và reset cờ fail */
    public static boolean consumeHasFail() {
        boolean v = HAS_FAIL.get();
        HAS_FAIL.set(false);
        return v;
    }

    /** Trả về và reset buffer message */
    public static String consumeMessages() {
        String s = MSG.get().toString();
        MSG.set(new StringBuilder());
        return s;
    }

    /** Reset trước mỗi keyword */
    public static void reset() {
        HAS_FAIL.set(false);
        MSG.set(new StringBuilder());
    }
}
