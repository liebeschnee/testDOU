package test.dou.testdrive.report;

import android.os.Environment;
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

    /** 已解析并确认可写的报告目录（首次写入时确定），避免每次重复探测 */
    private static volatile File resolvedDir;

    /**
     * 确保报告目录存在。依次尝试 /sdcard/DOUreport 与外部存储根下的 DOUreport，
     * 取第一个可创建/可写的目录；全部失败时回退到配置目录并返回（写入时会再报错）。
     */
    private static File ensureDir() {
        File cached = resolvedDir;
        if (cached != null) {
            return cached;
        }
        synchronized (DouReport.class) {
            if (resolvedDir != null) {
                return resolvedDir;
            }
            File fallback = new File(TestConfig.DOU_REPORT_DIR);
            String[] candidates = {
                    TestConfig.DOU_REPORT_DIR,
                    new File(Environment.getExternalStorageDirectory(), "DOUreport").getAbsolutePath()
            };
            for (String c : candidates) {
                try {
                    File d = new File(c);
                    if (d.exists() || d.mkdirs()) {
                        if (d.isDirectory() && (d.canWrite() || d.setWritable(true))) {
                            resolvedDir = d;
                            Log.i(TAG, "报告目录已就绪: " + d.getAbsolutePath());
                            return d;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "尝试报告目录失败: " + c, e);
                }
            }
            Log.e(TAG, "所有候选报告目录均不可写，最后回退: " + fallback.getAbsolutePath());
            return fallback;
        }
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
                os.write('\uFEFF');   // UTF-8 BOM，保证 Excel 正确识别 UTF-8，避免中文标题乱码
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
