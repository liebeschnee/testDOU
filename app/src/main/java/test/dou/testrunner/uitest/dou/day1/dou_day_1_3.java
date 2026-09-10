package test.dou.testrunner.uitest.dou.day1;

import android.util.Log;

import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import test.dou.testrunner.uitest.UiAutoTestCase;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * Day1 Step3 - (5min)
 *
 * <p>来源: Scenario(Phone) 表 Day1 Step3 -
 * <br>说明: 深度控件操作，1.Launch camera and capture multiple images (Capture 10 images) / 2.Rocording the video (for 1 mins)
 */
@LargeTest
public class dou_day_1_3 extends UiAutoTestCase {

    private static final String TAG = "dou_day_1_3";

    @Test
    public void testDoU() throws Exception {
        // 相机：连拍 10 张，再录 1 分钟视频
        Log.i(TAG, "[1] 启动相机");

        // 按已知 camera 包名逐个尝试启动，成功一个即跳出
        // 后续新增 camera 包名在此追加 else if 分支即可
        boolean launched = false;
        if (DoUHelper.tryLaunchPkg(device, "com.xunrui.camera")) {
            launched = true;
        } else if (DoUHelper.tryLaunchPkg(device, "com.google.android.GoogleCamera")) {
            launched = true;
        } else if (DoUHelper.tryLaunchPkg(device, "com.android.camera2")) {
            launched = true;
        }
        // TODO: 后续新增其他 camera 包名，在此追加 else if 分支

        if (!launched) {
            Log.w(TAG, "未找到已知的相机包名，回 Home 继续");
            DoUHelper.goHome(device);
        }
        if (!DoUHelper.clickDesc(device, "Photo") && !DoUHelper.clickDesc(device, "照片")) {
            DoUHelper.clickScreen(device);
        }
        Log.i(TAG, "[2] 连拍 10 张照片");
        for (int i = 1; i <= 10; i++) {
            Log.i(TAG, "拍摄第 " + i + " 张（点击快门 Shutter）");
            if (!DoUHelper.clickDesc(device, "Shutter") && !DoUHelper.clickText(device, "拍照") && !DoUHelper.clickDesc(device, "Take photo")) {
                DoUHelper.clickScreen(device);
            }
            DoUHelper.sleepSec(1);
        }
        Log.i(TAG, "[3] 切换到录像模式");
        if (!DoUHelper.clickDesc(device, "Video") && !DoUHelper.clickDesc(device, "视频")) {
            DoUHelper.clickScreen(device);
        }
        DoUHelper.sleepSec(1);
        Log.i(TAG, "[4] 开始录像并持续 60 秒");
        if (!DoUHelper.clickDesc(device, "Shutter") && !DoUHelper.clickText(device, "录像") && !DoUHelper.clickDesc(device, "Start video")) {
            DoUHelper.clickScreen(device);
        }
        DoUHelper.waitMinutes(1);
        Log.i(TAG, "[5] 点击结束录像");
        if (!DoUHelper.clickDesc(device, "Shutter") && !DoUHelper.clickText(device, "录像") && !DoUHelper.clickDesc(device, "Start video")) {
            DoUHelper.clickScreen(device);
        }
    }
}
