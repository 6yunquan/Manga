package com.tongming.manga.mvp.view.activity;

import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.tongming.manga.R;
import com.tongming.manga.cusview.SpaceItemDecoration;
import com.tongming.manga.mvp.base.BaseActivity;
import com.tongming.manga.mvp.download.DownloadManager;
import com.tongming.manga.mvp.presenter.DownloadPresenterImp;
import com.tongming.manga.mvp.view.adapter.DownloadAdapter;
import com.tongming.manga.server.DownloadConnection;
import com.tongming.manga.server.DownloadInfo;
import com.tongming.manga.util.CommonUtil;

import java.util.List;

import butterknife.BindView;

/**
 * Author: Tongming
 * Date: 2016/9/7
 */

public class DownloadDetailActivity extends BaseActivity implements IQueryDownloadView {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rv_download)
    RecyclerView rvDownload;
    @BindView(R.id.tv_control_all)
    TextView tvControl;
    @BindView(R.id.tv_directory)
    TextView tvDirectory;
    private DownloadManager manager;
    private DownloadConnection conn;
    private DownloadAdapter adapter;
    private List<DownloadInfo> infoList;
    private DownloadManager.DownloadBinder binder;
    private boolean serviceStarted;
    private boolean isStart;
    private String cid;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_download_detail;
    }

    @Override
    protected void initView() {
        initToolbar(toolbar);
        tvControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controlQueue();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        cid = intent.getStringExtra("cid");
        new DownloadPresenterImp(this).queryDownloadInfo(this, cid);
        serviceStarted = CommonUtil.isServiceStarted(this, DownloadManager.class.getName());
        if (serviceStarted && conn == null) {
            //绑定Service
            conn = new DownloadConnection(this, DownloadConnection.DOWNLOAD_TASK);
            bindService(new Intent(this, DownloadManager.class), conn, BIND_AUTO_CREATE);
        }
    }

    private void controlQueue() {
        //tvControl控制整个队列的开始暂停
        try {
            if (serviceStarted && binder != null) {
                if (isStart) {
                    binder.pauseQueue(cid);
                    isStart = false;
                    tvControl.setText("全部开始");
                } else {
                    binder.resumeQueue(cid);
                    isStart = true;
                    tvControl.setText("全部暂停");
                }
            } else {
                //默认情况下是暂停状态
                Intent intent = new Intent(this, DownloadManager.class);
                intent.putExtra("cid", cid);
                startService(intent);
                serviceStarted = true;
                if (conn == null) {
                    conn = new DownloadConnection(this, DownloadConnection.DOWNLOAD_TASK);
                    bindService(intent, conn, BIND_AUTO_CREATE);
                    isStart = true;
                    tvControl.setText("全部暂停");
                }
            }
        } catch (RemoteException e) {
            Logger.e(e.getMessage());
        }
    }

    @Override
    public void onQueryDownloadInfo(List<DownloadInfo> list) {
        if (infoList == null) {
            infoList = list;
            adapter = new DownloadAdapter(infoList, this);
            rvDownload.setLayoutManager(new LinearLayoutManager(this));
            rvDownload.addItemDecoration(new SpaceItemDecoration(10));
            adapter.setOnItemClickListener(new DownloadAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    //开始,暂停,观看
                    DownloadInfo info = infoList.get(position);
                    int status = info.getStatus();
                    switch (status) {
                        case DownloadInfo.WAIT:
                            break;
                        case DownloadInfo.DOWNLOAD:
                            if (serviceStarted) {
                                try {
                                    Logger.d("暂停" + info.getChapter_name() + "的下载");
                                    binder.pauseTask(info);
                                } catch (RemoteException e) {
                                    Logger.e(e.getMessage());
                                }
                            }
                            break;
                        case DownloadInfo.PAUSE:
                            if (serviceStarted) {
                                try {
                                    //判断是否有队列在下载,有的话点击变为等待
                                    Logger.d("继续" + info.getChapter_name() + "的下载");
                                    binder.resumeTask(info);
                                } catch (RemoteException e) {
                                    Logger.e(e.getMessage());
                                }
                            } else {
                                //启动Manager并绑定
                                Intent intent = new Intent(DownloadDetailActivity.this, DownloadManager.class);
                                intent.putExtra("download", info);
                                startService(intent);
                                serviceStarted = true;
                                if (conn == null) {
                                    conn = new DownloadConnection(DownloadDetailActivity.this, DownloadConnection.DOWNLOAD_TASK);
                                    bindService(intent, conn, BIND_AUTO_CREATE);
                                }
                            }
                            break;
                        case DownloadInfo.COMPLETE:
                            //跳转到PageActivity
                            Intent intent = new Intent(DownloadDetailActivity.this, PageActivity.class);
                            intent.putExtra("url", info.getChapter_url());
                            startActivity(intent);
                            break;
                    }
                }
            });
            adapter.setOnItemLongClickListener(new DownloadAdapter.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(View view, int position) {
                    //长按取消下载

                    return false;
                }
            });
            rvDownload.setAdapter(adapter);
        } else {
            infoList.clear();
            infoList.addAll(list);
            adapter.notifyDataSetChanged();
        }
        if (list.size() != 0) {
            tvDirectory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DownloadDetailActivity.this, ComicDetailActivity.class);
                    intent.putExtra("name", infoList.get(0).getComic_name());
                    intent.putExtra("url", infoList.get(0).getComic_url());
                    startActivity(intent);
                }
            });
        }
        initAction();
    }

    public void setOnTaskListener() {
        binder = conn.getBinder();
        conn.getManager().setOnTaskListener(new DownloadManager.onTaskListener() {
            @Override
            public void onTaskComplete(DownloadInfo info) {
                notifyItemChanged(info);
                Logger.d(info.getChapter_name() + "任务完成");
            }

            @Override
            public void onTaskWait(DownloadInfo info) {
                Logger.d(info.getChapter_name() + "任务等待");
                notifyItemChanged(info);
            }

            @Override
            public void onTaskStart(DownloadInfo info) {
                notifyItemChanged(info);
            }

            @Override
            public void onTaskPause(DownloadInfo info) {
                notifyItemChanged(info);
                Logger.d(info.getChapter_name() + "任务暂停");
            }

            @Override
            public void onTaskResume(DownloadInfo info) {
                notifyItemChanged(info);
                Logger.d(info.getChapter_name() + "任务继续");
            }

            @Override
            public void onTaskStop(DownloadInfo info) {
                notifyItemChanged(info);
                Logger.d(info.getChapter_name() + "任务取消");
            }

            @Override
            public void onTaskRestart(DownloadInfo info) {
                notifyItemChanged(info);
                Logger.d(info.getChapter_name() + "任务重启");
            }

            @Override
            public void onTaskFail(DownloadInfo info) {
                notifyItemChanged(info);
                Logger.d(info.getChapter_name() + "任务失败");
            }
        });
    }

    private void notifyItemChanged(DownloadInfo info) {
        int downloadSize = 0;
        int pauseSize = 0;
        for (int index = 0; index < infoList.size(); index++) {
            DownloadInfo downloadInfo = infoList.get(index);
            if (info.getChapter_url().equals(downloadInfo.getChapter_url())) {
                /*if (info.getStatus() == DownloadInfo.WAIT) {
                    Logger.d(info.getChapter_name() + "进入等待");
                }*/
                downloadInfo.setStatus(info.getStatus());
                downloadInfo.setPosition(info.getPosition());
                downloadInfo.setTotal(info.getTotal());
                if (info.getStatus() == DownloadInfo.DOWNLOAD) {
                    DownloadAdapter.DownloadHolder holder = (DownloadAdapter.DownloadHolder) rvDownload.findViewHolderForAdapterPosition(index);
                    holder.updateItem(downloadInfo);
                } else {
                    adapter.notifyItemChanged(index);
                }
            }
            switch (downloadInfo.getStatus()) {
                case DownloadInfo.WAIT:
                case DownloadInfo.DOWNLOAD:
                    downloadSize++;
                    break;
                case DownloadInfo.PAUSE:
                    pauseSize++;
                    break;
            }
        }
        /*tvControl,队列中如果存在等待或者下载状态的task,则显示'全部暂停'
        如全部完成则不可点击
        如除了已完成的task之外都是pause状态的话,则显示'全部开始'*/
        if (downloadSize > 0) {
            tvControl.setText("全部暂停");
            tvControl.setClickable(true);
            isStart = true;
        } else if (downloadSize == 0 && pauseSize > 0) {
            tvControl.setText("全部开始");
            tvControl.setClickable(true);
            isStart = false;
        } else {
            tvControl.setBackgroundResource(R.drawable.chapter_action_bg);
            tvControl.setTextColor(getResources().getColor(R.color.gray, null));
            tvControl.setClickable(false);
        }
    }

    private void initAction() {
        int downloadSize = 0;
        int pauseSize = 0;
        for (int index = 0; index < infoList.size(); index++) {
            DownloadInfo downloadInfo = infoList.get(index);
            switch (downloadInfo.getStatus()) {
                case DownloadInfo.WAIT:
                case DownloadInfo.DOWNLOAD:
                    downloadSize++;
                    break;
                case DownloadInfo.PAUSE:
                    pauseSize++;
                    break;
            }
        }
        if (downloadSize > 0) {
            tvControl.setText("全部暂停");
            tvControl.setClickable(true);
        } else if (downloadSize == 0 && pauseSize > 0) {
            tvControl.setText("全部开始");
            tvControl.setClickable(true);
        } else {
            tvControl.setBackgroundResource(R.drawable.chapter_action_bg);
            tvControl.setTextColor(getResources().getColor(R.color.gray, null));
            tvControl.setClickable(false);
        }
    }

    @Override
    public void onFail(Throwable throwable) {
        Logger.e(throwable.getMessage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conn != null) {
            conn.getManager().removeTaskListener();
            unbindService(conn);
        }
    }
}
