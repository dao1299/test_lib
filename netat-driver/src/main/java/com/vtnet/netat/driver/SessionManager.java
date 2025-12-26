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

    public static final String DEFAULT_SESSION = "default";
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final int QUIT_TIMEOUT_SECONDS = 10;

    private static final String MODE_THREAD_LOCAL = "THREAD_LOCAL";
    private static final String MODE_CLASS_BASED = "CLASS_BASED";

    private static final SessionManager INSTANCE = new SessionManager();
    private final ThreadLocal<Map<String, WebDriver>> threadLocalSessions =
            ThreadLocal.withInitial(ConcurrentHashMap::new);
    private final ThreadLocal<String> threadLocalCurrentSession = new ThreadLocal<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, WebDriver>> classBasedSessions =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> classCurrentSession =
            new ConcurrentHashMap<>();
    private final ThreadLocal<String> executionMode =
            ThreadLocal.withInitial(() -> MODE_THREAD_LOCAL);
    private final ThreadLocal<String> currentTestClass = new ThreadLocal<>();


    private SessionManager() {
        log.debug("SessionManager initialized with Hybrid Storage support");
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setThreadLocalMode() {
        String previousMode = executionMode.get();
        String previousClass = currentTestClass.get();

        executionMode.set(MODE_THREAD_LOCAL);
        currentTestClass.remove();

        log.debug("[Thread-{}] Mode changed: {} → {} | Class: {} → null",
                Thread.currentThread().getId(),
                previousMode, MODE_THREAD_LOCAL,
                previousClass);
    }

    public String getExecutionMode() {
        return executionMode.get();
    }

    public String getCurrentTestClass() {
        return currentTestClass.get();
    }

    public boolean isClassBasedMode() {
        return MODE_CLASS_BASED.equals(executionMode.get());
    }

    public void setClassBasedMode(String testClassName) {
        if (testClassName == null || testClassName.isBlank()) {
            throw new IllegalArgumentException("Test class name cannot be null or empty");
        }

        String previousMode = executionMode.get();
        String previousClass = currentTestClass.get();

        executionMode.set(MODE_CLASS_BASED);
        currentTestClass.set(testClassName);

        classBasedSessions.computeIfAbsent(testClassName, k -> new ConcurrentHashMap<>());

        migrateThreadLocalToClassBased(testClassName);

        log.debug("[Thread-{}] Mode changed: {} → {} | Class: {} → {}",
                Thread.currentThread().getId(),
                previousMode, MODE_CLASS_BASED,
                previousClass, testClassName);
    }

    private void migrateThreadLocalToClassBased(String testClassName) {
        Map<String, WebDriver> threadLocalMap = threadLocalSessions.get();

        if (threadLocalMap.isEmpty()) {
            return;
        }

        ConcurrentHashMap<String, WebDriver> classMap = classBasedSessions.get(testClassName);

        for (Map.Entry<String, WebDriver> entry : threadLocalMap.entrySet()) {
            String sessionName = entry.getKey();
            WebDriver driver = entry.getValue();

            if (!classMap.containsKey(sessionName)) {
                classMap.put(sessionName, driver);
                log.debug("[Thread-{}] Migrated session '{}' from ThreadLocal to class '{}'",
                        Thread.currentThread().getId(), sessionName, testClassName);
            }
        }

        String currentName = threadLocalCurrentSession.get();
        if (currentName != null) {
            classCurrentSession.putIfAbsent(testClassName, currentName);
        }

        threadLocalMap.clear();
        threadLocalCurrentSession.remove();
    }

    public void addSession(String sessionName, WebDriver driver) {
        validateSessionName(sessionName);
        validateDriver(driver);

        if (isClassBasedMode()) {
            addSessionClassBased(sessionName, driver);
        } else {
            addSessionThreadLocal(sessionName, driver);
        }
    }

    public WebDriver getCurrentDriver() {
        if (isClassBasedMode()) {
            return getCurrentDriverClassBased();
        } else {
            return getCurrentDriverThreadLocal();
        }
    }

    public WebDriver getSession(String sessionName) {
        if (isClassBasedMode()) {
            return getSessionClassBased(sessionName);
        } else {
            return getSessionThreadLocal(sessionName);
        }
    }

    public void switchSession(String sessionName) {
        validateSessionName(sessionName);

        if (isClassBasedMode()) {
            switchSessionClassBased(sessionName);
        } else {
            switchSessionThreadLocal(sessionName);
        }
    }

    public List<String> getSessionNames() {
        if (isClassBasedMode()) {
            return getSessionNamesClassBased();
        } else {
            return getSessionNamesThreadLocal();
        }
    }

    public Map<String, WebDriver> getAllSessions() {
        if (isClassBasedMode()) {
            return getAllSessionsClassBased();
        } else {
            return getAllSessionsThreadLocal();
        }
    }

    public String getCurrentSessionName() {
        if (isClassBasedMode()) {
            String className = currentTestClass.get();
            return className != null ? classCurrentSession.get(className) : null;
        } else {
            return threadLocalCurrentSession.get();
        }
    }


    public void setCurrentSessionName(String sessionName) {
        if (isClassBasedMode()) {
            String className = currentTestClass.get();
            if (className == null) {
                throw new IllegalStateException("Cannot set current session: no test class set");
            }

            ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
            if (sessionName != null && (sessions == null || !sessions.containsKey(sessionName))) {
                throw new IllegalStateException("Session does not exist: " + sessionName);
            }

            if (sessionName != null) {
                classCurrentSession.put(className, sessionName);
            } else {
                classCurrentSession.remove(className);
            }
        } else {
            Map<String, WebDriver> sessions = threadLocalSessions.get();
            if (sessionName != null && !sessions.containsKey(sessionName)) {
                throw new IllegalStateException("Session does not exist: " + sessionName);
            }
            threadLocalCurrentSession.set(sessionName);
        }
    }


    private void addSessionThreadLocal(String sessionName, WebDriver driver) {
        Map<String, WebDriver> currentThreadMap = threadLocalSessions.get();

        WebDriver existing = currentThreadMap.put(sessionName, driver);
        if (existing != null) {
            log.warn("[Thread-{}] Session '{}' was replaced. Old driver will NOT be closed automatically.",
                    Thread.currentThread().getId(), sessionName);
        }

        log.debug("[Thread-{}] Added session '{}' (ThreadLocal). Total sessions: {}",
                Thread.currentThread().getId(), sessionName, currentThreadMap.size());

        if (threadLocalCurrentSession.get() == null) {
            threadLocalCurrentSession.set(sessionName);
            log.debug("[Thread-{}] Auto-switched to session '{}'",
                    Thread.currentThread().getId(), sessionName);
        }
    }

    private WebDriver getCurrentDriverThreadLocal() {
        String name = threadLocalCurrentSession.get();
        Map<String, WebDriver> currentThreadMap = threadLocalSessions.get();

        if (name != null) {
            WebDriver driver = currentThreadMap.get(name);
            if (driver != null) {
                return driver;
            }
        }

        WebDriver defaultDriver = currentThreadMap.get(DEFAULT_SESSION);
        if (defaultDriver != null) {
            threadLocalCurrentSession.set(DEFAULT_SESSION);
            log.debug("[Thread-{}] Fallback to DEFAULT session", Thread.currentThread().getId());
            return defaultDriver;
        }

        log.warn("[Thread-{}] No active session found. Available: {}",
                Thread.currentThread().getId(), currentThreadMap.keySet());
        return null;
    }

    private WebDriver getSessionThreadLocal(String sessionName) {
        return threadLocalSessions.get().get(sessionName);
    }

    private void switchSessionThreadLocal(String sessionName) {
        Map<String, WebDriver> currentThreadMap = threadLocalSessions.get();

        if (!currentThreadMap.containsKey(sessionName)) {
            log.error("[Thread-{}] Session '{}' not found. Available: {}",
                    Thread.currentThread().getId(), sessionName, currentThreadMap.keySet());
            throw new IllegalArgumentException(
                    "Session not found: " + sessionName +
                            ". Available sessions: " + currentThreadMap.keySet() +
                            ". Make sure you created the session with startSession() first.");
        }

        threadLocalCurrentSession.set(sessionName);
        log.debug("[Thread-{}] Switched to session '{}'",
                Thread.currentThread().getId(), sessionName);
    }

    private List<String> getSessionNamesThreadLocal() {
        return new ArrayList<>(threadLocalSessions.get().keySet());
    }

    private Map<String, WebDriver> getAllSessionsThreadLocal() {
        return new ConcurrentHashMap<>(threadLocalSessions.get());
    }

    // ==================== CLASS-BASED IMPLEMENTATION ====================

    private void addSessionClassBased(String sessionName, WebDriver driver) {
        String className = currentTestClass.get();
        if (className == null) {
            throw new IllegalStateException(
                    "Cannot add session in CLASS_BASED mode: test class name not set. " +
                            "Call setClassBasedMode(className) first.");
        }

        ConcurrentHashMap<String, WebDriver> classMap =
                classBasedSessions.computeIfAbsent(className, k -> new ConcurrentHashMap<>());

        WebDriver existing = classMap.put(sessionName, driver);
        if (existing != null) {
            log.warn("[Thread-{}] Session '{}' for class '{}' was replaced.",
                    Thread.currentThread().getId(), sessionName, className);
        }

        log.debug("[Thread-{}] Added session '{}' for class '{}' (ClassBased). Total sessions: {}",
                Thread.currentThread().getId(), sessionName, className, classMap.size());

        classCurrentSession.putIfAbsent(className, sessionName);
    }

    private WebDriver getCurrentDriverClassBased() {
        String className = currentTestClass.get();
        if (className == null) {
            log.warn("[Thread-{}] CLASS_BASED mode but no test class set. Falling back to ThreadLocal.",
                    Thread.currentThread().getId());
            return getCurrentDriverThreadLocal();
        }

        String currentName = classCurrentSession.get(className);
        if (currentName == null) {
            currentName = DEFAULT_SESSION;
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("[Thread-{}] No sessions found for class '{}'. " +
                            "Make sure sessions were created in @BeforeClass or earlier @Test method.",
                    Thread.currentThread().getId(), className);
            return null;
        }

        WebDriver driver = sessions.get(currentName);
        if (driver == null) {
            log.warn("[Thread-{}] Session '{}' not found for class '{}'. Available: {}",
                    Thread.currentThread().getId(), currentName, className, sessions.keySet());
        }

        return driver;
    }

    private WebDriver getSessionClassBased(String sessionName) {
        String className = currentTestClass.get();
        if (className == null) {
            return null;
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
        return sessions != null ? sessions.get(sessionName) : null;
    }

    private void switchSessionClassBased(String sessionName) {
        String className = currentTestClass.get();
        if (className == null) {
            throw new IllegalStateException(
                    "Cannot switch session in CLASS_BASED mode: test class name not set. " +
                            "Call setClassBasedMode(className) first.");
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
        if (sessions == null || !sessions.containsKey(sessionName)) {
            Set<String> available = sessions != null ? sessions.keySet() : Set.of();
            log.error("[Thread-{}] Session '{}' not found for class '{}'. Available: {}",
                    Thread.currentThread().getId(), sessionName, className, available);
            throw new IllegalArgumentException(
                    "Session '" + sessionName + "' not found for class '" + className + "'. " +
                            "Available sessions: " + available + ". " +
                            "This may happen if:\n" +
                            "  1. The session was created in a different test class\n" +
                            "  2. The session name is misspelled\n" +
                            "  3. The session was already closed");
        }

        classCurrentSession.put(className, sessionName);
        log.debug("[Thread-{}] Switched to session '{}' for class '{}'",
                Thread.currentThread().getId(), sessionName, className);
    }

    private List<String> getSessionNamesClassBased() {
        String className = currentTestClass.get();
        if (className == null) {
            return new ArrayList<>();
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
        return sessions != null ? new ArrayList<>(sessions.keySet()) : new ArrayList<>();
    }

    private Map<String, WebDriver> getAllSessionsClassBased() {
        String className = currentTestClass.get();
        if (className == null) {
            return new ConcurrentHashMap<>();
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
        return sessions != null ? new ConcurrentHashMap<>(sessions) : new ConcurrentHashMap<>();
    }


    public boolean isSessionAlive(String sessionName) {
        WebDriver driver = getSession(sessionName);
        if (driver == null) {
            return false;
        }

        if (driver instanceof RemoteWebDriver) {
            RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
            return remoteDriver.getSessionId() != null;
        }

        return true;
    }


    public void removeDeadSession(String sessionName) {
        if (isClassBasedMode()) {
            String className = currentTestClass.get();
            if (className != null) {
                ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
                if (sessions != null) {
                    WebDriver removed = sessions.remove(sessionName);
                    if (removed != null) {
                        log.info("[Thread-{}] Removed dead session '{}' from class '{}'",
                                Thread.currentThread().getId(), sessionName, className);

                        String current = classCurrentSession.get(className);
                        if (sessionName.equals(current)) {
                            classCurrentSession.remove(className);
                        }
                    }
                }
            }
        } else {
            Map<String, WebDriver> sessions = threadLocalSessions.get();
            WebDriver removed = sessions.remove(sessionName);
            if (removed != null) {
                log.info("[Thread-{}] Removed dead session '{}' from ThreadLocal",
                        Thread.currentThread().getId(), sessionName);

                String current = threadLocalCurrentSession.get();
                if (sessionName.equals(current)) {
                    threadLocalCurrentSession.remove();
                }
            }
        }
    }

    public void stopSession(String sessionName) {
        if (isClassBasedMode()) {
            stopSessionClassBased(sessionName);
        } else {
            stopSessionThreadLocal(sessionName);
        }
    }

    private void stopSessionThreadLocal(String sessionName) {
        Map<String, WebDriver> currentThreadMap = threadLocalSessions.get();
        WebDriver driver = currentThreadMap.remove(sessionName);

        if (driver == null) {
            log.warn("[Thread-{}] Session '{}' not found or already closed",
                    Thread.currentThread().getId(), sessionName);
            return;
        }

        try {
            log.info("[Thread-{}] Stopping session '{}'",
                    Thread.currentThread().getId(), sessionName);
            logSessionInfo(driver, sessionName);
            quitWithTimeout(driver, sessionName, QUIT_TIMEOUT_SECONDS);
            log.info("[Thread-{}] Session '{}' stopped successfully",
                    Thread.currentThread().getId(), sessionName);
        } catch (Exception e) {
            log.error("[Thread-{}] Error stopping session '{}': {}",
                    Thread.currentThread().getId(), sessionName, e.getMessage(), e);
        } finally {
            String current = threadLocalCurrentSession.get();
            if (sessionName != null && sessionName.equals(current)) {
                threadLocalCurrentSession.remove();
            }
        }
    }

    private void stopSessionClassBased(String sessionName) {
        String className = currentTestClass.get();
        if (className == null) {
            log.warn("[Thread-{}] Cannot stop session '{}': no test class context",
                    Thread.currentThread().getId(), sessionName);
            return;
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
        if (sessions == null) {
            return;
        }

        WebDriver driver = sessions.remove(sessionName);
        if (driver == null) {
            log.warn("[Thread-{}] Session '{}' not found for class '{}'",
                    Thread.currentThread().getId(), sessionName, className);
            return;
        }

        try {
            log.info("[Thread-{}] Stopping session '{}' for class '{}'",
                    Thread.currentThread().getId(), sessionName, className);
            logSessionInfo(driver, sessionName);
            quitWithTimeout(driver, sessionName, QUIT_TIMEOUT_SECONDS);
            log.info("[Thread-{}] Session '{}' stopped successfully",
                    Thread.currentThread().getId(), sessionName);
        } catch (Exception e) {
            log.error("[Thread-{}] Error stopping session '{}': {}",
                    Thread.currentThread().getId(), sessionName, e.getMessage(), e);
        } finally {
            String current = classCurrentSession.get(className);
            if (sessionName.equals(current)) {
                classCurrentSession.remove(className);
            }
        }
    }

    /**
     * Stop all sessions for the current context (ThreadLocal or current class).
     */
    public void stopAllSessions() {
        if (isClassBasedMode()) {
            String className = currentTestClass.get();
            if (className != null) {
                stopAllSessionsForClass(className);
            }
        } else {
            stopAllSessionsThreadLocal();
        }
    }

    private void stopAllSessionsThreadLocal() {
        Map<String, WebDriver> currentThreadMap = threadLocalSessions.get();

        if (currentThreadMap.isEmpty()) {
            log.debug("[Thread-{}] No sessions to stop", Thread.currentThread().getId());
            return;
        }

        log.info("[Thread-{}] Stopping {} session(s): {}",
                Thread.currentThread().getId(),
                currentThreadMap.size(),
                currentThreadMap.keySet());

        List<String> failedSessions = new ArrayList<>();

        for (Map.Entry<String, WebDriver> entry : currentThreadMap.entrySet()) {
            String sessionName = entry.getKey();
            WebDriver driver = entry.getValue();

            if (driver == null) {
                continue;
            }

            try {
                logSessionInfo(driver, sessionName);
                quitWithTimeout(driver, sessionName, QUIT_TIMEOUT_SECONDS);
                log.info("[Thread-{}] Session '{}' closed successfully",
                        Thread.currentThread().getId(), sessionName);
            } catch (Exception e) {
                log.error("[Thread-{}] Failed to close session '{}': {}",
                        Thread.currentThread().getId(), sessionName, e.getMessage(), e);
                failedSessions.add(sessionName);
            }
        }

        currentThreadMap.clear();
        threadLocalCurrentSession.remove();

        if (!failedSessions.isEmpty()) {
            log.warn("[Thread-{}] Failed to close {} session(s): {}",
                    Thread.currentThread().getId(), failedSessions.size(), failedSessions);
        }
    }

    public void stopAllSessionsForClass(String className) {
        if (className == null) {
            log.warn("Cannot stop sessions: className is null");
            return;
        }

        ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.remove(className);
        classCurrentSession.remove(className);

        if (sessions == null || sessions.isEmpty()) {
            log.debug("No sessions to stop for class '{}'", className);
            return;
        }

        log.info("Stopping {} session(s) for class '{}': {}",
                sessions.size(), className, sessions.keySet());

        List<String> failedSessions = new ArrayList<>();

        for (Map.Entry<String, WebDriver> entry : sessions.entrySet()) {
            String sessionName = entry.getKey();
            WebDriver driver = entry.getValue();

            if (driver == null) {
                continue;
            }

            try {
                logSessionInfo(driver, sessionName);
                quitWithTimeout(driver, sessionName, QUIT_TIMEOUT_SECONDS);
                log.info("Session '{}' for class '{}' closed successfully", sessionName, className);
            } catch (Exception e) {
                log.error("Failed to close session '{}' for class '{}': {}",
                        sessionName, className, e.getMessage(), e);
                failedSessions.add(sessionName);
            }
        }

        if (!failedSessions.isEmpty()) {
            log.warn("Failed to close {} session(s) for class '{}': {}",
                    failedSessions.size(), className, failedSessions);
        } else {
            log.info("All sessions for class '{}' closed successfully", className);
        }
    }

    public void cleanupThread() {
        stopAllSessionsThreadLocal();
        threadLocalSessions.remove();
        threadLocalCurrentSession.remove();
        executionMode.remove();
        currentTestClass.remove();
        log.info("[Thread-{}] ThreadLocal resources cleaned up", Thread.currentThread().getId());
    }

    /**
     * Clean up all resources (both ThreadLocal and class-based).
     * WARNING: This affects all test classes. Use with caution.
     */
    public void cleanupAll() {
        // Clean ThreadLocal
        cleanupThread();

        // Clean all class-based sessions
        for (String className : new ArrayList<>(classBasedSessions.keySet())) {
            stopAllSessionsForClass(className);
        }

        classBasedSessions.clear();
        classCurrentSession.clear();

        log.info("All SessionManager resources cleaned up");
    }


    private void validateSessionName(String sessionName) {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException(
                    "Session name cannot be null or empty. " +
                            "Please provide a unique identifier (e.g., 'userA', 'admin', 'default').");
        }
    }

    private void validateDriver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver cannot be null");
        }
    }

    private void logSessionInfo(WebDriver driver, String sessionName) {
        try {
            if (driver instanceof RemoteWebDriver) {
                RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
                String sessionId = remoteDriver.getSessionId() != null
                        ? remoteDriver.getSessionId().toString()
                        : "unknown";
                String browserName = remoteDriver.getCapabilities().getBrowserName();
                String browserVersion = remoteDriver.getCapabilities().getBrowserVersion();

                log.info("[Thread-{}] Closing session '{}' | ID: {} | Browser: {} {}",
                        Thread.currentThread().getId(),
                        sessionName,
                        sessionId,
                        browserName,
                        browserVersion);
            } else {
                log.info("[Thread-{}] Closing LOCAL session '{}' | Type: {}",
                        Thread.currentThread().getId(),
                        sessionName,
                        driver.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.debug("Cannot log session info for '{}': {}", sessionName, e.getMessage());
        }
    }

    private void quitWithTimeout(WebDriver driver, String sessionName, int timeoutSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<?> future = executor.submit(() -> {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Exception during quit for session '{}': {}", sessionName, e.getMessage());
                throw e;
            }
        });

        try {
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("⚠️ TIMEOUT ({} seconds) while quitting session '{}'", timeoutSeconds, sessionName);
            future.cancel(true);

            if (driver instanceof RemoteWebDriver) {
                forceCloseRemoteSession((RemoteWebDriver) driver, sessionName);
            }
        } catch (Exception e) {
            log.error("Error during quit for session '{}': {}", sessionName, e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private void forceCloseRemoteSession(RemoteWebDriver driver, String sessionName) {
        try {
            String sessionId = driver.getSessionId() != null
                    ? driver.getSessionId().toString()
                    : null;

            if (sessionId == null) {
                log.warn("Cannot force close session '{}': sessionId is null", sessionName);
                return;
            }

            String gridUrl = getGridUrl();
            if (gridUrl == null || gridUrl.isEmpty()) {
                log.warn("Cannot force close: Grid URL not configured");
                return;
            }

            String deleteUrl = gridUrl + "/session/" + sessionId;
            log.info("Force closing session via Grid API: {}", deleteUrl);

            HttpURLConnection conn = (HttpURLConnection) new URL(deleteUrl).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 404) {
                log.info("Session '{}' force closed successfully (code: {})", sessionName, responseCode);
            } else {
                log.warn("Force close returned unexpected code: {}", responseCode);
            }

            conn.disconnect();

        } catch (IOException e) {
            log.error("Failed to force close remote session '{}': {}", sessionName, e.getMessage());
        }
    }

    private String getGridUrl() {
        String url = ConfigReader.getProperty("grid.url");
        if (url == null || url.isEmpty()) {
            url = ConfigReader.getProperty("appium.server.url");
        }
        return url;
    }

    public void printCurrentSessions() {
        String mode = executionMode.get();
        String className = currentTestClass.get();
        log.info("║ Thread: {} (ID: {})",
                Thread.currentThread().getName(),
                Thread.currentThread().getId());
        log.info("║ Mode: {}", mode);
        log.info("║ Test Class: {}", className != null ? className : "N/A");

        if (MODE_CLASS_BASED.equals(mode) && className != null) {
            ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
            String current = classCurrentSession.get(className);
            log.info("║ Current Session: {}", current);
            log.info("║ Sessions for class: {}", sessions != null ? sessions.keySet() : "[]");
        } else {
            Map<String, WebDriver> sessions = threadLocalSessions.get();
            String current = threadLocalCurrentSession.get();
            log.info("║ Current Session: {}", current);
            log.info("║ ThreadLocal Sessions: {}", sessions.keySet());
        }

        log.info("║ All Class-Based Classes: {}", classBasedSessions.keySet());
    }

    /**
     * Get detailed debug info as a string.
     *
     * @return Debug information string
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("SessionManager Debug Info:\n");
        sb.append("  Thread: ").append(Thread.currentThread().getName())
                .append(" (ID: ").append(Thread.currentThread().getId()).append(")\n");
        sb.append("  Mode: ").append(executionMode.get()).append("\n");
        sb.append("  Test Class: ").append(currentTestClass.get()).append("\n");

        if (isClassBasedMode()) {
            String className = currentTestClass.get();
            if (className != null) {
                ConcurrentHashMap<String, WebDriver> sessions = classBasedSessions.get(className);
                sb.append("  Current Session: ").append(classCurrentSession.get(className)).append("\n");
                sb.append("  Sessions: ").append(sessions != null ? sessions.keySet() : "[]").append("\n");
            }
        } else {
            sb.append("  Current Session: ").append(threadLocalCurrentSession.get()).append("\n");
            sb.append("  Sessions: ").append(threadLocalSessions.get().keySet()).append("\n");
        }

        sb.append("  All Class-Based Classes: ").append(classBasedSessions.keySet()).append("\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("SessionManager[mode=%s, class=%s, thread=%d, sessions=%s]",
                executionMode.get(),
                currentTestClass.get(),
                Thread.currentThread().getId(),
                getSessionNames());
    }
}