package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step22 Google Map (15min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step22 Google Map
 * <br>说明: 深度控件操作，1. Search location-Gold Gate Bridge / 2.Browse the map by moving the view. Find more info of 2 more additional places  by clicking the POI and opening more info ( for 15 mins)
 */
@LargeTest
public class dou_day_1_22 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_22";

    @Test
    public void testDoU() throws Exception {
        // Google Map：搜索 Golden Gate Bridge，拖动地图后查看 2 个附加地点 POI 信息
        Log.i(TAG, "[1] 启动 Google Map");
        DoUHelper.launchPkg(device, "com.google.android.apps.maps");
        Log.i(TAG, "[2] 等待并点击搜索框");
        DoUHelper.waitForVisible(device, "Search");
        DoUHelper.clickText(device, "Search");
        Log.i(TAG, "[3] 输入地点 Golden Gate Bridge");
        DoUHelper.inputText(device, "Golden Gate Bridge");
        DoUHelper.pressEnter(device);
        DoUHelper.sleepSec(2);
        Log.i(TAG, "[4] 拖动地图查看 2 个额外 POI 信息");
        for (int i = 1; i <= 2; i++) {
            DoUHelper.swipeUp(device);
            DoUHelper.sleepSec(2);
        }
        Log.i(TAG, "[5] 保持地图浏览 15 分钟");
        DoUHelper.waitMinutes(15);
    }
}
