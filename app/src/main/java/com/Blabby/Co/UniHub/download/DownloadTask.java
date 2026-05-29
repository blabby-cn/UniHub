package com.Blabby.Co.UniHub.download;

public class DownloadTask {

    public static final int STATUS_WAITING = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_FINISHED = 3;
    public static final int STATUS_ERROR = 4;

    public String id;
    public String url;
    public String fileName;
    public String savePath;
    public String mimeType;

    public long totalBytes;
    public long downloadedBytes;
    public long createTime;
    public long lastUpdateTime;
    public long speedBytes;

    public int progress;
    public int status;
    public boolean fileExists;

    public volatile boolean paused = false;

    public DownloadTask(String url, String fileName, String savePath) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.url = url;
        this.fileName = fileName;
        this.savePath = savePath;
        this.createTime = System.currentTimeMillis();
        this.lastUpdateTime = this.createTime;
        this.progress = 0;
        this.status = STATUS_WAITING;
        this.fileExists = false;
        this.mimeType = "";
        this.speedBytes = 0;
    }
}