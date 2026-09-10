package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step10 WhatsApp (30min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step10 WhatsApp
 * <br>说明: 深度控件操作，1.Send & receive messages (for 10 mins) / 2.Wait 5 mins / 3.Send & receive messages (for 10 mins) / 4.Wait 5 mins
 */
@LargeTest
public class dou_day_1_10 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_10";

    @Test
    public void testDoU() throws Exception {
        // WhatsApp：收发消息 10 分钟-等 5 分钟，再收发 10 分钟-再等 5 分钟
        Log.i(TAG, "[1] 启动 WhatsApp");
        DoUHelper.launchPkg(device, "com.whatsapp");
        Log.i(TAG, "[2] 两轮收发消息（各 10 分钟）与等待（各 5 分钟）");
        for (int round = 1; round <= 2; round++) {
            long end = System.currentTimeMillis() + 10 * 60L * 1000L;
            while (System.currentTimeMillis() < end) {
                Log.i(TAG, "第 " + round + " 轮：点击消息输入框并发送");
                DoUHelper.clickText(device, "Message");
                DoUHelper.sleepSec(3);
                DoUHelper.pressEnter(device);
                DoUHelper.sleepSec(3);
            }
            Log.i(TAG, "第 " + round + " 轮结束，等待 5 分钟");
            DoUHelper.waitMinutes(5);
        }
    }
}
