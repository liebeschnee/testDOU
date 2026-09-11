package test.dou.testdrive.ui;

import android.content.Context;
import android.util.Log;

import test.dou.testdrive.cmd.CMDUtils;
import test.dou.testdrive.cmd.TestCommand;
import test.dou.testdrive.config.DoUPlan;
import test.dou.testdrive.config.TestConfig;
import test.dou.testdrive.report.DouReport;
import test.dou.testdrive.report.LogCollector;
import test.dou.testdrive.report.ProgressStore;
import test.dou.testrunner.uitest.util.DoUHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 测试启动器：负责在子线程拼装命令、执行 am instrument，并回收结果。
 */
public class TestLauncher {

    private static final String TAG = "TestLauncher";
    private static volatile TestLauncher sInstance;

    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    /** 请求停止当前（及排队中的）测试执行 */
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    /** 当前线程池是否确有任务在执行（供 UI 判断持久化的“运行中”是否为遗留脏状态） */
    private final AtomicBoolean busy = new AtomicBoolean(false);
    /** 当前正在执行的命令会话，停止时关闭其读取流以中断阻塞式读取 */
    private volatile CMDUtils.CommandSession liveSession;

    /** 步骤结果回调：参数为 (day, step, 结果文案) */
    public interface StepCallback {
        void onStepUpdated(int day, int step, String desc);
    }

    public static TestLauncher getInstance() {
        if (sInstance == null) {
            synchronized (TestLauncher.class) {
                if (sInstance == null) {
                    sInstance = new TestLauncher();
                }
            }
        }
        return sInstance;
    }

    /**
     * 执行一条 uiautomator 用例。
     *
     * @param clsName 用例类全名，如 test.dou.testrunner.uitest.snapshot.PreloadTestCase
     * @param mtdName 用例方法名，如 testEmmcPreload
     */
    public void launch(Context context, String clsName, String mtdName) {
        pool.execute(() -> {
            String command = TestCommand.build(
                    TestConfig.INSTRUMENTATION_TARGET, // uitest 模块 applicationId
                    TestConfig.RUNNER_CLASS,           // instrumentation runner
                    clsName,
                    mtdName);

            Log.i(TAG, "执行命令: " + command);
            CMDUtils.CMD_Result rs = CMDUtils.runCMD(command, true, true);

            // 离线回读：把 instrument 输出写日志，供回归核对
            LogCollector.write(rs.success, rs.error);
            notifyResult(rs);
        });
    }

    /**
     * 请求停止当前正在执行的测试：标记停止，并销毁正在运行的 shell 进程以中断卡住的步骤。
     * 调用线程（UI）安全，可重复调用。
     */
    public void stopRun() {
        stopRequested.set(true);   // 关键：await 轮询会在 ~100ms 内感知并返回，主循环随即退出
        // 尽力立即销毁底层 shell 进程并清理后台残留（失败不影响停止——停止信号已足够）
        CMDUtils.CommandSession s = liveSession;
        if (s != null) {
            s.cancel();
        }
        // 尽力清理后台残留的 am instrument（部分环境有权限可生效，失败不影响停止）
        CMDUtils.killMatching("AndroidJUnitRunner");
    }

    /** 是否已请求停止（供执行循环检查） */
    private boolean isStopRequested() {
        return stopRequested.get();
    }

    /** 线程池当前是否确有任务执行（供 UI 区分真实的运行与遗留的脏“运行中”状态） */
    public boolean isBusy() {
        return busy.get();
    }

    /** 一次新运行开始时清除之前的停止标记 */
    public void resetStop() {
        stopRequested.set(false);
    }

    /**
     * 串行执行某一天的全部步骤，每步开始前标记 RUNNING、结束后标记 PASS/FAIL。
     * 若 steps 非空，则只执行其中指定的步骤。
     *
     * @param day       第几天（1~5）
     * @param steps     要执行的步骤号集合（null 或空 = 全部 1..STEP_COUNT）
     * @param callback  每步状态变化后的回调（可能 ran 于非主线程）
     * @param onFinished 当天全部步骤执行完毕后的回调（工作线程调用，可传 null）
     */
    public void runDay(Context context, int day, List<Integer> steps,
                       StepCallback callback, Runnable onFinished) {
        pool.execute(() -> {
            busy.set(true);
            try {
                for (int step = 1; step <= DoUPlan.STEP_COUNT; step++) {
                    if (steps != null && !steps.isEmpty() && !steps.contains(step)) {
                        continue;
                    }
                    // 用户点击停止：不再执行（当前正被销毁的步骤已自行标记为 FAIL），直接结束本轮
                    if (isStopRequested()) {
                        break;
                    }
                    String cls = DoUPlan.caseClass(day, step);
                    String command = TestCommand.build(
                            TestConfig.INSTRUMENTATION_TARGET,
                            TestConfig.RUNNER_CLASS,
                            cls,
                            "testDoU");

                    ProgressStore.set(context, day, step, ProgressStore.Status.RUNNING);
                    ProgressStore.setLastRound(context, step, day);
                    if (callback != null) {
                        callback.onStepUpdated(day, step, "RUNNING");
                    }
                    logProgress(context, day, step);

                    Log.i(TAG, "第" + day + "天 步骤" + step + " 执行: " + command);
                    // 清空 logcat，确保步骤结束后读到的是本步骤的电量数据
                    clearLogcat();
                    CMDUtils.CMD_Result rs;
                    CMDUtils.CommandSession session = null;
                    try {
                        session = CMDUtils.CommandSession.start(command, stopRequested);
                        liveSession = session;   // 注册当前会话，供“停止”按钮尽快销毁底层进程
                    } catch (Exception e) {
                        Log.e(TAG, "启动命令失败: " + command, e);
                    }
                    if (session == null) {
                        rs = new CMDUtils.CMD_Result();
                    } else {
                        rs = session.await(stopRequested);
                        liveSession = null;      // 会话结束（正常或被中断），注销
                    }
                    LogCollector.write(rs.success, rs.error);

                    boolean pass = isPassed(rs);
                    ProgressStore.set(context, day, step,
                            pass ? ProgressStore.Status.PASS : ProgressStore.Status.FAIL);
                    ProgressStore.setDetail(context, day, step,
                            readBatteryFromLogcat("DOU_BATTERY_BEFORE"),
                            readBatteryFromLogcat("DOU_BATTERY_AFTER"));
                    if (callback != null) {
                        callback.onStepUpdated(day, step, pass ? "PASS" : "FAIL");
                    }
                    logProgress(context, day, step);
                }
            } finally {
                if (onFinished != null) {
                    onFinished.run();
                }
                busy.set(false);
            }
        });
    }

    /**
     * 循环执行所有步骤直到电量耗尽关机（或用户点击停止）。
     * 每跑完一轮（1..STEP_COUNT 全部步骤）视为一天，轮次号自增。
     * 每个场景执行后立即把开始/结束电量写入 /sdcard/DOUreport/day_<N>.csv。
     *
     * @param callback  每步状态变化回调（参数为 cycle/step/desc），可能运行于非主线程
     * @param onFinished 全部结束后回调，可传 null
     */
    public void runContinuous(Context context, StepCallback callback, Runnable onFinished) {
        pool.execute(() -> {
            busy.set(true);
            try {
                int cycle = 1;
                // 不设电量上限：一直循环，直到设备因低电量自动关机或用户停止。
                // 每步报告立即 flush，确保关机前已落盘的数据不丢失。
                while (!isStopRequested()) {
                    Log.i(TAG, "===== 第 " + cycle + " 轮开始 =====");
                    for (int step = 1; step <= DoUPlan.STEP_COUNT; step++) {
                        if (isStopRequested()) break;

                        String cls = DoUPlan.caseClass(cycle, step);
                        String command = TestCommand.build(
                                TestConfig.INSTRUMENTATION_TARGET,
                                TestConfig.RUNNER_CLASS,
                                cls, "testDoU");

                        ProgressStore.set(context, cycle, step, ProgressStore.Status.RUNNING);
                        ProgressStore.setLastRound(context, step, cycle);

                        // 宿主直接读取测前电量（不依赖跨进程 logcat，测前立即可显示/保存），
                        // 若某机型读不到再回退到 logcat 标记。
                        String before = readCurrentBattery(context, "DOU_BATTERY_BEFORE");
                        ProgressStore.setDetail(context, cycle, step, before, "");

                        if (callback != null) {
                            callback.onStepUpdated(cycle, step, "RUNNING");
                        }
                        logProgress(context, cycle, step);

                        Log.i(TAG, "第" + cycle + "轮 步骤" + step + " 执行: " + command);
                        clearLogcat();
                        CMDUtils.CMD_Result rs;
                        CMDUtils.CommandSession session = null;
                        try {
                            session = CMDUtils.CommandSession.start(command, stopRequested);
                            liveSession = session;
                        } catch (Exception e) {
                            Log.e(TAG, "启动命令失败: " + command, e);
                        }
                        if (session == null) {
                            rs = new CMDUtils.CMD_Result();
                        } else {
                            rs = session.await(stopRequested);
                            liveSession = null;
                        }
                        LogCollector.write(rs.success, rs.error);

                        boolean pass = isPassed(rs);
                        String after = readCurrentBattery(context, "DOU_BATTERY_AFTER");

                        ProgressStore.set(context, cycle, step,
                                pass ? ProgressStore.Status.PASS : ProgressStore.Status.FAIL);
                        ProgressStore.setDetail(context, cycle, step, before, after);

                        // 写入电量报告：每步立即落盘，避免关机丢失
                        DouReport.writeStep(cycle, step, DoUPlan.stepTitle(step),
                                before, after, pass ? "PASS" : "FAIL");

                        if (callback != null) {
                            callback.onStepUpdated(cycle, step, pass ? "PASS" : "FAIL");
                        }
                        logProgress(context, cycle, step);
                    }
                    if (isStopRequested()) break;
                    cycle++;
                }
            } finally {
                if (onFinished != null) {
                    onFinished.run();
                }
                busy.set(false);
            }
        });
    }

    /** 清空 logcat 缓冲区（主 APP 为 system uid，可直接执行 logcat） */
    private void clearLogcat() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "logcat -c"});
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            Log.w(TAG, "清空 logcat 失败", e);
        }
    }

    /**
     * 读取当前电量：优先让宿主直接经 BatteryManager 读取（可靠、不依赖跨进程 logcat），
     * 读不到时回退到 logcat 标记（由用例 @Before/@After 通过 System.out 输出）。
     */
    private String readCurrentBattery(Context context, String logcatMarker) {
        if (context != null) {
            String s = DoUHelper.readBattery(context);
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        return logcatMarker == null ? "" : readBatteryFromLogcat(logcatMarker);
    }

    /**
     * 从 logcat 读取电量标记。测试进程通过 System.out.println 输出的
     * DOU_BATTERY_BEFORE / DOU_BATTERY_AFTER 在 logcat 中以 System.out tag 可见。
     * 主 APP 为 system uid，可直接读取 logcat。
     */
    private String readBatteryFromLogcat(String marker) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"sh", "-c", "logcat -d -s System.out:I"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String result = "";
            while ((line = r.readLine()) != null) {
                int idx = line.indexOf(marker + ":");
                if (idx >= 0) {
                    result = line.substring(idx + marker.length() + 1).trim();
                }
            }
            r.close();
            p.destroy();
            return result;
        } catch (Exception e) {
            Log.w(TAG, "读取 logcat 电量失败: " + marker, e);
            return "";
        }
    }

    /** 粗略判断 am instrument 是否成功：包含 OK(1 test) 且不含 FAILED */
    private boolean isPassed(CMDUtils.CMD_Result rs) {
        String out = rs == null ? "" : (rs.success == null ? "" : rs.success);
        if (out.isEmpty()) {
            return false;
        }
        boolean ok = out.contains("OK (1 test") || out.contains("OK (1");
        boolean failed = out.contains("FAILED") || out.contains("INSTRUMENTATION_FAILED");
        return ok && !failed;
    }

    private void logProgress(Context context, int day, int step) {
        ProgressStore.Status s = ProgressStore.get(context, day, step);
        Log.i(TAG, "进度更新 第" + day + "天 步骤" + step + " = " + s);
    }

    /** 结果回显/上报（骨架仅留接口，可按需接 UI/广播） */
    private void notifyResult(CMDUtils.CMD_Result rs) {
        Log.i(TAG, "测试结束: success=[" + rs.success + "] error=[" + rs.error + "]");
    }
}