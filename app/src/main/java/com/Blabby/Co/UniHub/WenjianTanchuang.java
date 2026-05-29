package com.Blabby.Co.UniHub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.io.File;

public class WenjianTanchuang {

    public interface ActionListener {
        void onCopy(File file);
        void onMove(File file);
        void onDelete(File file);
        void onRename(File file);
        void onProperties(File file);
        void onShare(File file);
        void onOpenWith(File file);
        void onCompress(File file);
    }

    public static void show(Context context, File file, ActionListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_file_actions, null);
        LinearLayout btnCopy = dialogView.findViewById(R.id.btn_copy);
        LinearLayout btnMove = dialogView.findViewById(R.id.btn_move);
        LinearLayout btnDelete = dialogView.findViewById(R.id.btn_delete);
        LinearLayout btnRename = dialogView.findViewById(R.id.btn_rename);
        LinearLayout btnProperties = dialogView.findViewById(R.id.btn_properties);
        LinearLayout btnShare = dialogView.findViewById(R.id.btn_share);
        LinearLayout btnOpenWith = dialogView.findViewById(R.id.btn_open_with);
        LinearLayout btnCompress = dialogView.findViewById(R.id.btn_compress);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle(file.getName())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        dialog.setCanceledOnTouchOutside(true);

        btnCopy.setOnClickListener(v -> { listener.onCopy(file); dialog.dismiss(); });
        btnMove.setOnClickListener(v -> { listener.onMove(file); dialog.dismiss(); });
        btnDelete.setOnClickListener(v -> { listener.onDelete(file); dialog.dismiss(); });
        btnRename.setOnClickListener(v -> { listener.onRename(file); dialog.dismiss(); });
        btnProperties.setOnClickListener(v -> { listener.onProperties(file); dialog.dismiss(); });
        btnShare.setOnClickListener(v -> { listener.onShare(file); dialog.dismiss(); });
        btnOpenWith.setOnClickListener(v -> { listener.onOpenWith(file); dialog.dismiss(); });
        btnCompress.setOnClickListener(v -> { listener.onCompress(file); dialog.dismiss(); });

        dialog.show();
    }
}
