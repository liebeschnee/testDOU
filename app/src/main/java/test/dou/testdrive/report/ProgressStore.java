package test.dou.testdrive.report;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * DoU 每步进度/结果存储（SharedPreferences 持久化）。
 * Key: "step_" + day + "_" + step，Value 为 {@link Status} 名。
 */
public final class ProgressStore {

    public enum Status {
        PENDING,   // 待测
        RUNNING,   // 运行中
        PASS,      // 通过
        FAIL,      // 失败
        SKIPPED    // 跳过
    }

    private static final String PREFS = "dou_progress";
    private static final String KEY_PREFIX = "step_";
    private static final String KEY_PREFIX_LAST_ROUND = "last_round_";
    private static final String KEY_PREFIX_DETAIL = "detail_";
    private static final String KEY_RUN_ACTIVE = "run_active";

    private ProgressStore() {
    }

    private static SharedPreferences sp(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 标记/清除整轮测试运行中状态（持久化，重进 APP 依然生效） */
    public static void setRunActive(Context ctx, boolean active) {
        sp(ctx).edit().putBoolean(KEY_RUN_ACTIVE, active).apply();
    }

    /** 是否整轮测试运行中 */
    public static boolean isRunActive(Context ctx) {
        return sp(ctx).getBoolean(KEY_RUN_ACTIVE, false);
    }

    /** 任意一天任一步处于 RUNNING 则视为测试仍在进行 */
    public static boolean isAnyStepRunning(Context ctx, int dayCount, int stepCount) {
        for (int day = 1; day <= dayCount; day++) {
            for (int step = 1; step <= stepCount; step++) {
                if (get(ctx, day, step) == Status.RUNNING) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 记录某天某步的状态 */
    public static void set(Context ctx, int day, int step, Status status) {
        sp(ctx).edit().putString(key(day, step), status.name()).apply();
    }

    /** 记录某场景最近一次执行所在的轮次（用于主页展示“第几轮”） */
    public static void setLastRound(Context ctx, int step, int cycle) {
        sp(ctx).edit().putInt(KEY_PREFIX_LAST_ROUND + step, cycle).apply();
    }

    /** 读取某场景最近一次执行的轮次，从未执行返回 0 */
    public static int getLastRound(Context ctx, int step) {
        return sp(ctx).getInt(KEY_PREFIX_LAST_ROUND + step, 0);
    }

    /** 读取某步状态，无记录视为 PENDING */
    public static Status get(Context ctx, int day, int step) {
        String v = sp(ctx).getString(key(day, step), Status.PENDING.name());
        try {
            return Status.valueOf(v);
        } catch (IllegalArgumentException e) {
            return Status.PENDING;
        }
    }

    /** 汇总某天各状态计数 */
    public static Map<Status, Integer> daySummary(Context ctx, int day, int stepCount) {
        Map<Status, Integer> summary = new HashMap<>();
        for (Status s : Status.values()) {
            summary.put(s, 0);
        }
        for (int step = 1; step <= stepCount; step++) {
            Status s = get(ctx, day, step);
            summary.put(s, summary.get(s) + 1);
        }
        return summary;
    }

    /** 全部已通过的步骤数（用于整体进度条） */
    public static int passedTotal(Context ctx, int dayCount, int stepCount) {
        int total = 0;
        for (int day = 1; day <= dayCount; day++) {
            for (int step = 1; step <= stepCount; step++) {
                if (get(ctx, day, step) == Status.PASS) {
                    total++;
                }
            }
        }
        return total;
    }

    private static String key(int day, int step) {
        return KEY_PREFIX + day + "_" + step;
    }

    /** 存储某步的测试前后电量记录（batteryBefore/batteryAfter 形如 "66%,3290.00"） */
    public static void setDetail(Context ctx, int day, int step,
                                 String batteryBefore, String batteryAfter) {
        sp(ctx).edit()
                .putString(detailKey(day, step) + "_before", batteryBefore == null ? "" : batteryBefore)
                .putString(detailKey(day, step) + "_after", batteryAfter == null ? "" : batteryAfter)
                .apply();
    }

    /** 读取某步电量记录，无则返回空串 */
    public static String getBatteryBefore(Context ctx, int day, int step) {
        return sp(ctx).getString(detailKey(day, step) + "_before", "");
    }

    /** 读取某步电量记录，无则返回空串 */
    public static String getBatteryAfter(Context ctx, int day, int step) {
        return sp(ctx).getString(detailKey(day, step) + "_after", "");
    }

    private static String detailKey(int day, int step) {
        return KEY_PREFIX_DETAIL + day + "_" + step;
    }
}