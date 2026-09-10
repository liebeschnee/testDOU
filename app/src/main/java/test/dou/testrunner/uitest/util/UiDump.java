package test.dou.testrunner.uitest.util;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * UI 树 dump 与截图工具（对应文章中 dumpWindowHierarchy 用法的封装）。
 */
public final class UiDump {

    private static final String CHARSET = "UTF-8";

    private UiDump() {
    }

    /** 获取目标应用内部存储下的 TestDrive 目录 */
    private static File getOutDir() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File base = ctx.getFilesDir();
        if (base == null) {
            return new File("/data/local/tmp/TestDrive");
        }
        return new File(base, "TestDrive");
    }

    /** dumpWindowHierarchy：把当前窗口 UI 树写入文件并打日志 */
    public static void dump(UiDevice device) {
        File dir = getOutDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        File out = new File(dir, "ui_" + time + ".xml");
        try {
            // uiautomator2：通过 dumpWindowHierarchy 导出窗口层级
            device.dumpWindowHierarchy(out);
        } catch (IOException e) {
            throw new RuntimeException("dumpWindowHierarchy 失败", e);
        }
    }

    /** 在当前窗口查找指定文本（返回定位到的对象，未找到为 null） */
    public static UiObject findText(UiDevice device, String text) {
        final int maxRetry = 5;
        for (int i = 0; i < maxRetry; i++) {
            UiObject obj = device.findObject(new UiSelector().text(text));
            if (obj != null && obj.exists()) {
                return obj;
            }
            device.waitForIdle(1000);
        }
        return null;
    }

    /** 断言文本可见，超时则抛出 AssertionError */
    public static void assertTextVisible(UiDevice device, String text, long waitMs) {
        long deadline = System.currentTimeMillis() + waitMs;
        while (System.currentTimeMillis() < deadline) {
            UiObject obj = device.findObject(new UiSelector().text(text));
            if (obj != null && obj.exists()) {
                return;
            }
            device.waitForIdle(1000);
        }
        throw new AssertionError("未在 " + waitMs + "ms 内定位到文本: " + text);
    }

    /** 截图到目标应用私有外部存储 TestDrive/step_<name>_<ts>.png */
    public static void capture(UiDevice device, String name) {
        File dir = getOutDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        File png = new File(dir, "step_" + sanitize(name) + "_" + time + ".png");
        try {
            device.takeScreenshot(png);
        } catch (Exception e) {
            throw new RuntimeException("截图失败: " + png, e);
        }
    }

    private static String sanitize(String s) {
        return s == null ? "none" : s.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}