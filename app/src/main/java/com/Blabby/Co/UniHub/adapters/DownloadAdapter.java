package com.Blabby.Co.UniHub.ui.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.Blabby.Co.UniHub.R;
import com.Blabby.Co.UniHub.download.DownloadManager;
import com.Blabby.Co.UniHub.download.DownloadTask;
import com.Blabby.Co.UniHub.util.DownloadFileUtils;
import com.Blabby.Co.UniHub.util.Localization;

import java.io.File;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.Holder> {

    private List<DownloadTask> tasks;
    private DownloadManager manager;

    public DownloadAdapter(List<DownloadTask> tasks) {
        this.tasks = tasks;
        this.manager = DownloadManager.getInstance();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        DownloadTask task = tasks.get(position);
        Localization l = Localization.getInstance(h.itemView.getContext());

        manager.checkFileExistence(task);

        h.tvName.setText(task.fileName);

        String statusText = "";
        switch (task.status) {
            case DownloadTask.STATUS_WAITING:
                statusText = l.get("status_waiting");
                break;
            case DownloadTask.STATUS_DOWNLOADING:
                statusText = l.get("status_downloading");
                break;
            case DownloadTask.STATUS_PAUSED:
                statusText = l.get("status_paused");
                break;
            case DownloadTask.STATUS_FINISHED:
                statusText = l.get("status_finished");
                break;
            case DownloadTask.STATUS_ERROR:
                statusText = l.get("status_error");
                break;
        }
        if (task.status == DownloadTask.STATUS_FINISHED && !task.fileExists) {
            statusText = l.get("status_file_missing");
        }
        h.tvStatus.setText(statusText);

        h.progressBar.setProgress(task.progress);

        if (task.status == DownloadTask.STATUS_DOWNLOADING && task.speedBytes > 0) {
            h.tvSpeed.setText(DownloadFileUtils.formatSize(task.speedBytes) + "/s");
        } else if (task.status == DownloadTask.STATUS_FINISHED) {
            h.tvSpeed.setText(l.get("completed"));
        } else {
            h.tvSpeed.setText("");
        }

        String downloadedStr = DownloadFileUtils.formatSize(task.downloadedBytes);
        String totalStr = DownloadFileUtils.formatSize(task.totalBytes);
        h.tvSize.setText(downloadedStr + " / " + totalStr);
        h.tvPercent.setText(task.progress + "%");

        if (task.status == DownloadTask.STATUS_DOWNLOADING) {
            h.btnAction.setText(l.get("pause"));
        } else {
            h.btnAction.setText(l.get("resume"));
        }

        h.btnAction.setOnClickListener(v -> {
            if (task.status == DownloadTask.STATUS_DOWNLOADING) {
                manager.pauseTask(task);
            } else if (task.status == DownloadTask.STATUS_PAUSED || task.status == DownloadTask.STATUS_ERROR || task.status == DownloadTask.STATUS_WAITING) {
                manager.resumeTask(task);
            }
            notifyItemChanged(position);
        });

        h.btnDelete.setOnClickListener(v -> {
            manager.deleteTask(task, true);
            notifyItemRemoved(position);
        });

        h.itemView.setOnClickListener(v -> {
            if (task.status == DownloadTask.STATUS_FINISHED && task.fileExists) {
                openFile(h.itemView, task);
            } else if (task.status == DownloadTask.STATUS_FINISHED && !task.fileExists) {
                Toast.makeText(v.getContext(), l.get("file_not_exist_deleted"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(v.getContext(), l.get("download_not_finished"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFile(View view, DownloadTask task) {
        Localization l = Localization.getInstance(view.getContext());
        try {
            File file = new File(task.savePath);
            if (!file.exists()) {
                Toast.makeText(view.getContext(), l.get("file_not_exist"), Toast.LENGTH_SHORT).show();
                return;
            }
            String authority = view.getContext().getPackageName() + ".fileprovider";
            Uri uri = FileProvider.getUriForFile(view.getContext(), authority, file);
            String mime = task.mimeType;
            if (mime == null || mime.isEmpty()) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mime == null) {
                    mime = "*/*";
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, l.get("open_file"));
            view.getContext().startActivity(chooser);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(view.getContext(), l.get("cannot_open_file", e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvSpeed, tvSize, tvPercent;
        ProgressBar progressBar;
        Button btnAction, btnDelete;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
            tvSize = itemView.findViewById(R.id.tv_size);
            tvPercent = itemView.findViewById(R.id.tv_percent);
            progressBar = itemView.findViewById(R.id.progress_bar);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}