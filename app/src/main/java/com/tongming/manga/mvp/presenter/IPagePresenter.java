package com.tongming.manga.mvp.presenter;

import android.content.Context;

import java.util.List;

/**
 * Created by Tongming on 2016/8/11.
 */
public interface IPagePresenter {
    void getPage(String chapterUrl);

    void cacheImg(Context mContext, List<String> imgList, boolean isLast);
}
