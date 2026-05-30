package com.Blabby.Co.UniHub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.Blabby.Co.UniHub.util.Localization;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipLiulan extends AppCompatActivity {

    private ListView listView;
    private TextView tvTitle;
    private ImageButton btnBack;
    private String zipPath;
    private ZipFile zipFile;
    private final List<Map<String, String>> entryList = new ArrayList<>();
    private final List<ZipEntry> entries = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zip_liulan);

        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        listView = findViewById(R.id.list_view);

        zipPath = getIntent().getStringExtra("zip_path");
        String name = getIntent().getStringExtra("zip_name");

        if (name != null) tvTitle.setText(name);

        btnBack.setOnClickListener(v -> finish());

        loadZipEntries();
    }

    private void loadZipEntries() {
        if (zipPath == null) return;
        try {
            zipFile = new ZipFile(zipPath);
            Enumeration<? extends ZipEntry> enu = zipFile.entries();
            while (enu.hasMoreElements()) {
                ZipEntry entry = enu.nextElement();
                entries.add(entry);
                Map<String, String> map = new HashMap<>();
                map.put("name", entry.getName());
                if (entry.isDirectory()) {
                    map.put("size", "");
                    map.put("date", "");
                } else {
                    map.put("size", formatSize(entry.getSize()));
                    map.put("date", formatDate(entry.getTime()));
                }
                entryList.add(map);
            }

            SimpleAdapter adapter = new SimpleAdapter(
                    this, entryList, R.layout.item_zip_entry,
                    new String[]{"name", "size", "date"},
                    new int[]{R.id.entry_name, R.id.entry_size, R.id.entry_date}
            );
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                ZipEntry entry = entries.get(position);
                if (entry.isDirectory()) return;
                extractAndOpen(entry);
            });

        } catch (Exception e) {
            Toast.makeText(this, Localization.getInstance(this).get("zip_error", e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void extractAndOpen(ZipEntry entry) {
        Localization l = Localization.getInstance(this);
        try {
            File cacheDir = new File(getCacheDir(), "zip_extract");
            cacheDir.mkdirs();
            File outFile = new File(cacheDir, entry.getName());
            outFile.getParentFile().mkdirs();
            try (InputStream is = zipFile.getInputStream(entry);
                 FileOutputStream os = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
            }
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", outFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(outFile.getName()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, l.get("open")));
        } catch (Exception e) {
            Toast.makeText(this, l.get("extract_failed", e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private String getMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": case "avi": case "mkv": return "video/*";
            default: return "*/*";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (zipFile != null) {
            try { zipFile.close(); } catch (Exception ignored) {}
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}