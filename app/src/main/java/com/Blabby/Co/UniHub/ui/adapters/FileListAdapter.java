package com.Blabby.Co.UniHub.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.Blabby.Co.UniHub.R;
import com.Blabby.Co.UniHub.data.model.FileItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private List<FileItem> fileList;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(FileItem item);
    }
    public interface OnItemLongClickListener {
        void onItemLongClick(FileItem item, View anchor);
    }

    public FileListAdapter(List<FileItem> fileList,
                           OnItemClickListener clickListener,
                           OnItemLongClickListener longClickListener) {
        this.fileList = fileList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void updateList(List<FileItem> newList) {
        this.fileList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = fileList.get(position);
        holder.bind(item, clickListener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, details;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.file_icon);
            name = itemView.findViewById(R.id.file_name);
            details = itemView.findViewById(R.id.file_details);
        }

        void bind(FileItem item,
                  OnItemClickListener clickListener,
                  OnItemLongClickListener longClickListener) {
            name.setText(item.getName());

            if (item.isParent()) {
                icon.setImageResource(R.drawable.ic_folder_up);
                icon.setColorFilter(Color.WHITE);
                details.setText("返回上级文件夹");
            } else if (item.isDirectory()) {
                icon.setImageResource(R.drawable.ic_folder_24);
                icon.setColorFilter(Color.WHITE);
                details.setText(formatDate(item.getLastModified()));
            } else {
                String lowName = item.getName().toLowerCase();
                if (lowName.endsWith(".mp4") || lowName.endsWith(".avi") || lowName.endsWith(".mkv")
                    || lowName.endsWith(".mov") || lowName.endsWith(".wmv") || lowName.endsWith(".flv")
                    || lowName.endsWith(".3gp") || lowName.endsWith(".webm")) {
                    icon.setImageResource(R.drawable.ic_file_24);
                    icon.setColorFilter(Color.parseColor("#EF4444"));
                    details.setText(formatSize(item.getSize()) + " | " + formatDate(item.getLastModified()));
                } else if (lowName.endsWith(".zip") || lowName.endsWith(".jar") || lowName.endsWith(".apk")) {
                    icon.setImageResource(R.drawable.ic_compress);
                    icon.setColorFilter(Color.parseColor("#F59E0B"));
                    details.setText(formatSize(item.getSize()) + " | " + formatDate(item.getLastModified()));
                } else {
                    icon.setImageResource(R.drawable.ic_file_24);
                    icon.setColorFilter(Color.parseColor("#93C5FD"));
                    details.setText(formatSize(item.getSize()) + " | " + formatDate(item.getLastModified()));
                }
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onItemClick(item);
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && !item.isParent()) {
                    longClickListener.onItemLongClick(item, itemView);
                    return true;
                }
                return false;
            });
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}