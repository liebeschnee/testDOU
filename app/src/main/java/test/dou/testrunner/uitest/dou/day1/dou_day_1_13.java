package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step13 Booking.com Hotel Deals (20min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step13 Booking.com Hotel Deals
 * <br>说明: 深度控件操作，1.To search around your current location hotels / 2.To browse the detail hotel info & photos at least 5 hotels
 */
@LargeTest
public class dou_day_1_13 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_13";

    @Test
    public void testDoU() throws Exception {
        // Booking.com：搜索当前位置周边酒店，浏览至少 5 家酒店详情与照片
        Log.i(TAG, "[1] 启动 Booking.com");
        DoUHelper.launchPkg(device, "com.booking");
        Log.i(TAG, "[2] 点击搜索入口");
        DoUHelper.clickText(device, "Search");
        DoUHelper.sleepSec(2);
        Log.i(TAG, "[3] 浏览至少 5 家酒店详情");
        for (int i = 1; i <= 5; i++) {
            Log.i(TAG, "浏览第 " + i + " 家酒店");
            DoUHelper.swipeUp(device);
            DoUHelper.sleepSec(2);
        }
        Log.i(TAG, "[4] 持续上下滑动浏览 20 分钟");
        DoUHelper.waitAndSwipe(device, 20);
    }
}
