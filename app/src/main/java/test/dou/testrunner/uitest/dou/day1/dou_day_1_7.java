package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step7 Phone & Messages (30min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step7 Phone & Messages
 * <br>说明: 深度控件操作，Make / Receive 6 calls and 6 SMS messages / (1. MO call for 3 min. / 2. MT call for 3 min. / 3. Repeat Step 1&2 for 3 times / 4. Send 1 SMS / 5. Receive 1 SMS / 6. Repeat Step 4&5 for 3 times) / 7. Send 3 E-mail (include text and 1 picture)
 */
@LargeTest
public class dou_day_1_7 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_7";

    /** 固定拨打的号码（MO 呼叫目标） */
    private static final String PHONE_NUMBER = "17080951634";

    /** 短信正文 */
    private static final String SMS_BODY = "你好！";

    /** 发送按钮的 content-desc */
    private static final String SEND_BUTTON = "Compose:Draft:Send";

    /** 发邮件按钮的 content-desc */
    private static final String SEND_EMAIL = "com.google.android.gm:id/compose_button";

    @Test
    public void testDoU() throws Exception {
        // 电话与短信：拨出/接听 6 通各 3 分钟、收发 6 条短信、发送 3 封带图邮件
        Log.i(TAG, "[1] 启动电话与短信应用");
        DoUHelper.launchPkg(device, "com.google.android.dialer");
        Log.i(TAG, "[2] 拨出/接听 3 轮（MO/MT 各 3 分钟）");
        for (int i = 1; i <= 3; i++) {
            Log.i(TAG, "第 " + i + " 轮：拨出电话 " + PHONE_NUMBER + " 3 分钟");
            DoUHelper.goHome(device);
            // 通过 shell 直接发起呼叫（shell 身份具备 CALL_PHONE 权限，无需 App 申请）
            device.executeShellCommand("am start -a android.intent.action.CALL -d tel:" + PHONE_NUMBER);
            DoUHelper.sleepSec(3);
            DoUHelper.waitMinutes(3);
            // 通话满 3 分钟后自动挂断
            endCall(device);
            Log.i(TAG, "第 " + i + " 轮：接听电话 3 分钟");
            DoUHelper.goHome(device);
            DoUHelper.waitMinutes(3);
            endCall(device);
        }
        Log.i(TAG, "[3] 发送短信 3 轮");
        for (int i = 1; i <= 3; i++) {
            Log.i(TAG, "第 " + i + " 轮：发送短信到 " + PHONE_NUMBER);
            DoUHelper.goHome(device);
            // 通过 SENDTO intent 打开短信界面并预填号码与正文
            device.executeShellCommand("am start -a android.intent.action.SENDTO -d sms:" + PHONE_NUMBER
                    + " --es sms_body \"" + SMS_BODY + "\" --ez exit_on_sent true");
            // 等待短信界面加载（需手动点击发送按钮）
            DoUHelper.sleepSec(10);
            // 点击发送按钮：优先按 content-desc，其次按 resource-id 兜底
            if (!DoUHelper.clickDesc(device, SEND_BUTTON)) {
                DoUHelper.clickID(device, SEND_BUTTON);
            }
            DoUHelper.sleepSec(3);
        }
        Log.i(TAG, "[4] 发送 3 封带图邮件");
        for (int i = 1; i <= 3; i++) {
            DoUHelper.goHome(device);
            DoUHelper.launchPkg(device, "com.google.android.gm");
            DoUHelper.sleepSec(5);
            for (int o = 1; o <= 3; o++) {
                if (DoUHelper.existsID(device, SEND_EMAIL)) {
                    DoUHelper.clickID(device, SEND_EMAIL);
                } else {
                    DoUHelper.goBack(device);
                }
            }
            DoUHelper.inputText(device, "你好!");
            DoUHelper.sleepSec(2);
            DoUHelper.clickID(device,"com.google.android.gm:id/send");
            DoUHelper.sleepSec(2);
        }
    }

    /**
     * 挂断当前通话：通过 shell 发送 KEYCODE_ENDCALL 按键事件。
     * shell 身份可直接触发该系统按键，无需 App 具备通话权限。
     */
    private void endCall(androidx.test.uiautomator.UiDevice device) {
        if (device == null) {
            return;
        }
        try {
            device.executeShellCommand("input keyevent KEYCODE_ENDCALL");
            device.waitForIdle(1000);
            Log.i(TAG, "已发送挂断按键 KEYCODE_ENDCALL");
        } catch (Exception e) {
            Log.w(TAG, "挂断失败", e);
        }
    }
}
