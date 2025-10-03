// com/vtnet/netat/core/assertion/AllureSoftAssert.java
package com.vtnet.netat.core.assertion;

import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

public class AllureSoftAssert extends SoftAssert {

    @Override
    public void onAssertFailure(IAssert<?> a, AssertionError ex) {
        String base = (a.getMessage() != null && !a.getMessage().isBlank())
                ? a.getMessage() : "Soft assertion failed";
        String msg = base
                + " | expected: " + String.valueOf(a.getExpected())
                + " | actual: "   + String.valueOf(a.getActual())
                + (ex != null && ex.getMessage()!=null ? " | error: " + ex.getMessage() : "");

        // ✅ chỉ đánh dấu để BaseKeyword set FAIL cho step cha
        SoftFailContext.markFailed(msg);

        // vẫn lưu failure để assertAll() tổng hợp
        super.onAssertFailure(a, ex);
    }

    @Override
    public void onAssertSuccess(IAssert<?> a) {
        // Không log gì tránh noise
        super.onAssertSuccess(a);
    }
}
