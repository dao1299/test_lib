// com/vtnet/netat/core/BaseKeyword.java
package com.vtnet.netat.core;

import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.assertion.SoftFailContext;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.exceptions.StepFailException;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.testng.asserts.SoftAssert;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class BaseKeyword {
    protected final NetatLogger logger = NetatLogger.getInstance(this.getClass());

    protected <T> T execute(Callable<T> logic, Object... params) {
        Method kw = findCallingKeywordMethod();
        if (kw == null) {
            throw new IllegalStateException("Could not find a method with @NetatKeyword in call stack.");
        }
        NetatKeyword meta = kw.getAnnotation(NetatKeyword.class);

        String name = meta.name();
        String paramsStr = Arrays.stream(params).map(p -> Objects.toString(p, "null")).collect(Collectors.joining(", "));
        logger.info("KEYWORD START: {} | Parameters: [{}]", name, paramsStr);

        List<Parameter> allureParams = buildAllureParams(meta, params);

        String displayName = buildDisplayName(meta, name, allureParams, params);

        String uuid = UUID.randomUUID().toString();
        StepResult step = new StepResult()
                .setName(displayName)
                .setParameters(allureParams);
        Allure.getLifecycle().startStep(uuid, step);

        long start = System.currentTimeMillis();
        try {
            SoftFailContext.reset();

            T result = logic.call();

            boolean softFailed = SoftFailContext.consumeHasFail();
            String msgs = SoftFailContext.consumeMessages();

            if (softFailed) {
                final String finalMsg = (msgs == null || msgs.isBlank()) ? "Soft assertions failed." : msgs;
                Allure.getLifecycle().updateStep(s -> {
                    s.setStatus(Status.FAILED);
                    s.setStatusDetails(new StatusDetails().setMessage(finalMsg));
                });
                try { ScreenshotUtils.takeScreenshot(name + "_softfail"); } catch (Throwable ignore) {}
                logger.error("KEYWORD SOFT-FAILED: {} | Duration: {}ms | Messages:\n{}",
                        name, (System.currentTimeMillis() - start), finalMsg);
            } else {
                Allure.getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
                logger.info("KEYWORD SUCCESS: {} | Duration: {}ms", name, (System.currentTimeMillis() - start));
            }
            return result;

        } catch (Throwable t) {
            String msg = (t.getMessage() == null ? t.toString() : t.getMessage());
            Throwable root = t;
            while (root.getCause() != null && root != root.getCause()) {
                root = root.getCause();
            }

            if (t instanceof AssertionError || root instanceof AssertionError) {
                Allure.getLifecycle().updateStep(s -> {
                    s.setStatus(Status.FAILED);
                    s.setStatusDetails(new StatusDetails().setMessage(msg));
                });
                try { ScreenshotUtils.takeScreenshot(name + "_failure"); } catch (Throwable ignore) {}
                // Rethrow as AssertionError để TestNG hiểu là FAIL (hard)
                throw (t instanceof AssertionError) ? (AssertionError) t : new AssertionError(root.getMessage(), root);
            }

            Allure.getLifecycle().updateStep(s -> {
                s.setStatus(Status.BROKEN);
                s.setStatusDetails(new StatusDetails().setMessage(msg));
            });

            String errorMessage = "Keyword '" + name + "' failed with params: [" + paramsStr + "]";
            StepFailException ex = new StepFailException(errorMessage, t, name);
            try { ex.setScreenshotPath(ScreenshotUtils.takeScreenshot(name + "_broken")); } catch (Throwable ignore) {}
            throw ex;

        } finally {
            Allure.getLifecycle().stopStep(uuid);
        }
    }

    private String buildDisplayName(NetatKeyword meta, String fallbackName, List<Parameter> allureParams, Object[] rawArgs) {
        String explainer = meta.explainer();
        if (explainer == null || explainer.isBlank()) {
            return fallbackName;
        }
        return renderExplainer(explainer, allureParams, rawArgs, fallbackName);
    }

    private String renderExplainer(String template,
                                   List<Parameter> allureParams,
                                   Object[] rawArgs,
                                   String fallbackName) {

        String out = template;

        if (allureParams != null) {
            Map<String, String> byName = new HashMap<>();
            for (Parameter p : allureParams) {
                String k = p.getName();
                String v = String.valueOf(p.getValue());
                byName.put(k, v);
            }
            for (Map.Entry<String, String> e : byName.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue());
            }
        }

        if (rawArgs != null) {
            for (int i = 0; i < rawArgs.length; i++) {
                String val = safeString(rawArgs[i]);
                out = out.replace("{" + i + "}", val);
            }
        }

        if (out.contains("{")) {
            return fallbackName;
        }

        if (out.length() > 300) {
            out = out.substring(0, 300) + "…";
        }
        return out;
    }

    private List<Parameter> buildAllureParams(NetatKeyword meta, Object... args) {
        String[] declared = (meta.parameters() == null) ? new String[0] : meta.parameters();
        List<String> names = new ArrayList<>(declared.length);
        for (String line : declared) {
            String n = line;
            int colon = n.indexOf(':');                 // "actualValue: Object - ..." -> lấy phần trước ':'
            if (colon > 0) n = n.substring(0, colon).trim();
            n = (n.isEmpty() ? null : n);
            names.add(n);
        }
        List<Parameter> out = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String pname = (i < names.size() && names.get(i) != null) ? names.get(i) : "arg" + (i + 1);
            String pvalue = safeString(args[i]);
            if (isSensitive(pname)) pvalue = "***";
            out.add(new Parameter().setName(pname).setValue(pvalue));
        }
        return out;
    }

    private String safeString(Object o) {
        String s = Objects.toString(o, "null");
        int MAX = 500;
        if (s.length() > MAX) s = s.substring(0, MAX) + "…(truncated)";
        return s;
    }

    private boolean isSensitive(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.contains("password") || n.contains("token") || n.contains("secret") || n.contains("key");
    }

    private Method findCallingKeywordMethod() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (int i = 3; i < st.length; i++) {
            try {
                Class<?> c = Class.forName(st[i].getClassName());
                if (BaseKeyword.class.isAssignableFrom(c)) {
                    for (Method m : c.getMethods()) {
                        if (m.getName().equals(st[i].getMethodName())
                                && m.isAnnotationPresent(NetatKeyword.class)) {
                            return m;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    protected SoftAssert sa() {
        return ExecutionContext.getInstance().getSoftAssert();
    }
}