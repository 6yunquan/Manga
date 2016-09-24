package com.tongming.manga.mvp.view.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tongming.manga.R;
import com.tongming.manga.server.DownloadInfo;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author: Tongming
 * Date: 2016/9/16
 */

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadHolder> implements View.OnClickListener, View.OnLongClickListener {

    private List<DownloadInfo> downloadInfoList;
    private Context mContext;

    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public DownloadAdapter(List<DownloadInfo> downloadInfoList, Context mContext) {
        this.downloadInfoList = downloadInfoList;
        this.mContext = mContext;
    }

    @Override
    public DownloadAdapter.DownloadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = View.inflate(mContext, R.layout.item_download, null);
        inflate.setOnClickListener(this);
        return new DownloadHolder(inflate);
    }

    @Override
    public void onBindViewHolder(DownloadAdapter.DownloadHolder holder, int position) {
        DownloadInfo info = downloadInfoList.get(position);
        holder.itemView.setTag(position);
        holder.tvName.setText(info.getChapter_name());
        holder.tvCurrent.setText((info.getPosition() + "/" + info.getTotal()));
        if (info.getTotal() == 0 || (info.getPosition() == info.getTotal() && info.getPosition() > 0)) {
            holder.progressBar.setVisibility(View.GONE);
        } else {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setMax(info.getTotal());
            holder.progressBar.setProgress(info.getPosition());
        }
        switch (info.getStatus()) {
            case DownloadInfo.DOWNLOAD:
                holder.tvStatus.setText("下载中");
                if (holder.progressBar.getVisibility() == View.GONE) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                }
                holder.ivAction.setVisibility(View.VISIBLE);
                holder.tvWatch.setVisibility(View.GONE);
                holder.ivAction.setImageResource(R.drawable.ic_download_pause);
                break;
            case DownloadInfo.WAIT:
                holder.tvStatus.setText("等待中");
                holder.ivAction.setImageResource(R.drawable.ic_download_waiting);
                break;
            case DownloadInfo.PAUSE:
                holder.tvStatus.setText("已暂停");
                holder.ivAction.setImageResource(R.drawable.ic_download_continue);
                break;
            case DownloadInfo.COMPLETE:
                holder.tvStatus.setText("已完成");
                holder.progressBar.setVisibility(View.GONE);
                holder.ivAction.setVisibility(View.GONE);
                holder.tvWatch.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return downloadInfoList.size();
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, (Integer) v.getTag());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return (onItemLongClickListener != null && onItemLongClickListener.onItemLongClick(v, (Integer) v.getTag()));
    }

    public class DownloadHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_chapter_name)
        TextView tvName;
        @BindView(R.id.tv_current_download)
        TextView tvCurrent;
        @BindView(R.id.tv_status)
        TextView tvStatus;
        @BindView(R.id.progress)
        ProgressBar progressBar;
        @BindView(R.id.iv_action)
        ImageView ivAction;
        @BindView(R.id.tv_watch)
        TextView tvWatch;

        public DownloadHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void updateItem(DownloadInfo info) {
            if (progressBar.getVisibility() == View.GONE) {
                progressBar.setVisibility(View.VISIBLE);
            }
            ivAction.setImageResource(R.drawable.ic_download_pause);
            progressBar.setProgress(info.getPosition());
            progressBar.setMax(info.getTotal());
            tvStatus.setText("下载中");
            tvCurrent.setText(info.getPosition() + "/" + info.getTotal());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }
}
