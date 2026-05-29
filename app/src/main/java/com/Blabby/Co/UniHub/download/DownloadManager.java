package com.Blabby.Co.UniHub.download;

import android.content.Context;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DownloadManager {

    private static DownloadManager instance;
    private final List<DownloadTask> tasks = new ArrayList<>();
    private DownloadListener listener;
    private Context context;

    public interface DownloadListener {
        void onTasksChanged();
    }

    public void setListener(DownloadListener l) {
        this.listener = l;
    }

    public static DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        loadTasks();
        resumeUnfinishedTasks();
    }

    public List<DownloadTask> getTasks() {
        return tasks;
    }

    public DownloadTask addTask(String url, String fileName, String savePath, String mimeType) {
        File file = new File(savePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        DownloadTask task = new DownloadTask(url, fileName, savePath);
        task.mimeType = mimeType;
        tasks.add(0, task);
        notifyChanged();
        startTask(task);
        return task;
    }

    public DownloadTask addTask(String url, String fileName, String savePath) {
        String mimeType = MimeTypeMap.getFileExtensionFromUrl(fileName);
        return addTask(url, fileName, savePath, mimeType);
    }

    public void startTask(DownloadTask task) {
        if (task.status == DownloadTask.STATUS_FINISHED) {
            return;
        }
        task.paused = false;
        task.status = DownloadTask.STATUS_WAITING;
        notifyChanged();
        new Thread(() -> {
            HttpDownloader.download(task, this);
        }).start();
    }

    public void pauseTask(DownloadTask task) {
        task.paused = true;
        task.status = DownloadTask.STATUS_PAUSED;
        notifyChanged();
    }

    public void resumeTask(DownloadTask task) {
        if (task.status == DownloadTask.STATUS_FINISHED) {
            return;
        }
        startTask(task);
    }

    public void deleteTask(DownloadTask task, boolean deleteFile) {
        task.paused = true;
        if (deleteFile) {
            File file = new File(task.savePath);
            if (file.exists()) {
                file.delete();
            }
        }
        tasks.remove(task);
        notifyChanged();
    }

    public void checkFileExistence(DownloadTask task) {
        File file = new File(task.savePath);
        task.fileExists = file.exists();
    }

    public void notifyChanged() {
        saveTasks();
        if (listener != null) {
            listener.onTasksChanged();
        }
    }

    private void saveTasks() {
        try {
            JSONArray array = new JSONArray();
            for (DownloadTask task : tasks) {
                JSONObject obj = new JSONObject();
                obj.put("id", task.id);
                obj.put("url", task.url);
                obj.put("fileName", task.fileName);
                obj.put("savePath", task.savePath);
                obj.put("mimeType", task.mimeType);
                obj.put("totalBytes", task.totalBytes);
                obj.put("downloadedBytes", task.downloadedBytes);
                obj.put("progress", task.progress);
                obj.put("status", task.status);
                obj.put("createTime", task.createTime);
                obj.put("lastUpdateTime", task.lastUpdateTime);
                obj.put("speedBytes", task.speedBytes);
                obj.put("fileExists", task.fileExists);
                array.put(obj);
            }
            FileOutputStream fos = context.openFileOutput("downloads.json", Context.MODE_PRIVATE);
            fos.write(array.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTasks() {
        try {
            FileInputStream fis = context.openFileInput("downloads.json");
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            String json = new String(buffer);
            JSONArray array = new JSONArray(json);
            tasks.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                DownloadTask task = new DownloadTask(
                        obj.getString("url"),
                        obj.getString("fileName"),
                        obj.getString("savePath")
                );
                task.id = obj.getString("id");
                task.mimeType = obj.optString("mimeType", "");
                task.totalBytes = obj.getLong("totalBytes");
                task.downloadedBytes = obj.getLong("downloadedBytes");
                task.progress = obj.getInt("progress");
                task.status = obj.getInt("status");
                task.createTime = obj.optLong("createTime", System.currentTimeMillis());
                task.lastUpdateTime = obj.optLong("lastUpdateTime", task.createTime);
                task.speedBytes = obj.optLong("speedBytes", 0);
                task.fileExists = obj.optBoolean("fileExists", false);
                tasks.add(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resumeUnfinishedTasks() {
        for (DownloadTask task : tasks) {
            checkFileExistence(task);
            if (task.status != DownloadTask.STATUS_FINISHED) {
                startTask(task);
            }
        }
    }
}