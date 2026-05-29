package com.Blabby.Co.UniHub;

import android.app.Application;

import com.Blabby.Co.UniHub.download.DownloadManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DownloadManager.getInstance().init(this);
    }
}