package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step6 Standby(83) (83min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step6 Standby(83)
 * <br>说明: 深度控件操作，come back to homepage, and display off / Suspend 83mins
 */
@LargeTest
public class dou_day_1_6 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_6";

    @Test
    public void testDoU() throws Exception {
        // 回到主界面并息屏，进入待机
        Log.i(TAG, "[1] 回到主界面");
        DoUHelper.goHome(device);
        Log.i(TAG, "[2] 息屏待机");
        DoUHelper.screenOff(device);
        Log.i(TAG, "[3] 待机 83 分钟");
        DoUHelper.waitMinutes(83);
    }
}
