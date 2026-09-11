package test.dou.testrunner.uitest;

import android.util.Log;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 后台弹框监控：在测试用例执行期间，后台守护线程轮询屏幕，
 * 发现匹配的弹框/对话框元素后自动点击关闭，避免弹框阻断测试。
 *
 * <p>支持三种匹配方式：
 * <ul>
 *   <li>{@link MatchType#TEXT} — 按文本匹配（textContains）</li>
 *   <li>{@link MatchType#ID} — 按资源ID匹配（resourceId 完全匹配）</li>
 *   <li>{@link MatchType#DESC} — 按描述匹配（descriptionContains）</li>
 * </ul>
 *
 * <p>预置了常见系统弹框按钮（ANR、权限、更新提示、清理优化等），
 * 也可通过 {@link #addTarget} 动态添加自定义目标。
 */
public class PopupMonitor {

    private static final String TAG = "PopupMonitor";
    private static final long POLL_INTERVAL_MS = 2000;

    /** 匹配类型 */
    public enum MatchType {
        /** 按文本匹配（textContains，模糊） */
        TEXT,
        /** 按资源ID匹配（resourceId，精确） */
        ID,
        /** 按描述匹配（descriptionContains，模糊） */
        DESC
    }

    /** 一个弹框关闭目标：匹配方式 + 匹配值，匹配到后点击该元素 */
    public static class DismissTarget {
        public final MatchType type;
        public final String value;

        public DismissTarget(MatchType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "[" + type + "] " + value;
        }
    }

    private final UiDevice device;
    private final List<DismissTarget> targets = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread monitorThread;

    public PopupMonitor(UiDevice device) {
        this.device = device;
        initDefaultTargets();
    }

    // ===================== 预置目标 =====================

    /**
     * 预置常见系统弹框关闭目标。
     * 注意：列表中靠前的目标优先匹配。如同时有"允许"和"拒绝"，
     * 先匹配到"允许"会点击它并关闭弹框，"拒绝"不会被重复点击。
     * 如需调整优先级，修改此方法即可。
     */
    private void initDefaultTargets() {
        // --- 权限弹框（优先允许，避免阻断测试流程） ---
        addTarget(MatchType.TEXT, "允许");
        addTarget(MatchType.TEXT, "仅在使用中允许");
        addTarget(MatchType.TEXT, "仅在使用该应用时允许");
        addTarget(MatchType.TEXT, "始终允许");
        addTarget(MatchType.TEXT, "全部允许");
        addTarget(MatchType.TEXT, "不用了");
        addTarget(MatchType.TEXT, "While using the app");
        addTarget(MatchType.TEXT, "Allow");
        addTarget(MatchType.TEXT, "Only this time");

        // --- ANR / 无响应弹框（优先等待，避免强杀被测应用） ---
        addTarget(MatchType.TEXT, "等待");
        addTarget(MatchType.TEXT, "Wait");
        addTarget(MatchType.TEXT, "强制关闭");
        addTarget(MatchType.TEXT, "Force close");
        addTarget(MatchType.TEXT, "关闭应用");
        addTarget(MatchType.TEXT, "Close app");

        // --- 系统更新 / 优化提示 ---
        addTarget(MatchType.TEXT, "稍后");
        addTarget(MatchType.TEXT, "Not now");
        addTarget(MatchType.TEXT, "以后再说");
        addTarget(MatchType.TEXT, "Later");
        addTarget(MatchType.TEXT, "不再提示");
        addTarget(MatchType.TEXT, "Don't show again");

        // --- 清理 / 优化弹框 ---
        addTarget(MatchType.TEXT, "立即清理");
        addTarget(MatchType.TEXT, "忽略");
        addTarget(MatchType.TEXT, "Ignore");

        // --- 通用确定 / 关闭 / 取消 ---
        addTarget(MatchType.TEXT, "确定");
        addTarget(MatchType.TEXT, "OK");
        addTarget(MatchType.TEXT, "知道了");
        addTarget(MatchType.TEXT, "Got it");
        addTarget(MatchType.TEXT, "关闭");
        addTarget(MatchType.TEXT, "取消");
        addTarget(MatchType.TEXT, "Cancel");

        // --- ID 匹配：系统对话框标准按钮 ---
        addTarget(MatchType.ID, "android:id/button1");   // 确定 / 强制关闭
        addTarget(MatchType.ID, "android:id/button2");   // 取消 / 等待
        addTarget(MatchType.ID, "android:id/button3");   // 中性按钮（以后再说等）
        addTarget(MatchType.ID, "android:id/dismiss");   // 关闭 / 叉号

        // --- DESC 匹配：关闭按钮 / 叉号 ---
        addTarget(MatchType.DESC, "关闭");
        addTarget(MatchType.DESC, "dismiss");
    }

    // ===================== 自定义目标 =====================

    /** 添加自定义弹框关闭目标 */
    public void addTarget(MatchType type, String value) {
        targets.add(new DismissTarget(type, value));
    }

    /** 添加多个自定义目标 */
    public void addTargets(List<DismissTarget> custom) {
        if (custom != null) {
            targets.addAll(custom);
        }
    }

    /** 清除所有目标（含预置），之后用 {@link #addTarget} 重新配置 */
    public void clearTargets() {
        targets.clear();
    }

    /** 获取当前所有目标列表（含预置），可直接增删 */
    public List<DismissTarget> getTargets() {
        return targets;
    }

    // ===================== 生命周期 =====================

    /** 启动后台监控 */
    public void start() {
        if (running.compareAndSet(false, true)) {
            monitorThread = new Thread(this::monitorLoop, "popup-monitor");
            monitorThread.setDaemon(true);
            monitorThread.start();
            Log.i(TAG, "弹框监控已启动，共 " + targets.size() + " 个目标");
        }
    }

    /** 停止后台监控 */
    public void stop() {
        running.set(false);
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
        Log.i(TAG, "弹框监控已停止");
    }

    public boolean isRunning() {
        return running.get();
    }

    // ===================== 监控循环 =====================

    private void monitorLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (device != null) {
                    scanAndDismiss();
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Log.w(TAG, "监控轮询异常: " + e.getMessage());
            }
        }
    }

    /** 扫描屏幕，逐个尝试匹配并点击关闭弹框 */
    private void scanAndDismiss() {
        for (DismissTarget t : targets) {
            if (!running.get()) break;
            try {
                UiSelector selector = buildSelector(t);
                UiObject obj = device.findObject(selector);
                if (obj != null && obj.exists()) {
                    boolean clicked = obj.click();
                    Log.i(TAG, "弹框关闭: " + t + " → click=" + clicked);
                    System.out.println("DOU_POPUP_DISMISSED:" + t.type + "|" + t.value);
                    device.waitForIdle(500);
                }
            } catch (Exception e) {
                // 单个目标匹配失败不影响其他目标
            }
        }
    }

    private UiSelector buildSelector(DismissTarget t) {
        switch (t.type) {
            case ID:
                return new UiSelector().resourceId(t.value);
            case DESC:
                return new UiSelector().descriptionContains(t.value);
            case TEXT:
            default:
                return new UiSelector().textContains(t.value);
        }
    }
}
