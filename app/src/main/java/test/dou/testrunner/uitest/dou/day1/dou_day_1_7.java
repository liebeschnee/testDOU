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

    @Test
    public void testDoU() throws Exception {
        // 电话与短信：拨出/接听 6 通各 3 分钟、收发 6 条短信、发送 3 封带图邮件
        Log.i(TAG, "[1] 启动电话与短信应用");
        DoUHelper.launchPkg(device, "com.google.android.dialer");
        Log.i(TAG, "[2] 拨出/接听 3 轮（MO/MT 各 3 分钟）");
        for (int i = 1; i <= 3; i++) {
            Log.i(TAG, "第 " + i + " 轮：拨出电话 3 分钟");
            DoUHelper.goHome(device);
            DoUHelper.launchPkg(device, "com.google.android.dialer");
            DoUHelper.waitMinutes(3);
            Log.i(TAG, "第 " + i + " 轮：接听电话 3 分钟");
            DoUHelper.goHome(device);
            DoUHelper.waitMinutes(3);
        }
        Log.i(TAG, "[3] 收发短信 3 轮");
        for (int i = 1; i <= 3; i++) {
            DoUHelper.goHome(device);
            DoUHelper.launchPkg(device, "com.google.android.dialer");
            DoUHelper.sleepSec(5);
        }
        Log.i(TAG, "[4] 发送 3 封带图邮件");
        for (int i = 1; i <= 3; i++) {
            DoUHelper.goHome(device);
            DoUHelper.launchPkg(device, "com.google.android.gm");
            DoUHelper.sleepSec(5);
        }
    }
}
