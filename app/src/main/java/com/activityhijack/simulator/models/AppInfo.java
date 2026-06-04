package com.activityhijack.simulator.models;

import android.graphics.drawable.Drawable;

/**
 * 应用信息模型类
 * 存储已安装应用的基本信息
 */
public class AppInfo {
    private String appName;
    private String packageName;
    private Drawable appIcon;
    private boolean isSelected;

    public AppInfo(String appName, String packageName, Drawable appIcon) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.isSelected = false;
    }

    // Getters and Setters
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public String toString() {
        return appName + " (" + packageName + ")";
    }
}