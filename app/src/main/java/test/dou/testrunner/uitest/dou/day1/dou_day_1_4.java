package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step4 Facebook (40min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step4 Facebook
 * <br>说明: 深度控件操作，1.Upload 1 image & 1 video file to social networks  / 2.To slide up and down(browse) the facebook (for 35 mins)
 */
@LargeTest
public class dou_day_1_4 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_4";

    @Test
    public void testDoU() throws Exception {
        // Facebook：上传 1 图 + 1 视频，然后上下滑动浏览 40 分钟
        Log.i(TAG, "[1] 启动 Facebook");
        DoUHelper.launchPkg(device, "com.facebook.katana");
        Log.i(TAG, "[2] 点击 Photo 打开上传入口");
        DoUHelper.clickText(device, "Photo");
        DoUHelper.sleepSec(2);
        Log.i(TAG, "[3] 等待上传完成");
        DoUHelper.sleepSec(5);
        Log.i(TAG, "[4] 上下滑动浏览信息流 40 分钟");
        DoUHelper.waitAndSwipe(device, 40);
    }
}
