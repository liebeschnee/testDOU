package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step8 XE Currency (5min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step8 XE Currency
 * <br>说明: 深度控件操作，1. Add Currency-TWD into the list / 2. Input 5 different USD price in the USD column (ex. 1000 USD, 600 USD, 500 USD, 100 USD, 50 USD)
 */
@LargeTest
public class dou_day_1_8 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_8";

    @Test
    public void testDoU() throws Exception {
        // XE Currency：新增 TWD 币种，录入 5 个不同 USD 价格
        Log.i(TAG, "[1] 启动 XE Currency");
        DoUHelper.launchPkg(device, "com.xe.app");
        Log.i(TAG, "[2] 点击添加币种入口");
        DoUHelper.clickText(device, "Add currency");
        Log.i(TAG, "[3] 输入并确认币种 TWD");
        DoUHelper.inputText(device, "TWD");
        DoUHelper.pressEnter(device);
        DoUHelper.sleepSec(2);
        Log.i(TAG, "[4] 录入 5 个 USD 价格: 1000/600/500/100/50");
        int[] prices = {1000, 600, 500, 100, 50};
        for (int price : prices) {
            DoUHelper.inputText(device, String.valueOf(price));
            DoUHelper.pressEnter(device);
        }
        Log.i(TAG, "[5] 保持应用前台 5 分钟");
        DoUHelper.waitMinutes(5);
    }
}
