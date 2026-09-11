package test.dou.testdrive;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import test.dou.testdrive.config.DoUPlan;
import test.dou.testdrive.report.ProgressStore;
import test.dou.testdrive.ui.TestLauncher;
import test.dou.testrunner.uitest.util.DoUHelper;

/**
 * 主页：只展示 23 个场景列表，每行显示该场景最近一次执行所在轮次
 * 及测试前/测试后的电量（% 与 mAh）。点“开始循环测试”后从第 1 轮起
 * 一直循环到电量耗尽自动关机（或用户点停止），不再需要二级界面。
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /** 当前正在执行的轮次号（从 1 开始） */
    private int currentCycle = 1;
    private StepListAdapter adapter;
    private TextView tvCycle;
    private TextView tvBattery;
    private TextView tvOverall;
    private ProgressBar pbOverall;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    /** 定时刷新当前电量显示 */
    private final Runnable batteryRefresh = new Runnable() {
        @Override
        public void run() {
            tvBattery.setText("当前电量: " + DoUHelper.readBattery(getApplicationContext()));
            uiHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        tvCycle = findViewById(R.id.tvCycle);
        tvBattery = findViewById(R.id.tvBattery);
        tvOverall = findViewById(R.id.tvOverall);
        pbOverall = findViewById(R.id.pbOverall);

        // 若持久化标记"运行中"但线程池实际无任务，说明是上次中断/重启留下的脏状态，自动复位
        if (ProgressStore.isRunActive(this) && !TestLauncher.getInstance().isBusy()) {
            ProgressStore.setRunActive(this, false);
            clearRunningSteps();
        }

        ListView listSteps = findViewById(R.id.listSteps);
        adapter = new StepListAdapter(this);
        listSteps.setAdapter(adapter);

        findViewById(R.id.btnStartDays).setOnClickListener(v -> startContinuous());
        findViewById(R.id.btnStopDays).setOnClickListener(v -> stopAll());
        updateStartButtonState();
        requestStoragePermission();

        // 启动电量定时刷新
        uiHandler.post(batteryRefresh);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        updateCycleView();
        updateOverall();
        updateStartButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(batteryRefresh);
    }

    /** 开始循环测试：一直跑到电量耗尽关机或用户停止 */
    private void startContinuous() {
        TestLauncher.getInstance().resetStop();
        ProgressStore.setRunActive(this, true);
        updateStartButtonState();

        TestLauncher.getInstance().runContinuous(this,
                (cycle, step, desc) -> runOnUiThread(() -> {
                    // 回调中 cycle 可能比 currentCycle 大（进入新一轮），同步更新
                    if (cycle > currentCycle) {
                        currentCycle = cycle;
                    }
                    adapter.notifyDataSetChanged();
                    updateCycleView();
                    updateOverall();
                }),
                () -> {
                    ProgressStore.setRunActive(MainActivity.this, false);
                    runOnUiThread(MainActivity.this::updateStartButtonState);
                });

        Toast.makeText(this, "开始循环测试，直至电量耗尽关机", Toast.LENGTH_SHORT).show();
    }

    /** 停止：发出停止信号，并强制复位持久化的运行状态。 */
    private void stopAll() {
        TestLauncher.getInstance().stopRun();
        ProgressStore.setRunActive(this, false);
        clearRunningSteps();
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
        adapter.notifyDataSetChanged();
        updateOverall();
        updateStartButtonState();
    }

    /** 把仍处于 RUNNING（被中断/残留）的步骤标记为 SKIPPED */
    private void clearRunningSteps() {
        // 扫描当前轮次及之前的所有步骤，避免遗留 RUNNING 状态
        for (int day = 1; day <= currentCycle; day++) {
            for (int step = 1; step <= DoUPlan.STEP_COUNT; step++) {
                if (ProgressStore.get(this, day, step) == ProgressStore.Status.RUNNING) {
                    ProgressStore.set(this, day, step, ProgressStore.Status.SKIPPED);
                }
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, 100);
            }
        }
    }

    private void updateCycleView() {
        tvCycle.setText("当前轮次: 第 " + currentCycle + " 轮（Day " + currentCycle + "）");
    }

    private void updateStartButtonState() {
        Button btnStart = findViewById(R.id.btnStartDays);
        Button btnStop = findViewById(R.id.btnStopDays);
        boolean active = ProgressStore.isRunActive(this)
                || ProgressStore.isAnyStepRunning(this, currentCycle, DoUPlan.STEP_COUNT);
        btnStart.setEnabled(!active);
        btnStop.setEnabled(active);
        btnStart.setText(active ? "测试运行中..." : "开始循环测试");
    }

    private void updateOverall() {
        int passed = 0;
        for (int step = 1; step <= DoUPlan.STEP_COUNT; step++) {
            if (ProgressStore.get(this, currentCycle, step) == ProgressStore.Status.PASS) {
                passed++;
            }
        }
        int total = DoUPlan.STEP_COUNT;
        pbOverall.setProgress((int) (passed * 100.0 / total));
        tvOverall.setText("本轮进度: " + passed + " / " + total);
    }

    /** 23 个场景的列表适配器：每行展示该场景最近一次执行的轮次及测试前后电量 */
    private class StepListAdapter extends BaseAdapter {

        private final Context ctx;

        StepListAdapter(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public int getCount() {
            return DoUPlan.STEP_COUNT;
        }

        @Override
        public Object getItem(int position) {
            return position + 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(ctx, R.layout.item_step, null);
            }
            int step = position + 1;
            CheckBox cb = convertView.findViewById(R.id.cbStatus);
            TextView title = convertView.findViewById(R.id.tvStepTitle);
            TextView detail = convertView.findViewById(R.id.tvStepDetail);

            cb.setText(step + ". " + DoUPlan.stepTitle(step));
            cb.setEnabled(false);

            // 只展示该场景最近一次执行的轮次及测试前后电量，无二级界面
            int round = ProgressStore.getLastRound(ctx, step);
            if (round <= 0) {
                cb.setChecked(false);
                title.setText("尚未测试");
                detail.setText("");
            } else {
                ProgressStore.Status s = ProgressStore.get(ctx, round, step);
                cb.setChecked(s == ProgressStore.Status.PASS);
                title.setText(statusText(s));
                detail.setText("第" + round + "轮  前 " + valor(ProgressStore.getBatteryBefore(ctx, round, step))
                        + " → 后 " + valor(ProgressStore.getBatteryAfter(ctx, round, step)));
            }
            return convertView;
        }

        /** 电量串 "91% / 4525mAh"，为空回显 "--" */
        private String valor(String s) {
            return s == null || s.isEmpty() ? "--" : s;
        }

        private String statusText(ProgressStore.Status s) {
            switch (s) {
                case RUNNING: return "运行中…";
                case PASS:    return "通过";
                case FAIL:    return "失败";
                case SKIPPED: return "跳过";
                default:      return "待测";
            }
        }
    }
}
