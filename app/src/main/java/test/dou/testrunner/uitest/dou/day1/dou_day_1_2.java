package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step2 Google News & Weather (20min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step2 Google News & Weather
 * <br>说明: 深度控件操作，1.Open this app and choose news type-International news from menu / 2.To browse news (for 20 mins)
 */
@LargeTest
public class dou_day_1_2 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_2";

    @Test
    public void testDoU() throws Exception {
        // 打开 Google News & Weather，从菜单选择 International news 浏览 20 分钟
        Log.i(TAG, "[1] 启动 Google News & Weather");
        DoUHelper.launchPkg(device, "com.google.android.apps.magazines");
        Log.i(TAG, "[2] 等待菜单出现 International");
        DoUHelper.waitForVisible(device, "International");
        Log.i(TAG, "[3] 点击 International 新闻类型");
        DoUHelper.clickText(device, "International");
        Log.i(TAG, "[4] 上下滑动浏览新闻 20 分钟");
        DoUHelper.waitAndSwipe(device, 20);
    }
}
