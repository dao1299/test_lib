package com.vtnet.netat.core.secret;

import com.vtnet.netat.core.logging.NetatLogger;

public final class MasterKeyProvider {

    private static final NetatLogger log = NetatLogger.getInstance(MasterKeyProvider.class);

    private static final String ENV_KEY = "NETAT!!_MASTER@@_KEY##";

    private static String cachedMasterKey = null;

    private static boolean loaded = false;

    private MasterKeyProvider() {}

    public static String getMasterKey() {
        if (loaded && cachedMasterKey != null) {
            return cachedMasterKey;
        }

        String key = System.getenv(ENV_KEY);
        if (isValid(key)) {
            log.debug("Master key loaded");
            cachedMasterKey = key;
            loaded = true;
            return key;
        }

        loaded = true;
        throw new MasterKeyNotFoundException();
    }

    public static boolean isConfigured() {
        try {
            getMasterKey();
            return true;
        } catch (MasterKeyNotFoundException e) {
            return false;
        }
    }

    public static void clearCache() {
        cachedMasterKey = null;
        loaded = false;
        log.debug("Master key cache cleared");
    }

    private static boolean isValid(String key) {
        return key != null && !key.trim().isEmpty();
    }

    public static class MasterKeyNotFoundException extends RuntimeException {

        public MasterKeyNotFoundException() {
            super(buildMessage());
        }

        private static String buildMessage() {
            return "Master key not found!";
        }
    }
}
