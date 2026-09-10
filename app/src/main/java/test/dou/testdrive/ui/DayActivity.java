package test.dou.testdrive.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import test.dou.testdrive.R;
import test.dou.testdrive.config.DoUPlan;
import test.dou.testdrive.report.ProgressStore;

import java.util.Collections;

/**
 * 二级菜单：展示某一天的全部步骤状态及每步测试前后电量记录。
 * 点击某一步骤行可单独在真机上运行该步。
 */
public class DayActivity extends AppCompatActivity {

    private static final String EXTRA_DAY = "day";
    private static final String TAG = "DayActivity";

    private int day;
    private StepAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 测试过程屏幕常亮，避免等待/操作时自动熄屏而点不到步骤
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_day);

        day = getIntent().getIntExtra(EXTRA_DAY, 1);
        ((TextView) findViewById(R.id.tvDayTitle)).setText("Day " + day);

        ListView list = findViewById(R.id.listSteps);
        adapter = new StepAdapter(this);
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /** 在真机上单独运行某一步骤（仅该步，不影响其他步骤状态） */
    private void runSingleStep(int step) {
        if (ProgressStore.isRunActive(this) || ProgressStore.isAnyStepRunning(this, DoUPlan.DAY_COUNT, DoUPlan.STEP_COUNT)) {
            return;
        }
        TestLauncher.getInstance().resetStop();
        ProgressStore.setRunActive(this, true);
        ProgressStore.set(this, day, step, ProgressStore.Status.RUNNING);
        adapter.notifyDataSetChanged();

        Log.i(TAG, "单独运行 第" + day + "天 步骤" + step);
        TestLauncher.getInstance().runDay(this, day, Collections.singletonList(step),
                (d, s, desc) -> runOnUiThread(adapter::notifyDataSetChanged), () -> {
                    ProgressStore.setRunActive(DayActivity.this, false);
                    runOnUiThread(adapter::notifyDataSetChanged);
                });
    }

    /** 列表项：CheckBox 显示每步状态勾选，标题为步骤名 */
    private class StepAdapter extends BaseAdapter {

        private final Context ctx;

        StepAdapter(Context ctx) {
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

            ProgressStore.Status s = ProgressStore.get(ctx, day, step);
            cb.setText(DoUPlan.stepTitle(step));
            cb.setChecked(s == ProgressStore.Status.PASS || s == ProgressStore.Status.RUNNING);
            cb.setEnabled(false);
            title.setText(statusText(s));
            detail.setText(detailText(ctx, day, step));

            // 点击该步骤行弹出确认对话框，确认后才在真机上运行
            convertView.setOnClickListener(v -> confirmRun(step, s));
            return convertView;
        }

        /** 弹出是否运行该步骤的确认框 */
        private void confirmRun(int step, ProgressStore.Status s) {
            new androidx.appcompat.app.AlertDialog.Builder(DayActivity.this)
                    .setTitle("确认运行")
                    .setMessage("确定在真机上单独运行该步骤？\nDay " + day + " Step " + step
                            + "\n" + DoUPlan.stepTitle(step)
                            + "\n当前状态: " + statusText(s))
                    .setPositiveButton("开始运行", (d, w) -> runSingleStep(step))
                    .setNegativeButton("取消", null)
                    .show();
        }

        /** 展示该步测试前后电量记录，如 "测试前 66% / 3290mAh → 测试后 65% / 3280mAh" */
        private String detailText(Context ctx, int day, int step) {
            String before = ProgressStore.getBatteryBefore(ctx, day, step);
            String after = ProgressStore.getBatteryAfter(ctx, day, step);
            if (before.isEmpty() && after.isEmpty()) {
                return "";
            }
            return "测试前 " + before + " → 测试后 " + after;
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