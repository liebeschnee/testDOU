package test.dou.testdrive;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import test.dou.testdrive.R;
import test.dou.testdrive.config.DoUPlan;
import test.dou.testdrive.report.ProgressStore;
import test.dou.testdrive.ui.DayActivity;
import test.dou.testdrive.ui.TestLauncher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 主页：列出 Day1~5 勾选项 + 整体进度条。
 * 勾选天数后点“开始测试”依次执行；点击某天行进入该天步骤二级菜单。
 */
public class MainActivity extends AppCompatActivity {

    private final Set<Integer> selected = new HashSet<>();
    private DayListAdapter adapter;
    private TextView tvOverall;
    private ProgressBar pbOverall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 测试过程屏幕常亮，避免等待/操作时自动熄屏中断
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        tvOverall = findViewById(R.id.tvOverall);
        pbOverall = findViewById(R.id.pbOverall);

        // 若持久化标记“运行中”但线程池实际无任务，说明是上次中断/重启留下的脏状态，自动复位
        if (ProgressStore.isRunActive(this) && !TestLauncher.getInstance().isBusy()) {
            ProgressStore.setRunActive(this, false);
            clearRunningSteps();
        }

        ListView listDays = findViewById(R.id.listDays);
        adapter = new DayListAdapter(this);
        listDays.setAdapter(adapter);

        findViewById(R.id.btnStartDays).setOnClickListener(v -> startSelected());
        findViewById(R.id.btnStopDays).setOnClickListener(v -> stopAll());
        updateStartButtonState();
        requestStoragePermission();
    }

    /**
     * 停止：发出停止信号，并强制复位持久化的运行状态。
     * 即使旧的执行线程早已因重启/被杀而消失、onFinished 不再触发，
     * 也能把卡死的“测试运行中”恢复成可再次开始。
     */
    private void stopAll() {
        TestLauncher.getInstance().stopRun();

        // 复位整轮运行标记
        ProgressStore.setRunActive(this, false);
        clearRunningSteps();
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
        adapter.notifyDataSetChanged();
        updateOverall();
        updateStartButtonState();
    }

    /** 把仍处于 RUNNING（被中断/残留）的步骤标记为 SKIPPED，避免 isAnyStepRunning 持续为真 */
    private void clearRunningSteps() {
        for (int day = 1; day <= DoUPlan.DAY_COUNT; day++) {
            for (int step = 1; step <= DoUPlan.STEP_COUNT; step++) {
                if (ProgressStore.get(this, day, step) == ProgressStore.Status.RUNNING) {
                    ProgressStore.set(this, day, step, ProgressStore.Status.SKIPPED);
                }
            }
        }
    }

    /** 进入某天的步骤详情（二级菜单） */
    private void openDay(int day) {
        Intent it = new Intent(this, DayActivity.class);
        it.putExtra("day", day);
        startActivity(it);
    }

    /** 请求外部存储读取权限（读 /sdcard/TestDrive/run 的电量记录，Android 6+ 需要运行时授权） */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, 100);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从二级菜单返回时刷新各天进度与整体进度条
        adapter.notifyDataSetChanged();
        updateOverall();
        // 恢复按钮状态：运行中保持不可点（持久化，退出重进 APP 依然生效）
        updateStartButtonState();
    }

    private void startSelected() {
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先勾选至少一天", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Integer> days = new ArrayList<>(selected);
        java.util.Collections.sort(days);

        // 持久化标记整轮测试运行中，运行期间按钮置灰；退出重进 APP 仍保持不可点
        TestLauncher.getInstance().resetStop();
        ProgressStore.setRunActive(this, true);
        updateStartButtonState();

        AtomicInteger remaining = new AtomicInteger(days.size());
        StringBuilder sb = new StringBuilder();
        for (Integer d : days) {
            sb.append("第").append(d).append("天 ");
            TestLauncher.getInstance().runDay(this, d, null, (day, step, desc) ->
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        updateOverall();
                    }), () -> {
                        // 全部勾选天执行完毕后才恢复可点
                        if (remaining.decrementAndGet() == 0) {
                            ProgressStore.setRunActive(MainActivity.this, false);
                            runOnUiThread(MainActivity.this::updateStartButtonState);
                        }
                    });
        }
        Toast.makeText(this, "开始: " + sb, Toast.LENGTH_SHORT).show();
    }

    /** 根据整轮运行状态控制“开始测试”按钮：运行中置灰不可点，结束后恢复可点 */
    private void updateStartButtonState() {
        Button btnStart = findViewById(R.id.btnStartDays);
        Button btnStop = findViewById(R.id.btnStopDays);
        boolean active = ProgressStore.isRunActive(this)
                || ProgressStore.isAnyStepRunning(this, DoUPlan.DAY_COUNT, DoUPlan.STEP_COUNT);
        btnStart.setEnabled(!active);
        btnStop.setEnabled(active);
        btnStart.setText(active ? "测试运行中..." : "开始测试（勾选天数）");
    }

    private void updateOverall() {
        int passed = ProgressStore.passedTotal(this, DoUPlan.DAY_COUNT, DoUPlan.STEP_COUNT);
        int total = DoUPlan.DAY_COUNT * DoUPlan.STEP_COUNT;
        pbOverall.setProgress((int) (passed * 100.0 / total));
        tvOverall.setText("整体进度: " + passed + " / " + total);
    }

    /** 每一天一行的适配器，含勾选框 + 该天通过/总进度 */
    private class DayListAdapter extends BaseAdapter {

        private final Context ctx;

        DayListAdapter(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public int getCount() {
            return DoUPlan.DAY_COUNT;
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
                convertView = View.inflate(ctx, R.layout.item_day, null);
            }
            int day = position + 1;
            CheckBox cb = convertView.findViewById(R.id.cbSelect);
            TextView dayName = convertView.findViewById(R.id.tvDayName);
            TextView dayProgress = convertView.findViewById(R.id.tvDayProgress);

            // 点击整行（非勾选框区域）进入该天步骤详情
            convertView.setOnClickListener(v -> openDay(day));

            dayName.setText("Day " + day);
            Map<ProgressStore.Status, Integer> sum =
                    ProgressStore.daySummary(ctx, day, DoUPlan.STEP_COUNT);
            dayProgress.setText("通过 " + sum.get(ProgressStore.Status.PASS)
                    + " / 失败 " + sum.get(ProgressStore.Status.FAIL) + " / 共 "
                    + DoUPlan.STEP_COUNT);

            cb.setChecked(selected.contains(day));
            cb.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    selected.add(day);
                } else {
                    selected.remove(day);
                }
            });
            return convertView;
        }
    }
}