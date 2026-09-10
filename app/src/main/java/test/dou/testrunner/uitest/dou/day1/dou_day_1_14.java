package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step14 Youtube (20min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step14 Youtube
 * <br>说明: 深度控件操作，Play HD video (for 20 mins) (Use WiFi, after complete this step, please turn off WiFi)
 */
@LargeTest
public class dou_day_1_14 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_14";

    @Test
    public void testDoU() throws Exception {
        // Youtube：播放高清视频 20 分钟（用 WiFi，结束后关闭 WiFi）
        Log.i(TAG, "[1] 启动 Youtube");
        DoUHelper.launchPkg(device, "com.google.android.youtube");
        Log.i(TAG, "[2] 点击播放高清视频");
        DoUHelper.clickText(device, "Play");
        DoUHelper.sleepSec(3);
        Log.i(TAG, "[3] 播放 20 分钟");
        DoUHelper.waitMinutes(20);
        Log.i(TAG, "[4] 关闭 WiFi");
        DoUHelper.setWifiEnabled(device, false);
    }
}
