package com.Blabby.Co.UniHub;

import android.os.Bundle;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Blabby.Co.UniHub.download.DownloadManager;
import com.Blabby.Co.UniHub.ui.adapters.DownloadAdapter;

public class DownloadActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnAdd = findViewById(R.id.btn_add);
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("下载管理");

        recyclerView = findViewById(R.id.recycler_downloads);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DownloadAdapter(DownloadManager.getInstance().getTasks());
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            EditText editText = new EditText(this);
            editText.setHint("输入下载链接");
            new AlertDialog.Builder(this)
                    .setTitle("新建下载")
                    .setView(editText)
                    .setPositiveButton("下载", (dialog, which) -> {
                        String url = editText.getText().toString().trim();
                        if (url.isEmpty()) return;
                        String fileName = URLUtil.guessFileName(url, null, null);
                        DownloadManager.getInstance().addTask(url, fileName, null);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        DownloadManager.getInstance().setListener(() -> {
            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }
}