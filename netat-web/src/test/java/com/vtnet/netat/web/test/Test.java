package com.vtnet.netat.web.test;// src/test/java/com/vtnet/netat/web/test/Test.java (Test case đã sửa)

import com.vtnet.netat.web.keywords.WebKeywords;
import com.vtnet.netat.web.elements.UIObjectRepository;
import com.vtnet.netat.web.elements.NetatUIObject;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        // Không cần khởi tạo WebKeywords nữa, gọi trực tiếp các phương thức static
        WebKeywords.openBrowser("chrome","https://www.automationexercise.com/login");

        // Lấy NetatUIObject từ UIObjectRepository (đảm bảo file JSON edtUsername tồn tại đúng đường dẫn)
        // và UIObjectRepository đã được triển khai.
        NetatUIObject edtUsername = UIObjectRepository.getInstance().getUIObjectByPath("edtUsername");

        // Sử dụng NetatUIObject trực tiếp với keyword static
        WebKeywords.inputText(edtUsername, "123");

        Thread.sleep(10000); // Đợi để quan sát

        WebKeywords.closeBrowser(); // Đừng quên đóng trình duyệt
    }
}