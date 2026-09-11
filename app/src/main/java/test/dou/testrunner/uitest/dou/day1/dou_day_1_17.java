package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step17 Hill Climb Racing (30min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step17 Hill Climb Racing
 * <br>说明: 深度控件操作，Play game (for 30 mins)
 */
@LargeTest
public class dou_day_1_17 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_17";

    @Test
    public void testDoU() throws Exception {
        // Hill Climb Racing：开始一场游戏并玩 30 分钟
        Log.i(TAG, "[1] 启动 Hill Climb Racing");
        DoUHelper.launchPkg(device, "com.fingersoft.hillclimb", "Hill Climb Racing");
        Log.i(TAG, "[2] 点击 Play 开始游戏");
        Log.i(TAG, "[3] 游戏进行 30 分钟");
        DoUHelper.waitMinutes(30);
    }
}
