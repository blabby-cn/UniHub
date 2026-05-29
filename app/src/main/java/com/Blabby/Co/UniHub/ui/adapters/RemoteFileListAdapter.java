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
import com.Blabby.Co.UniHub.data.model.RemoteFileEntry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemoteFileListAdapter extends RecyclerView.Adapter<RemoteFileListAdapter.ViewHolder> {
    private List<RemoteFileEntry> items;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemClickListener { void onItemClick(RemoteFileEntry item); }
    public interface OnItemLongClickListener { void onItemLongClick(RemoteFileEntry item, View anchor); }

    public RemoteFileListAdapter(List<RemoteFileEntry> items, OnItemClickListener click, OnItemLongClickListener longClick) {
        this.items = items;
        this.clickListener = click;
        this.longClickListener = longClick;
    }

    public void updateList(List<RemoteFileEntry> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int pos) { holder.bind(items.get(pos), clickListener, longClickListener); }
    @Override public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon; TextView name, details;
        ViewHolder(View v) { super(v); icon = v.findViewById(R.id.file_icon); name = v.findViewById(R.id.file_name); details = v.findViewById(R.id.file_details); }

        void bind(RemoteFileEntry item, OnItemClickListener click, OnItemLongClickListener longClick) {
            name.setText(item.getName());
            if ("Parent Directory".equals(item.getName())) {
                icon.setImageResource(R.drawable.ic_folder_up);
                icon.setColorFilter(Color.parseColor("#F97316"));
                details.setText("返回上级文件夹");
            } else if (item.isDirectory()) {
                icon.setImageResource(R.drawable.ic_folder_24);
                icon.setColorFilter(Color.parseColor("#F97316"));
                details.setText(formatDate(item.getLastModified()));
            } else {
                icon.setImageResource(R.drawable.ic_file_24);
                icon.setColorFilter(Color.parseColor("#1E3A8A"));
                details.setText(formatSize(item.getSize()) + " | " + formatDate(item.getLastModified()));
            }
            itemView.setOnClickListener(v -> { if (click != null) click.onItemClick(item); });
            itemView.setOnLongClickListener(v -> {
                if (longClick != null && !"Parent Directory".equals(item.getName())) {
                    longClick.onItemLongClick(item, itemView);
                    return true;
                }
                return false;
            });
        }

        private static String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp-1)+"";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }

        private static String formatDate(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}