package com.activityhijack.simulator.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.activityhijack.simulator.R;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Activity监测服务
 * 使用UsageStatsManager监测目标应用的启动
 */
public class ActivityMonitorService extends Service {
    private static final String TAG = "ActivityMonitorService";
    private static final String CHANNEL_ID = "ActivityMonitorChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long CHECK_INTERVAL = 500; // 检查间隔（毫秒）

    private String targetPackageName;
    private Handler handler;
    private Runnable checkRunnable;
    private boolean isMonitoring = false;
    private String lastForegroundApp = "";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetPackageName = intent.getStringExtra("target_package");
            if (targetPackageName != null) {
                startMonitoring();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Activity监测服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于监测目标应用启动的前台服务");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity劫持模拟器")
            .setContentText("正在监测应用: " + targetPackageName)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    /**
     * 开始监测
     */
    private void startMonitoring() {
        if (isMonitoring) return;

        isMonitoring = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    checkForegroundApp();
                    handler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        };

        handler.post(checkRunnable);
        Log.d(TAG, "开始监测目标应用: " + targetPackageName);
    }

    /**
     * 停止监测
     */
    private void stopMonitoring() {
        isMonitoring = false;
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        Log.d(TAG, "停止监测");
    }

    /**
     * 检查当前前台应用
     */
    private void checkForegroundApp() {
        String currentForegroundApp = getForegroundAppPackageName();

        if (currentForegroundApp != null && !currentForegroundApp.equals(lastForegroundApp)) {
            lastForegroundApp = currentForegroundApp;

            // 检查是否是目标应用
            if (currentForegroundApp.equals(targetPackageName)) {
                Log.d(TAG, "检测到目标应用启动: " + targetPackageName);
                onTargetAppDetected();
            }
        }
    }

    /**
     * 获取当前前台应用的包名
     * @return 前台应用包名
     */
    private String getForegroundAppPackageName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usageStatsManager != null) {
                long endTime = System.currentTimeMillis();
                long beginTime = endTime - 1000 * 60; // 查询最近1分钟

                UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);
                if (usageEvents != null) {
                    String foregroundApp = null;
                    while (usageEvents.hasNextEvent()) {
                        UsageEvents.Event event = new UsageEvents.Event();
                        usageEvents.getNextEvent(event);
                        if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            foregroundApp = event.getPackageName();
                        }
                    }
                    return foregroundApp;
                }
            }
        }
        return null;
    }

    /**
     * 检测到目标应用启动时的回调
     */
    private void onTargetAppDetected() {
        // 发送广播通知主界面
        Intent broadcastIntent = new Intent("com.activityhijack.TARGET_APP_DETECTED");
        broadcastIntent.putExtra("target_package", targetPackageName);
        sendBroadcast(broadcastIntent);

        // 启动覆盖界面服务
        Intent overlayIntent = new Intent(this, OverlayService.class);
        overlayIntent.putExtra("target_package", targetPackageName);
        startService(overlayIntent);

        Log.d(TAG, "已触发覆盖界面显示");
    }

    /**
     * 检查是否有使用统计权限
     * @param context 上下文
     * @return 是否有权限
     */
    public static boolean hasUsageStatsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usageStatsManager != null) {
                long endTime = System.currentTimeMillis();
                long beginTime = endTime - 1000 * 60;
                List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);
                return stats != null && !stats.isEmpty();
            }
        }
        return false;
    }
}