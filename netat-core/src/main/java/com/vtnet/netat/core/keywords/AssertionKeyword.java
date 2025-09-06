package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import io.qameta.allure.Step;

public class AssertionKeyword extends BaseKeyword {

    @NetatKeyword(
            name = "assertAll",
            description = "Tổng hợp tất cả các lỗi đã gặp trong chế độ Soft Assert. Nếu có lỗi, kịch bản sẽ FAILED tại đây.",
            category = "ASSERTION",
            example = "assertAll | |"
    )
    @Step("Tổng hợp kết quả Soft Assert")
    public void assertAll() {
        execute(() -> {
            ExecutionContext context = ExecutionContext.getInstance();
            if (context.getSoftAssert() != null) {
                context.getSoftAssert().assertAll();
            }
            // Reset lại sau khi assert
            context.setSoftAssert(null);
            return null;
        });
    }
}