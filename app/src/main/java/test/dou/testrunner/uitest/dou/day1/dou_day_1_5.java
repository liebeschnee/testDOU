package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step5 Gmail (2min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step5 Gmail
 * <br>说明: 深度控件操作，1. Read email
 */
@LargeTest
public class dou_day_1_5 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_5";

    @Test
    public void testDoU() throws Exception {
        // Gmail：读取邮件
        Log.i(TAG, "[1] 启动 Gmail");
        DoUHelper.launchPkg(device, "com.google.android.gm");
        Log.i(TAG, "[2] 打开第一封邮件");
        UiObject first = device.findObject(new UiSelector().clickable(true).instance(0));
        if (first != null && first.exists()) {
            first.click();
            device.waitForIdle(800);
        }
        Log.i(TAG, "[3] 阅读邮件 2 分钟");
        DoUHelper.waitMinutes(2);
    }
}
