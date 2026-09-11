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
 * DoU 循环测试电量报告写入器。
 * <p>每个测试轮次（Cycle，对应原"Day"概念）生成一个 CSV 文件：
 * {@code /sdcard/DOUreport/day_<N>.csv}，记录该轮每个场景的开始/结束电量（% 与 mAh）。
 * <p>每步执行完立即追加并 flush，确保设备因低电量关机时已落盘的数据不丢失。
 */
public final class DouReport {

    private static final String TAG = "DouReport";
    private static final String CSV_HEADER =
            "Step,Title,Before%,Before(mAh),After%,After(mAh),Status,Timestamp\n";

    private DouReport() {
    }

    /** 确保报告目录存在，失败则回退到应用私有目录 */
    private static File ensureDir() {
        File dir = new File(TestConfig.DOU_REPORT_DIR);
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            Log.i(TAG, "创建报告目录: " + dir + " -> " + ok);
        }
        return dir;
    }

    /**
     * 追加一条场景电量记录到指定轮次的报告文件。
     *
     * @param cycle      轮次号（从 1 开始，每跑完一轮 23 步自增）
     * @param step       步骤号（1..23）
     * @param title      步骤标题
     * @param batteryBefore 测试前电量字符串，形如 "91% / 4525mAh"
     * @param batteryAfter  测试后电量字符串，形如 "90% / 4500mAh"
     * @param status     PASS / FAIL
     */
    public static void writeStep(int cycle, int step, String title,
                                 String batteryBefore, String batteryAfter, String status) {
        File dir = ensureDir();
        File file = new File(dir, "day_" + cycle + ".csv");
        boolean isNew = !file.exists();

        int[] before = parseBattery(batteryBefore);
        int[] after = parseBattery(batteryAfter);
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        String line = String.format(Locale.US, "%d,%s,%d,%d,%d,%d,%s,%s%n",
                step,
                escapeCsv(title),
                before[0], before[1],
                after[0], after[1],
                status, ts);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            OutputStreamWriter os = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            if (isNew) {
                os.write(CSV_HEADER);
            }
            os.write(line);
            os.flush();
            // 同步到存储介质：确保设备因低电量立刻关机/断定时已写数据不丢失
            fos.getFD().sync();
            Log.i(TAG, String.format(Locale.US,
                    "报告写入 day_%d step %d: %s -> %s (%s)",
                    cycle, step, batteryBefore, batteryAfter, status));
        } catch (IOException e) {
            Log.e(TAG, "写报告失败: " + file, e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * 解析电量字符串 "91% / 4525mAh" 为 [percent, mAh]。
     * 解析失败或 mAh 缺失（"--mAh"）时对应位置返回 -1。
     */
    private static int[] parseBattery(String s) {
        int percent = -1;
        int mah = -1;
        if (s == null || s.isEmpty()) {
            return new int[]{percent, mah};
        }
        // 形如 "91% / 4525mAh"
        String[] parts = s.split("\\s*/\\s*");
        for (String p : parts) {
            p = p.trim();
            if (p.endsWith("%")) {
                try {
                    percent = Integer.parseInt(p.substring(0, p.length() - 1).trim());
                } catch (NumberFormatException ignore) {
                }
            } else if (p.endsWith("mAh")) {
                String num = p.substring(0, p.length() - 3).trim();
                if (!num.equals("--")) {
                    try {
                        mah = (int) Double.parseDouble(num);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return new int[]{percent, mah};
    }

    /** CSV 字段转义：含逗号/引号/换行时加引号并把内部引号双写 */
    private static String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
