package com.vtnet.netat.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public final class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    public static final String DEFAULT_SESSION = "default";
    private static final int QUIT_TIMEOUT_SECONDS = 10;

    public static final String MODE_THREAD_LOCAL = "THREAD_LOCAL";
    public static final String MODE_CLASS_BASED = "CLASS_BASED";
    public static final String MODE_SHARED = "SHARED";

    private static final SessionManager INSTANCE = new SessionManager();

    // ThreadLocal Storage
    private final ThreadLocal<Map<String, WebDriver>> threadLocalSessions =
            ThreadLocal.withInitial(ConcurrentHashMap::new);
    private final ThreadLocal<String> threadLocalCurrentSession = new ThreadLocal<>();

    // Class-Based Storage
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> classBasedSessions =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> classCurrentSession =
            new ConcurrentHashMap<>();

    // Shared Storage (Suite/Test level)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> sharedSessions =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sharedCurrentSession =
            new ConcurrentHashMap<>();

    // Mode Control
    private final ThreadLocal<String> executionMode =
            ThreadLocal.withInitial(() -> MODE_THREAD_LOCAL);
    private final ThreadLocal<String> currentContextKey = new ThreadLocal<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    // ==================== MODE CONTROL ====================

    public void setThreadLocalMode() {
        executionMode.set(MODE_THREAD_LOCAL);
        currentContextKey.remove();
        log.debug("[Thread-{}] Mode: THREAD_LOCAL", Thread.currentThread().getId());
    }

    public void setClassBasedMode(String className) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }

        executionMode.set(MODE_CLASS_BASED);
        currentContextKey.set(className);
        classBasedSessions.computeIfAbsent(className, k -> new ConcurrentHashMap<>());

        migrateThreadLocalToStorage(className, classBasedSessions, classCurrentSession);
        log.debug("[Thread-{}] Mode: CLASS_BASED, Class: {}", Thread.currentThread().getId(), className);
    }

    public void setSharedMode(String sharedKey) {
        if (sharedKey == null || sharedKey.isBlank()) {
            throw new IllegalArgumentException("Shared key cannot be null or empty");
        }

        executionMode.set(MODE_SHARED);
        currentContextKey.set(sharedKey);
        sharedSessions.computeIfAbsent(sharedKey, k -> new ConcurrentHashMap<>());
        log.debug("[Thread-{}] Mode: SHARED, Key: {}", Thread.currentThread().getId(), sharedKey);
    }

    public void setSharedMode(String sharedKey, WebDriver driver) {
        setSharedMode(sharedKey);

        if (driver != null) {
            ConcurrentHashMap<String, WebDriver> sessions = sharedSessions.get(sharedKey);
            if (!sessions.containsKey(DEFAULT_SESSION)) {
                sessions.put(DEFAULT_SESSION, driver);
                sharedCurrentSession.putIfAbsent(sharedKey, DEFAULT_SESSION);
                log.debug("[Thread-{}] Registered shared driver for key: {}",
                        Thread.currentThread().getId(), sharedKey);
            }
        }
    }

    public String getExecutionMode() {
        return executionMode.get();
    }

    public String getCurrentContextKey() {
        return currentContextKey.get();
    }

    public boolean isThreadLocalMode() {
        return MODE_THREAD_LOCAL.equals(executionMode.get());
    }

    public boolean isClassBasedMode() {
        return MODE_CLASS_BASED.equals(executionMode.get());
    }

    public boolean isSharedMode() {
        return MODE_SHARED.equals(executionMode.get());
    }

    private void migrateThreadLocalToStorage(String key,
                                             ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                             ConcurrentHashMap<String, String> currentSessionStorage) {
        Map<String, WebDriver> threadLocalMap = threadLocalSessions.get();
        if (threadLocalMap.isEmpty()) return;

        ConcurrentHashMap<String, WebDriver> targetMap = storage.get(key);
        for (Map.Entry<String, WebDriver> entry : threadLocalMap.entrySet()) {
            if (!targetMap.containsKey(entry.getKey())) {
                targetMap.put(entry.getKey(), entry.getValue());
            }
        }

        String currentName = threadLocalCurrentSession.get();
        if (currentName != null) {
            currentSessionStorage.putIfAbsent(key, currentName);
        }

        threadLocalMap.clear();
        threadLocalCurrentSession.remove();
    }

    // ==================== UNIFIED API ====================

    public void addSession(String sessionName, WebDriver driver) {
        validateSessionName(sessionName);
        validateDriver(driver);

        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                addToStorage(sharedSessions, sharedCurrentSession, key, sessionName, driver);
                break;
            case MODE_CLASS_BASED:
                addToStorage(classBasedSessions, classCurrentSession, key, sessionName, driver);
                break;
            default:
                addSessionThreadLocal(sessionName, driver);
        }
    }

    private void addToStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                              ConcurrentHashMap<String, String> currentSessionStorage,
                              String key, String sessionName, WebDriver driver) {
        if (key == null) {
            throw new IllegalStateException("Context key not set. Call setClassBasedMode() or setSharedMode() first.");
        }

        ConcurrentHashMap<String, WebDriver> sessions = storage.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        sessions.put(sessionName, driver);
        currentSessionStorage.putIfAbsent(key, sessionName);

        log.debug("[Thread-{}] Added session '{}' to key '{}'",
                Thread.currentThread().getId(), sessionName, key);
    }

    private void addSessionThreadLocal(String sessionName, WebDriver driver) {
        Map<String, WebDriver> map = threadLocalSessions.get();
        map.put(sessionName, driver);

        if (threadLocalCurrentSession.get() == null) {
            threadLocalCurrentSession.set(sessionName);
        }

        log.debug("[Thread-{}] Added session '{}' (ThreadLocal)",
                Thread.currentThread().getId(), sessionName);
    }

    public WebDriver getCurrentDriver() {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                return getDriverFromStorage(sharedSessions, sharedCurrentSession, key);
            case MODE_CLASS_BASED:
                return getDriverFromStorage(classBasedSessions, classCurrentSession, key);
            default:
                return getCurrentDriverThreadLocal();
        }
    }

    private WebDriver getDriverFromStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                           ConcurrentHashMap<String, String> currentSessionStorage,
                                           String key) {
        if (key == null) {
            log.warn("[Thread-{}] Context key not set, falling back to ThreadLocal",
                    Thread.currentThread().getId());
            return getCurrentDriverThreadLocal();
        }

        String currentName = currentSessionStorage.getOrDefault(key, DEFAULT_SESSION);
        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);

        if (sessions == null || sessions.isEmpty()) {
            log.warn("[Thread-{}] No sessions found for key '{}'", Thread.currentThread().getId(), key);
            return null;
        }

        return sessions.get(currentName);
    }

    private WebDriver getCurrentDriverThreadLocal() {
        String name = threadLocalCurrentSession.get();
        Map<String, WebDriver> map = threadLocalSessions.get();

        if (name != null) {
            WebDriver driver = map.get(name);
            if (driver != null) return driver;
        }

        WebDriver defaultDriver = map.get(DEFAULT_SESSION);
        if (defaultDriver != null) {
            threadLocalCurrentSession.set(DEFAULT_SESSION);
            return defaultDriver;
        }

        return null;
    }

    public WebDriver getSession(String sessionName) {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                return getSessionFromStorage(sharedSessions, key, sessionName);
            case MODE_CLASS_BASED:
                return getSessionFromStorage(classBasedSessions, key, sessionName);
            default:
                return threadLocalSessions.get().get(sessionName);
        }
    }

    private WebDriver getSessionFromStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                            String key, String sessionName) {
        if (key == null) return null;
        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);
        return sessions != null ? sessions.get(sessionName) : null;
    }

    public void switchSession(String sessionName) {
        validateSessionName(sessionName);

        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                switchSessionInStorage(sharedSessions, sharedCurrentSession, key, sessionName);
                break;
            case MODE_CLASS_BASED:
                switchSessionInStorage(classBasedSessions, classCurrentSession, key, sessionName);
                break;
            default:
                switchSessionThreadLocal(sessionName);
        }
    }

    private void switchSessionInStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                        ConcurrentHashMap<String, String> currentSessionStorage,
                                        String key, String sessionName) {
        if (key == null) {
            throw new IllegalStateException("Context key not set");
        }

        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);
        if (sessions == null || !sessions.containsKey(sessionName)) {
            Set<String> available = sessions != null ? sessions.keySet() : Set.of();
            throw new IllegalArgumentException(
                    "Session '" + sessionName + "' not found. Available: " + available);
        }

        currentSessionStorage.put(key, sessionName);
        log.debug("[Thread-{}] Switched to session '{}' for key '{}'",
                Thread.currentThread().getId(), sessionName, key);
    }

    private void switchSessionThreadLocal(String sessionName) {
        Map<String, WebDriver> map = threadLocalSessions.get();
        if (!map.containsKey(sessionName)) {
            throw new IllegalArgumentException(
                    "Session '" + sessionName + "' not found. Available: " + map.keySet());
        }
        threadLocalCurrentSession.set(sessionName);
    }

    public List<String> getSessionNames() {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                return getSessionNamesFromStorage(sharedSessions, key);
            case MODE_CLASS_BASED:
                return getSessionNamesFromStorage(classBasedSessions, key);
            default:
                return new ArrayList<>(threadLocalSessions.get().keySet());
        }
    }

    private List<String> getSessionNamesFromStorage(
            ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage, String key) {
        if (key == null) return new ArrayList<>();
        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);
        return sessions != null ? new ArrayList<>(sessions.keySet()) : new ArrayList<>();
    }

    public Map<String, WebDriver> getAllSessions() {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                return getAllSessionsFromStorage(sharedSessions, key);
            case MODE_CLASS_BASED:
                return getAllSessionsFromStorage(classBasedSessions, key);
            default:
                return new ConcurrentHashMap<>(threadLocalSessions.get());
        }
    }

    private Map<String, WebDriver> getAllSessionsFromStorage(
            ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage, String key) {
        if (key == null) return new ConcurrentHashMap<>();
        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);
        return sessions != null ? new ConcurrentHashMap<>(sessions) : new ConcurrentHashMap<>();
    }

    public String getCurrentSessionName() {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                return key != null ? sharedCurrentSession.get(key) : null;
            case MODE_CLASS_BASED:
                return key != null ? classCurrentSession.get(key) : null;
            default:
                return threadLocalCurrentSession.get();
        }
    }

    // ==================== SESSION HEALTH CHECK ====================

    public boolean isSessionAlive(String sessionName) {
        WebDriver driver = getSession(sessionName);
        if (driver == null) return false;

        if (driver instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) driver).getSessionId() != null;
        }
        return true;
    }

    public void removeDeadSession(String sessionName) {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                removeFromStorage(sharedSessions, sharedCurrentSession, key, sessionName);
                break;
            case MODE_CLASS_BASED:
                removeFromStorage(classBasedSessions, classCurrentSession, key, sessionName);
                break;
            default:
                threadLocalSessions.get().remove(sessionName);
                if (sessionName.equals(threadLocalCurrentSession.get())) {
                    threadLocalCurrentSession.remove();
                }
        }
    }

    private void removeFromStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                   ConcurrentHashMap<String, String> currentSessionStorage,
                                   String key, String sessionName) {
        if (key == null) return;
        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);
        if (sessions != null) {
            sessions.remove(sessionName);
            if (sessionName.equals(currentSessionStorage.get(key))) {
                currentSessionStorage.remove(key);
            }
        }
    }

    // ==================== CLEANUP ====================

    public void stopSession(String sessionName) {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                stopSessionFromStorage(sharedSessions, sharedCurrentSession, key, sessionName);
                break;
            case MODE_CLASS_BASED:
                stopSessionFromStorage(classBasedSessions, classCurrentSession, key, sessionName);
                break;
            default:
                stopSessionThreadLocal(sessionName);
        }
    }

    private void stopSessionFromStorage(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                        ConcurrentHashMap<String, String> currentSessionStorage,
                                        String key, String sessionName) {
        if (key == null) return;
        ConcurrentHashMap<String, WebDriver> sessions = storage.get(key);
        if (sessions == null) return;

        WebDriver driver = sessions.remove(sessionName);
        if (driver != null) {
            quitDriver(driver, sessionName);
        }

        if (sessionName.equals(currentSessionStorage.get(key))) {
            currentSessionStorage.remove(key);
        }
    }

    private void stopSessionThreadLocal(String sessionName) {
        Map<String, WebDriver> map = threadLocalSessions.get();
        WebDriver driver = map.remove(sessionName);
        if (driver != null) {
            quitDriver(driver, sessionName);
        }

        if (sessionName.equals(threadLocalCurrentSession.get())) {
            threadLocalCurrentSession.remove();
        }
    }

    public void stopAllSessions() {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        switch (mode) {
            case MODE_SHARED:
                if (key != null) stopAllSessionsForKey(sharedSessions, sharedCurrentSession, key);
                break;
            case MODE_CLASS_BASED:
                if (key != null) stopAllSessionsForKey(classBasedSessions, classCurrentSession, key);
                break;
            default:
                stopAllSessionsThreadLocal();
        }
    }

    public void stopAllSessionsForClass(String className) {
        stopAllSessionsForKey(classBasedSessions, classCurrentSession, className);
    }

    public void stopAllSessionsForSharedKey(String sharedKey) {
        stopAllSessionsForKey(sharedSessions, sharedCurrentSession, sharedKey);
    }

    private void stopAllSessionsForKey(ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> storage,
                                       ConcurrentHashMap<String, String> currentSessionStorage,
                                       String key) {
        if (key == null) return;

        ConcurrentHashMap<String, WebDriver> sessions = storage.remove(key);
        currentSessionStorage.remove(key);

        if (sessions == null || sessions.isEmpty()) {
            log.debug("No sessions to stop for key '{}'", key);
            return;
        }

        log.info("Stopping {} session(s) for key '{}': {}", sessions.size(), key, sessions.keySet());

        for (Map.Entry<String, WebDriver> entry : sessions.entrySet()) {
            quitDriver(entry.getValue(), entry.getKey());
        }
    }

    private void stopAllSessionsThreadLocal() {
        Map<String, WebDriver> map = threadLocalSessions.get();
        if (map.isEmpty()) return;

        log.info("[Thread-{}] Stopping {} session(s): {}",
                Thread.currentThread().getId(), map.size(), map.keySet());

        for (Map.Entry<String, WebDriver> entry : map.entrySet()) {
            quitDriver(entry.getValue(), entry.getKey());
        }

        map.clear();
        threadLocalCurrentSession.remove();
    }

    public void cleanupThread() {
        stopAllSessionsThreadLocal();
        threadLocalSessions.remove();
        threadLocalCurrentSession.remove();
        executionMode.remove();
        currentContextKey.remove();
    }

    public void cleanupAll() {
        cleanupThread();

        for (String key : new ArrayList<>(classBasedSessions.keySet())) {
            stopAllSessionsForKey(classBasedSessions, classCurrentSession, key);
        }

        for (String key : new ArrayList<>(sharedSessions.keySet())) {
            stopAllSessionsForKey(sharedSessions, sharedCurrentSession, key);
        }

        classBasedSessions.clear();
        classCurrentSession.clear();
        sharedSessions.clear();
        sharedCurrentSession.clear();

        log.info("All SessionManager resources cleaned up");
    }

    // ==================== UTILITY ====================

    private void validateSessionName(String sessionName) {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
    }

    private void validateDriver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver cannot be null");
        }
    }

    private void quitDriver(WebDriver driver, String sessionName) {
        if (driver == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Exception during quit for session '{}': {}", sessionName, e.getMessage());
            }
        });

        try {
            future.get(QUIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Session '{}' closed successfully", sessionName);
        } catch (TimeoutException e) {
            log.error("Timeout closing session '{}'", sessionName);
            future.cancel(true);
            forceCloseRemoteSession(driver, sessionName);
        } catch (Exception e) {
            log.error("Error closing session '{}': {}", sessionName, e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private void forceCloseRemoteSession(WebDriver driver, String sessionName) {
        if (!(driver instanceof RemoteWebDriver)) return;

        try {
            RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
            String sessionId = remoteDriver.getSessionId() != null
                    ? remoteDriver.getSessionId().toString()
                    : null;
            if (sessionId == null) return;

            String gridUrl = getGridUrl();
            if (gridUrl == null || gridUrl.isEmpty()) return;

            String deleteUrl = gridUrl + "/session/" + sessionId;
            HttpURLConnection conn = (HttpURLConnection) new URL(deleteUrl).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();

            log.info("Force closed session '{}' via Grid API", sessionName);
        } catch (IOException e) {
            log.error("Failed to force close session '{}': {}", sessionName, e.getMessage());
        }
    }

    private String getGridUrl() {
        String url = ConfigReader.getProperty("grid.url");
        if (url == null || url.isEmpty()) url = ConfigReader.getProperty("appium.server.url");
        return url;
    }

    // ==================== DEBUG ====================

    public void printCurrentState() {
        String mode = executionMode.get();
        String key = currentContextKey.get();

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║ SessionManager State                                        ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║ Thread: {} (ID: {})", Thread.currentThread().getName(), Thread.currentThread().getId());
        log.info("║ Mode: {}", mode);
        log.info("║ Context Key: {}", key);
        log.info("║ Current Session: {}", getCurrentSessionName());
        log.info("║ Sessions: {}", getSessionNames());
        log.info("║ Class-Based Keys: {}", classBasedSessions.keySet());
        log.info("║ Shared Keys: {}", sharedSessions.keySet());
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    public String getDebugInfo() {
        return String.format("SessionManager[mode=%s, key=%s, thread=%d, sessions=%s]",
                executionMode.get(),
                currentContextKey.get(),
                Thread.currentThread().getId(),
                getSessionNames());
    }
}