package com.vtnet.netat.driver;

import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý nhiều phiên WebDriver (per-thread) qua tên phiên.
 * Không tham chiếu ngược DriverManager để tránh vòng đệ quy.
 */
public final class SessionManager {

    public static final String DEFAULT_SESSION = "default";

    // Singleton (per-process); bản thân data là per-thread qua ThreadLocal sessionName
    private static final SessionManager INSTANCE = new SessionManager();

    // Lưu driver theo tên phiên cho thread hiện tại
    private final Map<String, WebDriver> sessionMap = new ConcurrentHashMap<>();

    // Tên phiên đang hoạt động (per-thread)
    private static final ThreadLocal<String> currentSessionName = new ThreadLocal<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Thêm một phiên; KHÔNG chuyển active trừ khi caller tự gọi switchSession */
    public void addSession(String sessionName, WebDriver driver) {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("sessionName must not be null/blank");
        }
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        sessionMap.put(sessionName, driver);
        // Nếu chưa có phiên hiện tại, tự động dùng phiên vừa thêm
        if (currentSessionName.get() == null) {
            currentSessionName.set(sessionName);
        }
    }

    /** Chuyển phiên hoạt động */
    public void switchSession(String sessionName) {
        if (!sessionMap.containsKey(sessionName)) {
            throw new IllegalArgumentException("Không tìm thấy phiên làm việc: " + sessionName);
        }
        currentSessionName.set(sessionName);
    }

    /** Lấy driver theo tên phiên (có thể null nếu không tồn tại) */
    public WebDriver getSession(String sessionName) {
        return sessionMap.get(sessionName);
    }

    /**
     * Lấy driver của phiên đang hoạt động.
     * Nếu chưa switch, ưu tiên phiên "default" nếu tồn tại.
     * Tuyệt đối KHÔNG gọi ngược DriverManager để tránh vòng lặp.
     */
    public WebDriver getCurrentDriver() {
        String name = currentSessionName.get();
        if (name != null) {
            return sessionMap.get(name);
        }
        // fallback về default nếu có
        WebDriver d = sessionMap.get(DEFAULT_SESSION);
        if (d != null) {
            // đồng bộ trạng thái currentSessionName để các lần sau nhất quán
            currentSessionName.set(DEFAULT_SESSION);
            return d;
        }
        // Không có phiên nào → trả null (để caller tự xử lý), KHÔNG gọi DriverManager tại đây
        return null;
    }

    /** Đóng & xoá một phiên cụ thể */
    public void stopSession(String sessionName) {
        WebDriver driver = sessionMap.remove(sessionName);
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        // Nếu đang ở phiên này thì xoá trạng thái current
        String cur = currentSessionName.get();
        if (sessionName != null && sessionName.equals(cur)) {
            currentSessionName.remove();
        }
    }

    /** Đóng tất cả phiên của thread hiện tại; KHÔNG gọi DriverManager để tránh vòng lặp */
    public void stopAllSessions() {
        for (WebDriver d : sessionMap.values()) {
            try { d.quit(); } catch (Exception ignored) {}
        }
        sessionMap.clear();
        currentSessionName.remove();
    }

    /** Lấy tên phiên hiện tại (có thể null) */
    public String getCurrentSessionName() {
        return currentSessionName.get();
    }

    /** Đặt trực tiếp current session name (dùng thận trọng) */
    public void setCurrentSessionName(String sessionName) {
        if (sessionName != null && !sessionMap.containsKey(sessionName)) {
            throw new IllegalStateException("Session chưa tồn tại: " + sessionName);
        }
        currentSessionName.set(sessionName);
    }
}
