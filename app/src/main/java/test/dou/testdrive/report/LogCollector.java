package test.dou.testdrive.report;

import android.util.Log;

import test.dou.testdrive.config.TestConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 测试结果落盘收集器。
 * 将 am instrument 输出写入 /sdcard/TestDrive/log，供离线回归核对。
 */
public final class LogCollector {

    private static final String TAG = "LogCollector";

    private LogCollector() {
    }

    /** 追加一行日志到当日结果文件 */
    public static void write(String success, String error) {
        String time = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US)
                .format(new Date());
        StringBuilder sb = new StringBuilder()
                .append('[').append(time).append("]\n")
                .append("SUCCESS=").append(success == null ? "" : success).append('\n')
                .append("ERROR=").append(error == null ? "" : error).append('\n')
                .append("----\n");
        File dir = new File(TestConfig.LOG_DIR);
        if (!dir.exists()) {
            // 系统签名下可创建 /sdcard 目录；否则使用应用私有目录
            if (dir.mkdirs() && LOG_DEBUG) {
                Log.i(TAG, "创建日志目录: " + dir);
            }
        }
        File file = new File(dir, "result.txt");
        try (OutputStreamWriter os = new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            os.write(sb.toString());
        } catch (IOException e) {
            Log.e(TAG, "写日志失败", e);
        }
    }

    private static final boolean LOG_DEBUG = true;
}