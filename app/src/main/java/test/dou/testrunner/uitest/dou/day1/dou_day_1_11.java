package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step11 Facebook messenger (10min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step11 Facebook messenger
 * <br>说明: 深度控件操作，To make a voice call (for 10 mins)
 */
@LargeTest
public class dou_day_1_11 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_11";

    @Test
    public void testDoU() throws Exception {
        // Facebook Messenger：发起语音通话 10 分钟
        Log.i(TAG, "[1] 启动 Facebook Messenger");
        DoUHelper.launchPkg(device, "com.whatsapp", "WhatsApp");
        DoUHelper.sleepSec(10);
        Log.i(TAG, "[2] 点击语音通话入口");
        DoUHelper.clickID(device,"com.whatsapp:id/camera_btn");
        Log.i(TAG, "[3] 语音通话 10 分钟");
        DoUHelper.waitMinutes(10);
        Log.i(TAG, "[4] 通话结束回主界面");
        DoUHelper.goHome(device);
    }
}
