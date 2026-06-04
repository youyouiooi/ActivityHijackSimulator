package com.activityhijack.simulator.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.activityhijack.simulator.R;
import com.activityhijack.simulator.adapters.AppListAdapter;
import com.activityhijack.simulator.models.AppInfo;

import java.util.List;

/**
 * 应用列表选择对话框
 * 显示已安装应用列表供用户选择
 */
public class AppListDialog extends Dialog {
    private Context context;
    private List<AppInfo> appList;
    private AppListAdapter adapter;
    private OnAppSelectedListener listener;
    private AppInfo selectedApp;

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo);
    }

    public AppListDialog(Context context, List<AppInfo> appList, OnAppSelectedListener listener) {
        super(context);
        this.context = context;
        this.appList = appList;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_app_list);

        // 设置对话框大小
        Window window = getWindow();
        if (window != null) {
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        initViews();
    }

    private void initViews() {
        // 搜索框
        EditText etSearch = findViewById(R.id.et_search);
        RecyclerView rvAppList = findViewById(R.id.rv_app_list);
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnConfirm = findViewById(R.id.btn_confirm);

        // 设置RecyclerView
        adapter = new AppListAdapter(context, appList);
        rvAppList.setLayoutManager(new LinearLayoutManager(context));
        rvAppList.setAdapter(adapter);

        // 设置应用选择监听器
        adapter.setOnAppSelectedListener(new AppListAdapter.OnAppSelectedListener() {
            @Override
            public void onAppSelected(AppInfo appInfo, int position) {
                selectedApp = appInfo;
                btnConfirm.setEnabled(true);
            }
        });

        // 搜索功能
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                selectedApp = null;
                btnConfirm.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> dismiss());

        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            if (selectedApp != null && listener != null) {
                listener.onAppSelected(selectedApp);
            }
            dismiss();
        });
    }
}