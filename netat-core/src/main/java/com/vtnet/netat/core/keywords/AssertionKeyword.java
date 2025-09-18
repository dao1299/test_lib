package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import io.qameta.allure.Step;

public class AssertionKeyword extends BaseKeyword {

    @NetatKeyword(
            name = "assertAll",
            description = "Thực hiện kiểm tra tất cả các soft assertion đã được thu thập trước đó. Nếu có bất kỳ assertion nào thất bại, phương thức sẽ ném ra ngoại lệ với thông tin chi tiết về các lỗi. Sau khi gọi phương thức này, soft assert sẽ được reset về null.",
            category = "Assertion",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Sau khi thực hiện nhiều soft assertion\n" +
                    "softAssert.assertEquals(actualTitle, expectedTitle, \"Title không khớp\");\n" +
                    "softAssert.assertTrue(isElementDisplayed, \"Element không hiển thị\");\n" +
                    "// Thực hiện kiểm tra tất cả các assertion\n" +
                    "assertAll();"
    )
    @Step("Aggregate Soft Assert results")
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