package com.activityhijack.simulator.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.activityhijack.simulator.R;
import com.activityhijack.simulator.models.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用列表适配器
 * 用于在RecyclerView中显示已安装的应用列表
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
    private Context context;
    private List<AppInfo> appList;
    private List<AppInfo> filteredList;
    private int selectedPosition = -1;
    private OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo, int position);
    }

    public AppListAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        this.filteredList = new ArrayList<>(appList);
    }

    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo appInfo = filteredList.get(position);

        holder.tvAppName.setText(appInfo.getAppName());
        holder.tvPackageName.setText(appInfo.getPackageName());
        holder.ivAppIcon.setImageDrawable(appInfo.getAppIcon());

        // 显示选中状态
        if (position == selectedPosition) {
            holder.ivSelected.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(0xFFE3F2FD); // 浅蓝色背景
        } else {
            holder.ivSelected.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // 白色背景
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                // 更新选中状态
                int previousSelected = selectedPosition;
                selectedPosition = adapterPosition;

                // 通知更新
                if (previousSelected >= 0) {
                    notifyItemChanged(previousSelected);
                }
                notifyItemChanged(selectedPosition);

                // 回调
                if (listener != null) {
                    listener.onAppSelected(filteredList.get(adapterPosition), adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * 获取当前选中的应用
     * @return 选中的应用信息，如果未选中则返回null
     */
    public AppInfo getSelectedApp() {
        if (selectedPosition >= 0 && selectedPosition < filteredList.size()) {
            return filteredList.get(selectedPosition);
        }
        return null;
    }

    /**
     * 过滤应用列表
     * @param query 搜索关键词
     */
    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(appList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AppInfo app : appList) {
                if (app.getAppName().toLowerCase().contains(lowerQuery) ||
                    app.getPackageName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(app);
                }
            }
        }
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    /**
     * 更新应用列表
     * @param newList 新的应用列表
     */
    public void updateList(List<AppInfo> newList) {
        this.appList = newList;
        this.filteredList = new ArrayList<>(newList);
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        TextView tvPackageName;
        ImageView ivSelected;

        ViewHolder(View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            ivSelected = itemView.findViewById(R.id.iv_selected);
        }
    }
}