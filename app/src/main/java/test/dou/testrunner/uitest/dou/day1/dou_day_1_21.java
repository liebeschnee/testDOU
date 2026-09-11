package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step21 Spotify (40min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step21 Spotify
 * <br>说明: 深度控件操作，1.Play music (for 40 mins) / Open spotify, open daily mix, hit play
 */
@LargeTest
public class dou_day_1_21 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_21";

    @Test
    public void testDoU() throws Exception {
        // Spotify：打开 Daily Mix 开始播放 40 分钟
        Log.i(TAG, "[1] 启动 Spotify");
        DoUHelper.launchPkg(device, "com.spotify.music","Spotify");
        DoUHelper.sleepSec(10);
        Log.i(TAG, "[2] 点击 Daily Mix");
        DoUHelper.clickScreen(device);
        DoUHelper.sleepSec(3);
        Log.i(TAG, "[3] 点击 Play 开始播放");
        DoUHelper.clickID(device, "com.spotify.music:id/button_play_and_pause");
        DoUHelper.sleepSec(3);
        Log.i(TAG, "[4] 播放音乐 40 分钟");
        DoUHelper.waitMinutes(40);
    }
}
