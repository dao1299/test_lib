package com.vtnet.netat.core.utils;

public class SecureText {
    private final String value;

    public SecureText(String value) {
        // Trong một hệ thống thực tế, bạn có thể mã hóa giá trị ở đây
        this.value = value;
    }

    /**
     * Lấy giá trị thật (plain text) của chuỗi nhạy cảm.
     */
    public String getValue() {
        // Và giải mã nó ở đây nếu cần
        return this.value;
    }

    /**
     * Đây là phương thức quan trọng nhất.
     * Khi đối tượng này được chuyển thành chuỗi, nó sẽ chỉ hiển thị chuỗi này.
     */
    @Override
    public String toString() {
        return "********";
    }
}