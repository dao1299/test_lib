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
import java.util.concurrent.*;

public final class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    public static final String DEFAULT_SESSION = "default";
    private static final int QUIT_TIMEOUT_SECONDS = 10;

    private static final SessionManager INSTANCE = new SessionManager();

    private final ThreadLocal<Map<String, WebDriver>> sessionMap =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private final ThreadLocal<String> currentSessionName = new ThreadLocal<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void addSession(String sessionName, WebDriver driver) {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("sessionName must not be null/blank");
        }
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }

        Map<String, WebDriver> currentThreadMap = sessionMap.get();
        currentThreadMap.put(sessionName, driver);

        log.debug("[Thread-{}] Added session '{}'. Total sessions: {}",
                Thread.currentThread().getId(), sessionName, currentThreadMap.size());

        if (currentSessionName.get() == null) {
            currentSessionName.set(sessionName);
            log.debug("[Thread-{}] Auto-switched to session '{}'",
                    Thread.currentThread().getId(), sessionName);
        }
    }

    public void switchSession(String sessionName) {
        Map<String, WebDriver> currentThreadMap = sessionMap.get();

        if (!currentThreadMap.containsKey(sessionName)) {
            log.error("[Thread-{}] Session '{}' not found. Available: {}",
                    Thread.currentThread().getId(), sessionName, currentThreadMap.keySet());
            throw new IllegalArgumentException("Không tìm thấy phiên làm việc: " + sessionName);
        }

        currentSessionName.set(sessionName);
        log.debug("[Thread-{}] Switched to session '{}'",
                Thread.currentThread().getId(), sessionName);
    }

    // ==================== GET SESSION ====================

    public WebDriver getSession(String sessionName) {
        return sessionMap.get().get(sessionName);
    }

    public WebDriver getCurrentDriver() {
        String name = currentSessionName.get();
        Map<String, WebDriver> currentThreadMap = sessionMap.get();

        if (name != null) {
            WebDriver driver = currentThreadMap.get(name);
            if (driver != null) {
                return driver;
            }
        }

        WebDriver defaultDriver = currentThreadMap.get(DEFAULT_SESSION);
        if (defaultDriver != null) {
            currentSessionName.set(DEFAULT_SESSION);
            log.debug("[Thread-{}] Fallback to DEFAULT session",
                    Thread.currentThread().getId());
            return defaultDriver;
        }

        log.warn("[Thread-{}] No active session found. Available: {}",
                Thread.currentThread().getId(), currentThreadMap.keySet());
        return null;
    }

    public void stopSession(String sessionName) {
        Map<String, WebDriver> currentThreadMap = sessionMap.get();
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
            String current = currentSessionName.get();
            if (sessionName != null && sessionName.equals(current)) {
                currentSessionName.remove();
                log.debug("[Thread-{}] Cleared current session name",
                        Thread.currentThread().getId());
            }
        }
    }

    public void stopAllSessions() {
        Map<String, WebDriver> currentThreadMap = sessionMap.get();

        if (currentThreadMap.isEmpty()) {
            log.debug("[Thread-{}] No sessions to stop",
                    Thread.currentThread().getId());
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
                log.warn("[Thread-{}] Session '{}' has null driver, skipping",
                        Thread.currentThread().getId(), sessionName);
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

        // ✅ Clear map regardless
        currentThreadMap.clear();
        currentSessionName.remove();

        // ✅ Report failures
        if (!failedSessions.isEmpty()) {
            log.warn("[Thread-{}] Failed to close {} session(s): {}",
                    Thread.currentThread().getId(),
                    failedSessions.size(),
                    failedSessions);
        } else {
            log.info("[Thread-{}] All sessions closed successfully",
                    Thread.currentThread().getId());
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

                log.info("[Thread-{}] Closing REMOTE session '{}' | ID: {} | Browser: {} {}",
                        Thread.currentThread().getId(),
                        sessionName,
                        sessionId,
                        browserName,
                        browserVersion);
            } else {
                log.info("[Thread-{}] Closing LOCAL session '{}'",
                        Thread.currentThread().getId(), sessionName);
            }
        } catch (Exception e) {
            log.debug("[Thread-{}] Cannot log session info for '{}': {}",
                    Thread.currentThread().getId(), sessionName, e.getMessage());
        }
    }

    private void quitWithTimeout(WebDriver driver, String sessionName, int timeoutSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<?> future = executor.submit(() -> {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("[Thread-{}] Exception during quit for session '{}': {}",
                        Thread.currentThread().getId(), sessionName, e.getMessage());
                throw e;
            }
        });

        try {
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("[Thread-{}] ⚠️ TIMEOUT ({} seconds) while quitting session '{}'",
                    Thread.currentThread().getId(), timeoutSeconds, sessionName);
            future.cancel(true);

            if (driver instanceof RemoteWebDriver) {
                forceCloseRemoteSession((RemoteWebDriver) driver, sessionName);
            }
        } catch (Exception e) {
            log.error("[Thread-{}] Error during quit for session '{}': {}",
                    Thread.currentThread().getId(), sessionName, e.getMessage());
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
                log.warn("[Thread-{}] Cannot force close session '{}': sessionId is null",
                        Thread.currentThread().getId(), sessionName);
                return;
            }

            String gridUrl = getGridUrl();
            if (gridUrl == null || gridUrl.isEmpty()) {
                log.warn("[Thread-{}] Cannot force close: Grid URL not configured",
                        Thread.currentThread().getId());
                return;
            }

            String deleteUrl = gridUrl + "/session/" + sessionId;
            log.info("[Thread-{}] Force closing session via Grid API: {}",
                    Thread.currentThread().getId(), deleteUrl);

            HttpURLConnection conn = (HttpURLConnection) new URL(deleteUrl).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 404) {
                log.info("[Thread-{}] Session '{}' force closed successfully (code: {})",
                        Thread.currentThread().getId(), sessionName, responseCode);
            } else {
                log.warn("[Thread-{}] Force close returned unexpected code: {}",
                        Thread.currentThread().getId(), responseCode);
            }

            conn.disconnect();

        } catch (IOException e) {
            log.error("[Thread-{}] Failed to force close remote session '{}': {}",
                    Thread.currentThread().getId(), sessionName, e.getMessage());
        }
    }


    private String getGridUrl() {
        String url = ConfigReader.getProperty("grid.url");
        if (url == null || url.isEmpty()) {
            url = ConfigReader.getProperty("remote.hub.url");
        }
        if (url == null || url.isEmpty()) {
            url = ConfigReader.getProperty("web.remote.hub.url");
        }
        if (url == null || url.isEmpty()) {
            url = ConfigReader.getProperty("device.farm.hub.url");
        }
        return url;
    }

    public String getCurrentSessionName() {
        return currentSessionName.get();
    }

    public void setCurrentSessionName(String sessionName) {
        Map<String, WebDriver> currentThreadMap = sessionMap.get();

        if (sessionName != null && !currentThreadMap.containsKey(sessionName)) {
            throw new IllegalStateException("Session chưa tồn tại: " + sessionName);
        }
        currentSessionName.set(sessionName);
    }

    public void cleanupThread() {
        stopAllSessions();
        sessionMap.remove();
        currentSessionName.remove();
        log.info("[Thread-{}] ThreadLocal cleaned up", Thread.currentThread().getId());
    }

    public void printCurrentSessions() {
        Map<String, WebDriver> currentThreadMap = sessionMap.get();
        String current = currentSessionName.get();

        log.info("[Thread-{}] Current session: '{}', Total sessions: {}, Names: {}",
                Thread.currentThread().getId(), current,
                currentThreadMap.size(), currentThreadMap.keySet());
    }
}