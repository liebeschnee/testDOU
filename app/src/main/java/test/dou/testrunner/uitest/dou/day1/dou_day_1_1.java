package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step1 - (1min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step1 -
 * <br>说明: 深度控件操作，Unlock device
 */
@LargeTest
public class dou_day_1_1 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_1";

    @Test
    public void testDoU() throws Exception {
        // 息屏 10 秒作为本步骤开始时间点（screenOff 息屏前会记录一次当前电量）
        Log.i(TAG, "[1] 息屏 10 秒（开始时间点）");
        DoUHelper.screenOff(device);
        DoUHelper.sleepSec(10);
        // 随后亮屏 1 分钟，作为本步骤结束测试时间点
        Log.i(TAG, "[2] 唤醒并解锁设备（无密码，上滑解锁）");
        DoUHelper.wakeAndUnlock(device);
        Log.i(TAG, "[3] 保持亮屏 1 分钟（结束测试时间点）");
        DoUHelper.waitMinutes(1);
    }
}
