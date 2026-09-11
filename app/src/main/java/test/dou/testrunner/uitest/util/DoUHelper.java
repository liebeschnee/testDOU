package test.dou.testrunner.uitest.util;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

/**
 * DoU 场景通用基础操作：应用启动、手势、等待、电量读取。
 * <p>控件相关的操作（启动包名、点击文本/描述、输入、坐标）由各场景脚本内联体现，
 * 便于按机型/控件信息差异直接维护脚本；滑动、唤醒/息屏、等待等通用动作保留在本类。
 */
public final class DoUHelper {

    private static final String TAG = "DoUHelper";

    private DoUHelper() {
    }

    // ===================== 应用启动 =====================

    /** 按包名启动应用（包名即控件信息，写在场景脚本里便于按机型修改）；未安装则回 Home 不阻断。
     *  用 Intent setPackage + ACTION_MAIN/CATEGORY_LAUNCHER，由系统服务端解析，
     *  绕过 Android 11+ 进程级包可见性限制（getLaunchIntentForPackage 会返回 null）。 */
    public static void launchPkg(UiDevice device, String pkg) {
        try {
            Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(pkg);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            device.waitForIdle(3000);
            Log.i(TAG, "已启动: " + pkg);
        } catch (Exception e) {
            Log.w(TAG, "启动失败，回 Home 继续: " + pkg, e);
            goHome(device);
        }
    }

    /** 尝试按包名启动应用，成功返回 true，失败（未安装/无 launcher Activity）返回 false。
     *  供场景脚本逐个尝试多个包名时使用，失败不会回 Home。 */
    public static boolean tryLaunchPkg(UiDevice device, String pkg) {
        try {
            Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(pkg);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            device.waitForIdle(3000);
            Log.i(TAG, "已启动: " + pkg);
            return true;
        } catch (Exception e) {
            Log.i(TAG, "包未安装或无可启动 Activity: " + pkg);
            return false;
        }
    }

    /** 回到主界面 */
    public static void goHome(UiDevice device) {
        device.pressHome();
        device.waitForIdle(1000);
    }

    // ===================== 手势 / 系统动作 =====================

    /** 唤醒并解锁（无密码，上滑解锁） */
    public static void wakeAndUnlock(UiDevice device) {
        try {
            device.wakeUp();
            device.waitForIdle(1000);
        } catch (Exception e) {
            Log.w(TAG, "唤醒设备失败", e);
        }
        swipeUp(device);
        device.waitForIdle(1000);
    }

    /** 息屏待机（息屏前记录一次当前电量） */
    public static void screenOff(UiDevice device) {
        logBattery(device);
        try {
            device.sleep();
            device.waitForIdle(1000);
        } catch (Exception e) {
            Log.w(TAG, "息屏失败", e);
        }
    }

    /** 按住屏幕中央上滑 */
    public static void swipeUp(UiDevice device) {
        int w = device.getDisplayWidth();
        int h = device.getDisplayHeight();
        device.swipe(w / 2, (int) (h * 0.8), w / 2, (int) (h * 0.2), 20);
        device.waitForIdle(800);
    }

    /** 下滑 */
    public static void swipeDown(UiDevice device) {
        int w = device.getDisplayWidth();
        int h = device.getDisplayHeight();
        device.swipe(w / 2, (int) (h * 0.2), w / 2, (int) (h * 0.8), 20);
        device.waitForIdle(800);
    }

    /** 点击屏幕中央（相机快门/占位用） */
    public static void clickScreen(UiDevice device) {
        int w = device.getDisplayWidth();
        int h = device.getDisplayHeight();
        device.click(w / 2, h / 2);
        device.waitForIdle(500);
    }

    /** 持续上下滑动 minutes 分钟（浏览型步骤主体，换机型只需调整 swipeUp/swipeDown 一处） */
    public static void waitAndSwipe(UiDevice device, int minutes) {
        long end = System.currentTimeMillis() + minutes * 60L * 1000L;
        boolean up = true;
        while (System.currentTimeMillis() < end) {
            if (up) {
                swipeUp(device);
            } else {
                swipeDown(device);
            }
            up = !up;
            sleepSec(8);
        }
    }

    /** 系统级 WiFi 开关（uiautomator 无法直接控制系统设置，需 adb svc wifi 或系统设置完成） */
    public static void setWifiEnabled(UiDevice device, boolean enable) {
        Log.i(TAG, "设置 WiFi = " + enable + " (需 adb svc wifi 或系统设置完成)");
    }

    // ===================== 控件操作（供场景脚本内联调用，文本/描述按机型可改） =====================

    /** 等待目标文本出现（超时 10s） */
    public static void waitForVisible(UiDevice device, String text) {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            UiObject obj = device.findObject(new UiSelector().text(text));
            if (obj != null && obj.exists()) {
                return;
            }
            sleepSec(1);
        }
        Log.w(TAG, "超时未看到文本: " + text);
    }

    /** 按精确文本点击，找不到告警不阻断 */
    public static boolean clickText(UiDevice device, String text) {
        UiObject obj = device.findObject(new UiSelector().text(text));
        if (obj != null && obj.exists()) {
            try {
                obj.click();
                Log.i(TAG, "点击文本: " + text);
                device.waitForIdle(800);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "点击失败: " + text, e);
            }
        } else {
            Log.w(TAG, "未找到文本以点击: " + text);
        }
        return false;
    }

    /** 按描述（desc）点击 */
    public static boolean clickDesc(UiDevice device, String desc) {
        UiObject obj = device.findObject(new UiSelector().description(desc));
        if (obj != null && obj.exists()) {
            try {
                obj.click();
                Log.i(TAG, "点击描述: " + desc);
                device.waitForIdle(800);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "点击失败: " + desc, e);
            }
        }
        return false;
    }
    /** 按描述（contains）点击 */
    public static boolean clickDescContains(UiDevice device, String desc) {
        UiObject obj = device.findObject(new UiSelector().descriptionContains(desc));
        if (obj != null && obj.exists()) {
            try {
                obj.click();
                Log.i(TAG, "点击描述: " + desc);
                device.waitForIdle(800);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "点击失败: " + desc, e);
            }
        }
        return false;
    }

    /** 按资源 ID 点击（完整 resourceId 精确匹配，如 "com.android.camera:id/shutter_button"），找不到告警不阻断 */
    public static boolean clickID(UiDevice device, String id) {
        UiObject obj = device.findObject(new UiSelector().resourceId(id));
        if (obj != null && obj.exists()) {
            try {
                obj.click();
                Log.i(TAG, "点击ID: " + id);
                device.waitForIdle(800);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "点击失败: " + id, e);
            }
        } else {
            Log.w(TAG, "未找到ID以点击: " + id);
        }
        return false;
    }

    /** 向焦点输入框输入文本（无焦点时回退到第一个 EditText） */
    public static boolean inputText(UiDevice device, String text) {
        UiObject input = device.findObject(new UiSelector().focused(true));
        if (input == null || !input.exists()) {
            input = device.findObject(new UiSelector().className("android.widget.EditText"));
        }
        if (input != null && input.exists()) {
            try {
                input.setText(text);
                Log.i(TAG, "输入文本: " + text);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "输入失败", e);
            }
        }
        Log.w(TAG, "未找到输入框: " + text);
        return false;
    }

    /** 按回车键 */
    public static void pressEnter(UiDevice device) {
        device.pressEnter();
        device.waitForIdle(800);
    }

    // ===================== 等待 / 电量 =====================

    /** 等待指定分钟（DoU 步骤的主体耗时） */
    public static void waitMinutes(int minutes) {
        long ms = minutes * 60L * 1000L;
        Log.i(TAG, String.format(Locale.US, "等待 %d 分钟 (%d ms)", minutes, ms));
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            sleepSec(10);
        }
    }

    /** 秒级等待 */
    public static void sleepSec(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 读取当前电量并格式化为 "91% / 4525mAh"（百分比 + 剩余容量 mAh），失败返回空串 */
    public static String readBattery() {
        try {
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            int percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            double mah = readMah(bm);
            if (mah > 0) {
                return String.format(Locale.US, "%d%% / %.0fmAh", percent, mah);
            }
            // 部分机型读不到剩余容量，仅展示百分比，避免出现 -1mAh 之类的异常值
            return String.format(Locale.US, "%d%% / --mAh", percent);
        } catch (Exception e) {
            Log.w(TAG, "读取电量失败", e);
            return "";
        }
    }

    /** 记录当前电量：百分比 + 剩余容量 mAh */
    public static void logBattery(UiDevice device) {
        Log.i(TAG, "当前电量: " + readBattery());
    }

    /** 剩余容量 mAh：优先 BatteryManager 的 CHARGE_COUNTER（µAh），读不到时回退 sysfs 节点 */
    private static double readMah(BatteryManager bm) {
        long counter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        if (counter > 0) {
            return counter / 1000.0; // µAh -> mAh
        }
        String[] nodes = {
                "/sys/class/power_supply/battery/charge_now",
                "/sys/class/power_supply/battery/charge_counter"
        };
        for (String node : nodes) {
            try {
                File f = new File(node);
                if (!f.exists()) {
                    continue;
                }
                long uah = Long.parseLong(readFirstLine(f).trim());
                if (uah > 0) {
                    return uah / 1000.0;
                }
            } catch (Exception ignored) {
                // 该节点不可读则尝试下一个
            }
        }
        return -1;
    }

    private static String readFirstLine(File f) throws Exception {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            return r.readLine();
        }
    }
}
