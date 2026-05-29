package com.Blabby.Co.UniHub;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.Blabby.Co.UniHub.util.Localization;

public class WenbenBianjiqi extends AppCompatActivity {

    private EditText editText;
    private TextView tvTitle;
    private ImageButton btnBack, btnSave, btnSaveAs;

    private String filePath;
    private String fileName;
    private boolean modified = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wenben_bianjiqu);

        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        btnSaveAs = findViewById(R.id.btn_save_as);
        editText = findViewById(R.id.edit_text);

        filePath = getIntent().getStringExtra("file_path");
        fileName = getIntent().getStringExtra("file_name");

        if (fileName != null) tvTitle.setText(fileName);
        if (filePath != null) loadFile(filePath);

        btnBack.setOnClickListener(v -> checkSaveBeforeExit());
        btnSave.setOnClickListener(v -> saveFile());
        btnSaveAs.setOnClickListener(v -> showSaveAsDialog());

        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { modified = true; }
        });

        final WenbenBianjiqi activity = this;
        editText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            private static final int ID_SELECT_ALL = 1;
            private static final int ID_COPY = 2;
            private static final int ID_SHARE = 3;
            private static final int ID_CUT = 4;
            private static final int ID_PASTE = 5;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                menu.add(0, ID_SELECT_ALL, 0, Localization.getInstance(activity).get("select_all"));
                menu.add(0, ID_COPY, 1, Localization.getInstance(activity).get("copy"));
                menu.add(0, ID_SHARE, 2, Localization.getInstance(activity).get("share"));
                menu.add(0, ID_CUT, 3, Localization.getInstance(activity).get("cut"));
                menu.add(0, ID_PASTE, 4, Localization.getInstance(activity).get("paste"));
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int selStart = editText.getSelectionStart();
                int selEnd = editText.getSelectionEnd();
                boolean hasSelection = selStart >= 0 && selEnd >= 0 && selStart != selEnd;
                String selectedText = hasSelection ? editText.getText().subSequence(Math.min(selStart, selEnd), Math.max(selStart, selEnd)).toString() : "";

                int id = item.getItemId();
                if (id == ID_SELECT_ALL) {
                    editText.selectAll();
                    mode.finish();
                    return true;
                } else if (id == ID_COPY) {
                    if (hasSelection) {
                        ClipboardManager cm = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("text", selectedText));
                        Toast.makeText(activity, Localization.getInstance(activity).get("copy"), Toast.LENGTH_SHORT).show();
                    }
                    mode.finish();
                    return true;
                } else if (id == ID_SHARE) {
                    if (!hasSelection) {
                        selectedText = editText.getText().toString();
                    }
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, selectedText);
                    activity.startActivity(Intent.createChooser(share, Localization.getInstance(activity).get("share_text")));
                    mode.finish();
                    return true;
                } else if (id == ID_CUT) {
                    if (hasSelection) {
                        ClipboardManager cm = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("text", selectedText));
                        editText.getText().replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), "");
                        Toast.makeText(activity, Localization.getInstance(activity).get("cut"), Toast.LENGTH_SHORT).show();
                    }
                    mode.finish();
                    return true;
                } else if (id == ID_PASTE) {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                    if (cm.hasPrimaryClip()) {
                        ClipData.Item clipItem = cm.getPrimaryClip().getItemAt(0);
                        CharSequence pasteData = clipItem.getText();
                        if (pasteData != null) {
                            int start = Math.min(selStart, selEnd);
                            int end = Math.max(selStart, selEnd);
                            if (start >= 0 && end >= 0 && start != end) {
                                editText.getText().replace(start, end, pasteData);
                            } else {
                                int cursor = editText.getSelectionStart();
                                if (cursor >= 0) {
                                    editText.getText().insert(cursor, pasteData);
                                } else {
                                    editText.append(pasteData);
                                }
                            }
                        }
                    }
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {}
        });
    }

    private void checkSaveBeforeExit() {
        if (modified) {
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle(Localization.getInstance(this).get("unsaved"))
                    .setMessage(Localization.getInstance(this).get("unsaved_msg"))
                    .setPositiveButton(Localization.getInstance(this).get("save"), (d, w) -> { saveFile(); finish(); })
                    .setNegativeButton(Localization.getInstance(this).get("dont_save"), (d, w) -> finish())
                    .setNeutralButton(Localization.getInstance(this).get("cancel"), null)
                    .show();
        } else {
            finish();
        }
    }

    private void loadFile(String path) {
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            editText.setText(sb.toString());
            editText.setSelection(0);
            modified = false;
        } catch (Exception e) {
            Toast.makeText(this, Localization.getInstance(this).get("cant_open_file", e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void saveFile() {
        if (filePath == null) { showSaveAsDialog(); return; }
        if (!checkStoragePermission()) return;
        writeFile(filePath, editText.getText().toString());
    }

    private void showSaveAsDialog() {
        if (!checkStoragePermission()) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_as, null);
        EditText etPath = view.findViewById(R.id.et_save_path);
        EditText etName = view.findViewById(R.id.et_save_name);
        Spinner spEncoding = view.findViewById(R.id.sp_encoding);
        String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1", "UTF-16", "ASCII"};
        spEncoding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, encodings));

        if (filePath != null) {
            etPath.setText(new File(filePath).getParent());
            etName.setText(new File(filePath).getName());
        } else {
            etPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
            etName.setText("untitled.txt");
        }

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(Localization.getInstance(this).get("save_as"))
                .setView(view)
                .setPositiveButton(Localization.getInstance(this).get("save"), (d, w) -> {
                    String dir = etPath.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    if (dir.isEmpty() || name.isEmpty()) {
                        Toast.makeText(this, Localization.getInstance(this).get("path_name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String encoding = (String) spEncoding.getSelectedItem();
                    String fullPath = dir.endsWith("/") ? dir + name : dir + "/" + name;
                    if (encoding != null && !encoding.equals("UTF-8")) {
                        writeFileWithEncoding(fullPath, editText.getText().toString(), encoding);
                    } else {
                        writeFile(fullPath, editText.getText().toString());
                    }
                    filePath = fullPath;
                    fileName = name;
                    tvTitle.setText(fileName);
                    modified = false;
                })
                .setNegativeButton(Localization.getInstance(this).get("cancel"), null)
                .show();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                        .setTitle(Localization.getInstance(this).get("need_perm"))
                        .setMessage(Localization.getInstance(this).get("save_as_need_perm"))
                        .setPositiveButton(Localization.getInstance(this).get("go_to"), (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton(Localization.getInstance(this).get("cancel"), null)
                        .show();
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, Localization.getInstance(this).get("no_storage_perm"), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void writeFile(String path, String content) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.close();
            modified = false;
            Toast.makeText(this, Localization.getInstance(this).get("saved"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, Localization.getInstance(this).get("save_failed", e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void writeFileWithEncoding(String path, String content, String encoding) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, Charset.forName(encoding));
            writer.write(content);
            writer.close();
            fos.close();
            modified = false;
            Toast.makeText(this, Localization.getInstance(this).get("saved_with_encoding", encoding), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, Localization.getInstance(this).get("save_failed", e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        checkSaveBeforeExit();
    }
}
