package com.Blabby.Co.UniHub.download;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpDownloader {

    public static void download(DownloadTask task, DownloadManager manager) {
        OkHttpClient client = new OkHttpClient();
        try {
            File file = new File(task.savePath);
            long downloaded = 0;
            if (file.exists()) {
                downloaded = file.length();
            }
            task.downloadedBytes = downloaded;
            Request request = new Request.Builder()
                    .url(task.url)
                    .addHeader("Range", "bytes=" + downloaded + "-")
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                task.status = DownloadTask.STATUS_ERROR;
                manager.notifyChanged();
                return;
            }
            long bodyLength = response.body().contentLength();
            task.totalBytes = downloaded + bodyLength;
            InputStream input = response.body().byteStream();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(downloaded);
            byte[] buffer = new byte[8192];
            int len;
            long lastUpdate = 0;
            long lastBytes = downloaded;
            long lastTime = System.currentTimeMillis();
            task.status = DownloadTask.STATUS_DOWNLOADING;
            manager.notifyChanged();
            while ((len = input.read(buffer)) != -1) {
                if (task.paused) {
                    task.status = DownloadTask.STATUS_PAUSED;
                    manager.notifyChanged();
                    input.close();
                    raf.close();
                    return;
                }
                raf.write(buffer, 0, len);
                task.downloadedBytes += len;
                task.progress = (int) ((task.downloadedBytes * 100) / task.totalBytes);
                long now = System.currentTimeMillis();
                long timeDiff = now - lastTime;
                if (timeDiff >= 1000) {
                    long bytesDiff = task.downloadedBytes - lastBytes;
                    task.speedBytes = bytesDiff / (timeDiff / 1000);
                    lastBytes = task.downloadedBytes;
                    lastTime = now;
                }
                if (now - lastUpdate > 300) {
                    lastUpdate = now;
                    task.lastUpdateTime = now;
                    manager.notifyChanged();
                }
            }
            raf.close();
            input.close();
            task.progress = 100;
            task.status = DownloadTask.STATUS_FINISHED;
            task.fileExists = true;
            manager.notifyChanged();
        } catch (Exception e) {
            e.printStackTrace();
            task.status = DownloadTask.STATUS_ERROR;
            manager.notifyChanged();
        }
    }
}