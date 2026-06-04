package com.activityhijack.simulator.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.activityhijack.simulator.models.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 应用管理器
 * 负责获取设备上已安装的应用列表
 */
public class AppManager {
    private Context context;
    private PackageManager packageManager;

    public AppManager(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    /**
     * 获取所有已安装的应用列表
     * @return 应用信息列表
     */
    public List<AppInfo> getInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : packages) {
            // 过滤掉系统应用（可选）
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                String packageName = appInfo.packageName;
                Drawable icon = packageManager.getApplicationIcon(appInfo);

                AppInfo app = new AppInfo(appName, packageName, icon);
                appList.add(app);
            }
        }

        // 按应用名称排序
        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.getAppName().compareToIgnoreCase(app2.getAppName());
            }
        });

        return appList;
    }

    /**
     * 获取所有应用（包括系统应用）
     * @return 应用信息列表
     */
    public List<AppInfo> getAllApps() {
        List<AppInfo> appList = new ArrayList<>();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : packages) {
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            String packageName = appInfo.packageName;
            Drawable icon = packageManager.getApplicationIcon(appInfo);

            AppInfo app = new AppInfo(appName, packageName, icon);
            appList.add(app);
        }

        // 按应用名称排序
        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.getAppName().compareToIgnoreCase(app2.getAppName());
            }
        });

        return appList;
    }

    /**
     * 根据包名获取应用信息
     * @param packageName 包名
     * @return 应用信息，如果未找到则返回null
     */
    public AppInfo getAppByPackageName(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            Drawable icon = packageManager.getApplicationIcon(appInfo);
            return new AppInfo(appName, packageName, icon);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查应用是否已安装
     * @param packageName 包名
     * @return 是否已安装
     */
    public boolean isAppInstalled(String packageName) {
        try {
            packageManager.getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}