package test.dou.testrunner.uitest;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import test.dou.testrunner.uitest.util.DoUHelper;
import test.dou.testrunner.uitest.util.UiDump;

import org.junit.After;
import org.junit.Before;

/**
 * uiautomator 用例基类：统一初始化 UiDevice，并提供通用断言/截图辅助。
 * 每个场景测试前记录一次电量（DOU_BATTERY_BEFORE）、测试后再记录一次（DOU_BATTERY_AFTER），
 * 通过 System.out 输出到 logcat（tag=System.out），由 Host 端 TestLauncher 读取。
 *
 * <p>每个用例执行期间，{@link PopupMonitor} 后台守护线程轮询屏幕，
 * 自动关闭匹配的系统弹框（ANR、权限、更新提示等），避免弹框阻断测试。
 */
public abstract class UiAutoTestCase {

    protected UiDevice device;
    private String batteryBefore;
    private PopupMonitor popupMonitor;

    @Before
    public void setUp() {
        // 先记录测试前电量（仅依赖 Context，与 UiAutomation 无关），
        // 即使后续 UiAutomation 连接失败导致进程异常，测试前电量也不会丢失
        batteryBefore = DoUHelper.readBattery();
        System.out.println("DOU_BATTERY_BEFORE:" + batteryBefore);
        // 再通过 instrumentation 拿到设备控制器（uiautomator2）
        try {
            device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        } catch (Exception e) {
            System.out.println("DOU_UI_CONNECT_FAIL:" + e);
            device = null;
        }
        // 启动后台弹框监控（device 为 null 时监控无实际效果，不报错）
        popupMonitor = new PopupMonitor(device);
        popupMonitor.start();
    }

    @After
    public void tearDown() {
        // 先停止弹框监控，避免 tearDown 期间误关即将出现的对话框
        if (popupMonitor != null) {
            popupMonitor.stop();
            popupMonitor = null;
        }
        String batteryAfter = DoUHelper.readBattery();
        System.out.println("DOU_BATTERY_AFTER:" + batteryAfter);
    }

    /** 断言屏幕上存在指定文本，超时轮询 */
    protected void assertTextVisible(String text, long waitMs) {
        UiDump.assertTextVisible(device, text, waitMs);
    }

    /** 每个关键步骤截图留档 */
    protected void captureStep(String name) {
        UiDump.capture(device, name);
    }
}