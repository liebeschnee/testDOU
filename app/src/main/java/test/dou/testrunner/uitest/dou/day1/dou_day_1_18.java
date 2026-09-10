package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step18 Chrome (30min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step18 Chrome
 * <br>说明: 深度控件操作，Browse webpages from Chrome(for 20 mins)  /  a.Input URL http://tw.yahoo.com from Chrome /  b.Select "Sports" tab to browse webpages / 3.Send 3 E-mail (include text and 1 picture)
 */
@LargeTest
public class dou_day_1_18 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_18";

    @Test
    public void testDoU() throws Exception {
        // Chrome：输入 URL http://tw.yahoo.com，进入体育页浏览，并发送 3 封带图邮件
        Log.i(TAG, "[1] 启动 Chrome");
        DoUHelper.launchPkg(device, "com.android.chrome");
        Log.i(TAG, "[2] 点击地址栏");
        DoUHelper.clickText(device, "Search or type web address");
        Log.i(TAG, "[3] 输入 URL http://tw.yahoo.com 并回车");
        UiObject addressBar = device.findObject(new UiSelector().className("android.widget.EditText"));
        if (addressBar != null && addressBar.exists()) {
            addressBar.setText("http://tw.yahoo.com");
            DoUHelper.pressEnter(device);
        }
        Log.i(TAG, "[4] 点击 Sports 标签");
        DoUHelper.clickText(device, "Sports");
        Log.i(TAG, "[5] 上下滑动浏览 30 分钟");
        DoUHelper.waitAndSwipe(device, 30);
        Log.i(TAG, "[6] 发送 3 封带图邮件");
        for (int i = 1; i <= 3; i++) {
            DoUHelper.goHome(device);
            DoUHelper.launchPkg(device, "com.google.android.gm");
            DoUHelper.sleepSec(5);
        }
    }
}
