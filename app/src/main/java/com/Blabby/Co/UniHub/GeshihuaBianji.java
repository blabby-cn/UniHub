package com.Blabby.Co.UniHub;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import androidx.drawerlayout.widget.DrawerLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GeshihuaBianji extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ListView lvFileHistory;
    private EditText etEditor;
    private TextView tvLineNumbers, tvFileName, tvStatus;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geshihua_bianji);

        initViews();
        setupDrawer();
        setupEditor();
        setupUndoRedo();
        setupSymbolButtons();
        setupAutoSave();
        loadHistory();

        currentFilePath = getIntent().getStringExtra("file_path");
        currentFileName = getIntent().getStringExtra("file_name");
        if (currentFileName != null) tvFileName.setText(currentFileName);
        if (currentFilePath != null) {
            loadFileWithConflictCheck(currentFilePath);
            addToHistory(currentFilePath, currentFileName);
        } else {
            etEditor.setText("");
            currentFileName = getString(R.string.untitled);
            tvFileName.setText(currentFileName);
        }

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.sideMenu)));
        btnUndo.setOnClickListener(v -> undo());
        btnRedo.setOnClickListener(v -> redo());
        btnSave.setOnClickListener(v -> saveToOriginalPath());
        btnEditTools.setOnClickListener(this::showMarkdownTools);
        btnOverflow.setOnClickListener(this::showOverflowMenu);
        btnNewFile.setOnClickListener(v -> createNewFile());
        btnSideMenuMore.setOnClickListener(v -> showSideMenuMore());
        btnMinimize.setOnClickListener(v -> finish());
        btnPreviewBack.setOnClickListener(v -> hidePreview());

        etEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUndoRedo) {
                    pushToUndoStack(s.toString());
                    redoStack.clear();
                }
                modified = true;
                updateLineNumbers();
                if (autoSaveEnabled && currentFilePath != null) {
                    saveToCache();
                }
            }
        });

        etEditor.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            tvLineNumbers.scrollTo(0, scrollY);
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        lvFileHistory = findViewById(R.id.lvFileHistory);
        etEditor = findViewById(R.id.etEditor);
        tvLineNumbers = findViewById(R.id.tvLineNumbers);
        tvFileName = findViewById(R.id.tvFileName);
        tvStatus = findViewById(R.id.tvStatus);
        scrollView = findViewById(R.id.scrollView);
        webPreview = findViewById(R.id.webPreview);
        btnMenu = findViewById(R.id.btnMenu);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnSave = findViewById(R.id.btnSave);
        btnEditTools = findViewById(R.id.btnEditTools);
        btnOverflow = findViewById(R.id.btnOverflow);
        btnNewFile = findViewById(R.id.btnNewFile);
        btnSideMenuMore = findViewById(R.id.btnSideMenuMore);
        btnMinimize = findViewById(R.id.btnMinimize);
        btnSymbolRight = findViewById(R.id.btnSymbolRight);
        btnSymbolSlash = findViewById(R.id.btnSymbolSlash);
        btnSymbolPlus = findViewById(R.id.btnSymbolPlus);
        btnSymbolMinus = findViewById(R.id.btnSymbolMinus);
        btnSymbolStar = findViewById(R.id.btnSymbolStar);
        btnSymbolEqual = findViewById(R.id.btnSymbolEqual);
        btnSymbolLt = findViewById(R.id.btnSymbolLt);
        btnSymbolGt = findViewById(R.id.btnSymbolGt);
        bottomBar = findViewById(R.id.bottomBar);
        previewContainer = findViewById(R.id.previewContainer);
        btnPreviewBack = findViewById(R.id.btnPreviewBack);
    }

    private void setupUndoRedo() {
        pushToUndoStack("");
    }

    private void pushToUndoStack(String text) {
        if (!undoStack.isEmpty() && undoStack.peek().equals(text)) return;
        undoStack.push(text);
        if (undoStack.size() > 100) {
            undoStack.remove(0);
        }
        updateUndoRedoButtons();
    }

    private void undo() {
        if (undoStack.size() <= 1) return;
        isUndoRedo = true;
        String current = etEditor.getText().toString();
        redoStack.push(current);
        undoStack.pop();
        String previous = undoStack.peek();
        etEditor.setText(previous);
        etEditor.setSelection(previous.length());
        isUndoRedo = false;
        updateUndoRedoButtons();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        isUndoRedo = true;
        String next = redoStack.pop();
        pushToUndoStack(next);
        etEditor.setText(next);
        etEditor.setSelection(next.length());
        isUndoRedo = false;
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        btnUndo.setEnabled(undoStack.size() > 1);
        btnRedo.setEnabled(!redoStack.isEmpty());
    }

    private void setupDrawer() {
        historyPrefs = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
        historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view;
                text.setText(historyList.get(position).displayName);
                text.setTextColor(getColor(R.color.on_surface));
                text.setPadding(32, 16, 16, 16);
                return view;
            }
        };
        lvFileHistory.setAdapter(historyAdapter);
        lvFileHistory.setOnItemClickListener((parent, view, position, id) -> {
            HistoryItem item = historyList.get(position);
            if (currentFilePath != null && modified && autoSaveEnabled) saveToCache();
            currentFilePath = item.filePath;
            currentFileName = new File(item.filePath).getName();
            tvFileName.setText(currentFileName);
            loadFileWithConflictCheck(currentFilePath);
            drawerLayout.closeDrawers();
        });
        lvFileHistory.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.remove_from_history))
                    .setMessage(historyList.get(position).displayName)
                    .setPositiveButton(getString(R.string.ok), (d, w) -> removeFromHistory(position))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            return true;
        });
    }

    private void setupEditor() {
        etEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateLineNumbers(); }
        });
    }

    private void updateLineNumbers() {
        String text = etEditor.getText().toString();
        int lines = text.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        tvLineNumbers.setText(sb);
    }

    private void setupSymbolButtons() {
        View.OnClickListener insertSymbol = v -> {
            String sym = ((TextView) v).getText().toString();
            int start = etEditor.getSelectionStart();
            if (start < 0) start = 0;
            etEditor.getText().insert(start, sym);
            etEditor.setSelection(start + sym.length());
        };
        btnSymbolRight.setOnClickListener(insertSymbol);
        btnSymbolSlash.setOnClickListener(insertSymbol);
        btnSymbolPlus.setOnClickListener(insertSymbol);
        btnSymbolMinus.setOnClickListener(insertSymbol);
        btnSymbolStar.setOnClickListener(insertSymbol);
        btnSymbolEqual.setOnClickListener(insertSymbol);
        btnSymbolLt.setOnClickListener(insertSymbol);
        btnSymbolGt.setOnClickListener(insertSymbol);
    }

    private void setupAutoSave() {
        SharedPreferences prefs = getSharedPreferences("editor_settings", MODE_PRIVATE);
        autoSaveEnabled = prefs.getBoolean("auto_save", true);
        if (autoSaveEnabled) startAutoSaveTimer();
    }

    private void startAutoSaveTimer() {
        if (autoSaveExecutor != null) autoSaveExecutor.shutdown();
        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor();
        autoSaveExecutor.scheduleAtFixedRate(() -> {
            if (autoSaveEnabled && currentFilePath != null && modified) {
                runOnUiThread(() -> saveToCache());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void saveToCache() {
        if (currentFilePath == null) return;
        try {
            File cacheDir = new File(getFilesDir(), "editor_cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            String hash = String.valueOf(currentFilePath.hashCode());
            File cacheFile = new File(cacheDir, hash + ".cache");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(etEditor.getText().toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception ignored) { }
    }

    private String loadFromCache(String originalPath) {
        try {
            File cacheDir = new File(getFilesDir(), "editor_cache");
            String hash = String.valueOf(originalPath.hashCode());
            File cacheFile = new File(cacheDir, hash + ".cache");
            if (cacheFile.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String loadFromOriginal(String path) {
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadFileWithConflictCheck(String path) {
        String cached = loadFromCache(path);
        String original = loadFromOriginal(path);
        if (cached != null && original != null && !cached.equals(original)) {
            new AlertDialog.Builder(this)
                    .setTitle("文件冲突")
                    .setMessage("检测到原文件已被外部修改，是否使用缓存中的内容？\n\n选择\"使用缓存\"：恢复上次未保存的编辑\n选择\"使用源文件\"：放弃未保存的更改")
                    .setPositiveButton("使用缓存", (d, w) -> {
                        etEditor.setText(cached);
                        modified = true;
                        updateLineNumbers();
                        undoStack.clear();
                        redoStack.clear();
                        pushToUndoStack(cached);
                    })
                    .setNegativeButton("使用源文件", (d, w) -> {
                        etEditor.setText(original);
                        modified = false;
                        updateLineNumbers();
                        undoStack.clear();
                        redoStack.clear();
                        pushToUndoStack(original);
                        saveToCache();
                    })
                    .show();
        } else if (cached != null) {
            etEditor.setText(cached);
            modified = true;
            updateLineNumbers();
            undoStack.clear();
            redoStack.clear();
            pushToUndoStack(cached);
        } else if (original != null) {
            etEditor.setText(original);
            modified = false;
            updateLineNumbers();
            undoStack.clear();
            redoStack.clear();
            pushToUndoStack(original);
        } else {
            etEditor.setText("");
            modified = false;
            Toast.makeText(this, getString(R.string.cant_open_file, "文件不存在或无法读取"), Toast.LENGTH_LONG).show();
        }
        etEditor.setSelection(0);
    }

    private void saveToOriginalPath() {
        if (currentFilePath == null) {
            showSaveAsDialog();
            return;
        }
        if (!checkStoragePermission()) return;
        try {
            File file = new File(currentFilePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(etEditor.getText().toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
            modified = false;
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
            saveToCache();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void showSaveAsDialog() {
        if (!checkStoragePermission()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_as, null);
        EditText etPath = view.findViewById(R.id.et_save_path);
        EditText etName = view.findViewById(R.id.et_save_name);
        android.widget.Spinner spEncoding = view.findViewById(R.id.sp_encoding);
        String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1", "UTF-16", "ASCII"};
        spEncoding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, encodings));
        etPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        etName.setText(currentFileName != null ? currentFileName : "untitled.md");
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.save_as))
                .setView(view)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String dir = etPath.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    String encoding = (String) spEncoding.getSelectedItem();
                    String fullPath = dir.endsWith("/") ? dir + name : dir + "/" + name;
                    try {
                        File file = new File(fullPath);
                        File parent = file.getParentFile();
                        if (parent != null && !parent.exists()) parent.mkdirs();
                        if (encoding != null && !encoding.equals("UTF-8")) {
                            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName(encoding));
                            writer.write(etEditor.getText().toString());
                            writer.close();
                        } else {
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(etEditor.getText().toString().getBytes(StandardCharsets.UTF_8));
                            fos.close();
                        }
                        currentFilePath = fullPath;
                        currentFileName = name;
                        tvFileName.setText(currentFileName);
                        modified = false;
                        addToHistory(currentFilePath, currentFileName);
                        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
                        saveToCache();
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.save_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.need_perm))
                        .setMessage(getString(R.string.save_as_need_perm))
                        .setPositiveButton(getString(R.string.go_to), (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.no_storage_perm), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void showMarkdownTools(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "H1");
        popup.getMenu().add(0, 2, 1, "H2");
        popup.getMenu().add(0, 3, 2, "H3");
        popup.getMenu().add(0, 4, 3, "H4");
        popup.getMenu().add(0, 5, 4, "H5");
        popup.getMenu().add(0, 6, 5, "H6");
        popup.getMenu().add(0, 7, 6, getString(R.string.inline_code));
        popup.getMenu().add(0, 8, 7, getString(R.string.code_block));
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) insertAtLine("# ");
            else if (id == 2) insertAtLine("## ");
            else if (id == 3) insertAtLine("### ");
            else if (id == 4) insertAtLine("#### ");
            else if (id == 5) insertAtLine("##### ");
            else if (id == 6) insertAtLine("###### ");
            else if (id == 7) insertInlineCode();
            else if (id == 8) showCodeBlockDialog();
            return true;
        });
        popup.show();
    }

    private void insertAtLine(String prefix) {
        int start = etEditor.getSelectionStart();
        int lineStart = start;
        CharSequence text = etEditor.getText();
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') lineStart--;
        etEditor.getText().insert(lineStart, prefix);
    }

    private void insertInlineCode() {
        int start = etEditor.getSelectionStart();
        int end = etEditor.getSelectionEnd();
        if (start == end) {
            etEditor.getText().insert(start, "``");
            etEditor.setSelection(start + 1);
        } else {
            String sel = etEditor.getText().subSequence(start, end).toString();
            etEditor.getText().replace(start, end, "`" + sel + "`");
        }
    }

    private void showCodeBlockDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_code_block, null);
        android.widget.Spinner spLang = view.findViewById(R.id.sp_code_lang);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CODE_LANGUAGES);
        spLang.setAdapter(adapter);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.code_block))
                .setView(view)
                .setPositiveButton(getString(R.string.insert), (d, w) -> {
                    String lang = (String) spLang.getSelectedItem();
                    insertCodeBlock(lang);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void insertCodeBlock(String language) {
        String langMap = getLangKey(language);
        StringBuilder sb = new StringBuilder();
        sb.append("```").append(langMap).append("\n");
        int start = etEditor.getSelectionStart();
        int end = etEditor.getSelectionEnd();
        if (start != end) {
            sb.append(etEditor.getText().subSequence(start, end));
        } else {
            sb.append(getCodeTemplate(language));
        }
        sb.append("\n```");
        etEditor.getText().replace(start, end, sb.toString());
        int cursor = start + 3 + langMap.length() + 1;
        etEditor.setSelection(cursor);
    }

    private String getLangKey(String lang) {
        switch (lang) {
            case "C#": return "csharp";
            case "C++": return "cpp";
            case "TS": return "typescript";
            case "JS": return "javascript";
            case "ObjC": return "objectivec";
            case "汇编": return "assembly";
            case "HTML": return "html";
            case "XML": return "xml";
            case "JSON": return "json";
            case "YAML": return "yaml";
            default: return lang.toLowerCase();
        }
    }

    private String getCodeTemplate(String lang) {
        switch (lang) {
            case "C#": return "Console.WriteLine(\"Hello!\");";
            case "C": return "puts(\"Hello!\");";
            case "C++": return "std::cout << \"Hello!\" << std::endl;";
            case "Rust": return "println!(\"Hello!\");";
            case "TS": return "console.log(\"Hello!\");";
            case "JS": return "console.log(\"Hello!\");";
            case "Zig": return "std.debug.print(\"Hello!\\n\", .{});";
            case "Go": return "fmt.Println(\"Hello!\")";
            case "Swift": return "print(\"Hello!\")";
            case "Kotlin": return "println(\"Hello!\")";
            case "ObjC": return "NSLog(@\"Hello!\");";
            case "汇编": return "mov rax, 1\nmov rdi, 1\nmov rsi, msg\nmov rdx, 7\nsyscall";
            case "Ruby": return "puts \"Hello!\"";
            case "Python": return "print(\"Hello!\")";
            case "HTML": return "<!DOCTYPE html>\n<html>\n<head><title>Page</title></head>\n<body>\n  <h1>Hello!</h1>\n</body>\n</html>";
            case "XML": return "<root>\n  <message>Hello!</message>\n</root>";
            case "JSON": return "{\n  \"message\": \"Hello!\"\n}";
            case "YAML": return "message: Hello!";
            case "Lisp": return "(print \"Hello!\")";
            default: return "puts \"Hello!\"";
        }
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, getString(R.string.preview));
        popup.getMenu().add(0, 2, 1, getString(R.string.edit_settings));
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                showPreview();
            } else if (id == 2) {
                startActivity(new Intent(this, EditSettingsActivity.class));
            }
            return true;
        });
        popup.show();
    }

    private void showPreview() {
        if (showingPreview) return;
        showingPreview = true;
        String markdown = etEditor.getText().toString();
        String html = markdownToHtml(markdown);
        webPreview.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        bottomBar.setVisibility(View.GONE);
        previewContainer.setVisibility(View.VISIBLE);
    }

    private void hidePreview() {
        showingPreview = false;
        bottomBar.setVisibility(View.VISIBLE);
        previewContainer.setVisibility(View.GONE);
    }

    private String markdownToHtml(String md) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        sb.append("<style>");
        sb.append("body { font-family: sans-serif; padding: 12px; background: #0F172A; color: #FFFFFF; line-height: 1.6; }");
        sb.append("h1 { font-size: 24px; border-bottom: 1px solid #334155; padding-bottom: 6px; }");
        sb.append("h2 { font-size: 20px; border-bottom: 1px solid #334155; padding-bottom: 4px; }");
        sb.append("h3 { font-size: 18px; }");
        sb.append("h4 { font-size: 16px; }");
        sb.append("h5 { font-size: 14px; }");
        sb.append("h6 { font-size: 13px; color: #94A3B8; }");
        sb.append("pre { background: #1E293B; padding: 10px; border-radius: 6px; overflow-x: auto; }");
        sb.append("code { background: #1E293B; padding: 2px 4px; border-radius: 3px; font-size: 90%; }");
        sb.append("pre code { background: none; padding: 0; }");
        sb.append("blockquote { border-left: 3px solid #3B82F6; margin: 8px 0; padding: 4px 12px; color: #94A3B8; }");
        sb.append("table { border-collapse: collapse; width: 100%; }");
        sb.append("th, td { border: 1px solid #334155; padding: 6px 10px; text-align: left; }");
        sb.append("th { background: #1E293B; }");
        sb.append("a { color: #60A5FA; }");
        sb.append("img { max-width: 100%; }");
        sb.append("hr { border: none; border-top: 1px solid #334155; }");
        sb.append("ul, ol { padding-left: 24px; }");
        sb.append("</style></head><body>");
        String[] lines = md.split("\n", -1);
        boolean inCodeBlock = false;
        StringBuilder codeBlock = new StringBuilder();
        boolean inTable = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    sb.append("<pre><code>").append(escapeHtml(codeBlock.toString())).append("</code></pre>\n");
                    codeBlock = new StringBuilder();
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                if (codeBlock.length() > 0) codeBlock.append("\n");
                codeBlock.append(line);
                continue;
            }
            if (trimmed.isEmpty()) {
                sb.append("<p>");
                while (i + 1 < lines.length && lines[i + 1].trim().isEmpty()) i++;
                sb.append("</p>\n");
                continue;
            }
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (!inTable) { sb.append("<table>"); inTable = true; }
                String[] cells = trimmed.split("\\|");
                boolean isHeader = i + 1 < lines.length && lines[i + 1].trim().matches("^[|\\s:-]+$");
                sb.append("<tr>");
                for (int c = 1; c < cells.length - 1; c++) {
                    String cell = cells[c].trim();
                    if (cell.isEmpty()) continue;
                    if (isHeader) sb.append("<th>").append(renderInline(cell)).append("</th>");
                    else sb.append("<td>").append(renderInline(cell)).append("</td>");
                }
                sb.append("</tr>");
                if (isHeader) i++;
                continue;
            } else if (inTable) { sb.append("</table>"); inTable = false; }
            if (trimmed.startsWith("###### ")) sb.append("<h6>").append(renderInline(trimmed.substring(7))).append("</h6>\n");
            else if (trimmed.startsWith("##### ")) sb.append("<h5>").append(renderInline(trimmed.substring(6))).append("</h5>\n");
            else if (trimmed.startsWith("#### ")) sb.append("<h4>").append(renderInline(trimmed.substring(5))).append("</h4>\n");
            else if (trimmed.startsWith("### ")) sb.append("<h3>").append(renderInline(trimmed.substring(4))).append("</h3>\n");
            else if (trimmed.startsWith("## ")) sb.append("<h2>").append(renderInline(trimmed.substring(3))).append("</h2>\n");
            else if (trimmed.startsWith("# ")) sb.append("<h1>").append(renderInline(trimmed.substring(2))).append("</h1>\n");
            else if (trimmed.startsWith("> ")) sb.append("<blockquote>").append(renderInline(trimmed.substring(2))).append("</blockquote>\n");
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) sb.append("<li>").append(renderInline(trimmed.substring(2))).append("</li>\n");
            else if (trimmed.matches("^\\d+\\.\\s.*")) { int dot = trimmed.indexOf('.'); sb.append("<li>").append(renderInline(trimmed.substring(dot + 1).trim())).append("</li>\n"); }
            else if (trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("___")) sb.append("<hr />\n");
            else sb.append("<p>").append(renderInline(trimmed)).append("</p>\n");
        }
        if (inCodeBlock) sb.append("<pre><code>").append(escapeHtml(codeBlock.toString())).append("</code></pre>\n");
        if (inTable) sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String renderInline(String text) {
        String r = escapeHtml(text);
        r = r.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        r = r.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
        r = r.replaceAll("`([^`]+)`", "<code>$1</code>");
        r = r.replaceAll("!\\[([^]]*)\\]\\(([^)]+)\\)", "<img src=\"$2\" alt=\"$1\" />");
        r = r.replaceAll("\\[([^]]+)\\]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");
        r = r.replaceAll("~~(.+?)~~", "<s>$1</s>");
        return r;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void showSideMenuMore() {
        PopupMenu popup = new PopupMenu(this, btnSideMenuMore);
        popup.getMenu().add(0, 1, 0, getString(R.string.clear_history));
        popup.setOnMenuItemClickListener(item -> {
            historyList.clear();
            saveHistorySet();
            refreshHistoryList();
            return true;
        });
        popup.show();
    }

    private void createNewFile() {
        if (modified && autoSaveEnabled && currentFilePath != null) saveToCache();
        currentFilePath = null;
        currentFileName = getString(R.string.untitled);
        tvFileName.setText(currentFileName);
        etEditor.setText("");
        modified = false;
        undoStack.clear();
        redoStack.clear();
        pushToUndoStack("");
    }

    private void addToHistory(String path, String name) {
        for (HistoryItem item : historyList) {
            if (item.filePath.equals(path)) return;
        }
        historyList.add(0, new HistoryItem(path, name));
        if (historyList.size() > 50) historyList.remove(historyList.size() - 1);
        saveHistorySet();
        refreshHistoryList();
    }

    private void removeFromHistory(int index) {
        historyList.remove(index);
        saveHistorySet();
        refreshHistoryList();
    }

    private void saveHistorySet() {
        Set<String> set = new HashSet<>();
        for (HistoryItem item : historyList) set.add(item.filePath + "|" + item.displayName);
        historyPrefs.edit().putStringSet(HISTORY_SET, set).apply();
    }

    private void loadHistory() {
        Set<String> set = historyPrefs.getStringSet(HISTORY_SET, new HashSet<>());
        historyList.clear();
        for (String s : set) {
            String[] parts = s.split("\\|", 2);
            if (parts.length == 2) historyList.add(new HistoryItem(parts[0], parts[1]));
        }
        refreshHistoryList();
    }

    private void refreshHistoryList() {
        ArrayList<String> display = new ArrayList<>();
        for (HistoryItem item : historyList) display.add(item.displayName);
        historyAdapter.clear();
        historyAdapter.addAll(display);
        historyAdapter.notifyDataSetChanged();
    }

    private static class HistoryItem {
        String filePath, displayName;
        HistoryItem(String path, String name) { filePath = path; displayName = name; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveExecutor != null) autoSaveExecutor.shutdown();
        if (autoSaveEnabled && currentFilePath != null) saveToCache();
    }

    @Override
    public void onBackPressed() {
        if (showingPreview) {
            hidePreview();
        } else if (drawerLayout.isDrawerOpen(findViewById(R.id.sideMenu))) {
            drawerLayout.closeDrawers();
        } else if (modified && autoSaveEnabled) {
            saveToCache();
            finish();
        } else if (modified) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.unsaved))
                    .setMessage(getString(R.string.unsaved_msg))
                    .setPositiveButton(getString(R.string.save), (d, w) -> { saveToOriginalPath(); finish(); })
                    .setNegativeButton(getString(R.string.dont_save), (d, w) -> finish())
                    .setNeutralButton(getString(R.string.cancel), null)
                    .show();
        } else {
            finish();
        }
    }
}