package com.Blabby.Co.UniHub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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

public class GeshihuaBianji extends AppCompatActivity {

    private EditText editSource;
    private WebView webPreview;
    private ImageButton btnBack, btnSave, btnTools, btnSaveAs;
    private TextView tvTitle;

    private String filePath;
    private String fileName;
    private boolean modified = false;
    private boolean showingPreview = false;

    private static final String[] CODE_LANGUAGES = {
        "C#", "C", "C++", "Rust", "TS", "JS", "Zig", "Go",
        "Swift", "Kotlin", "ObjC", "汇编", "Ruby", "Python",
        "HTML", "XML", "JSON", "YAML", "Lisp"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geshihua_bianji);

        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        btnTools = findViewById(R.id.btn_tools);
        btnSaveAs = findViewById(R.id.btn_save_as);
        editSource = findViewById(R.id.edit_source);
        webPreview = findViewById(R.id.web_preview);

        filePath = getIntent().getStringExtra("file_path");
        fileName = getIntent().getStringExtra("file_name");

        if (fileName != null) tvTitle.setText(fileName);
        if (filePath != null) loadFile(filePath);

        btnBack.setOnClickListener(v -> {
            if (showingPreview) {
                togglePreview();
            } else {
                checkSaveBeforeExit();
            }
        });
        btnSave.setOnClickListener(v -> saveFile());
        btnSaveAs.setOnClickListener(v -> showSaveAsDialog());
        btnTools.setOnClickListener(this::showToolsMenu);

        editSource.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { modified = true; }
        });
    }

    private void showToolsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "H1");
        popup.getMenu().add(0, 2, 1, "H2");
        popup.getMenu().add(0, 3, 2, "H3");
        popup.getMenu().add(0, 4, 3, "H4");
        popup.getMenu().add(0, 5, 4, "H5");
        popup.getMenu().add(0, 6, 5, "H6");
        popup.getMenu().add(0, 7, 6, "行内代码 `code`");
        popup.getMenu().add(0, 8, 7, Localization.getInstance(this).get("code_block"));
        popup.getMenu().add(0, 9, 8, "一键预览");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: insertAtLine("# "); return true;
                case 2: insertAtLine("## "); return true;
                case 3: insertAtLine("### "); return true;
                case 4: insertAtLine("#### "); return true;
                case 5: insertAtLine("##### "); return true;
                case 6: insertAtLine("###### "); return true;
                case 7: insertInlineCode(); return true;
                case 8: showCodeBlockDialog(); return true;
                case 9: if (!showingPreview) togglePreview(); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private void insertAtLine(String prefix) {
        int start = editSource.getSelectionStart();
        int lineStart = start;
        CharSequence text = editSource.getText();
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') lineStart--;
        editSource.getText().insert(lineStart, prefix);
    }

    private void insertInlineCode() {
        int start = editSource.getSelectionStart();
        int end = editSource.getSelectionEnd();
        if (start == end) {
            editSource.getText().insert(start, "``");
            editSource.setSelection(start + 1);
        } else {
            String sel = editSource.getText().subSequence(start, end).toString();
            editSource.getText().replace(start, end, "`" + sel + "`");
        }
    }

    private void showCodeBlockDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_code_block, null);
        Spinner spLang = view.findViewById(R.id.sp_code_lang);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CODE_LANGUAGES);
        spLang.setAdapter(adapter);

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(Localization.getInstance(this).get("code_block"))
                .setView(view)
                .setPositiveButton(Localization.getInstance(this).get("insert"), (d, w) -> {
                    String lang = (String) spLang.getSelectedItem();
                    insertCodeBlock(lang);
                })
                .setNegativeButton(Localization.getInstance(this).get("cancel"), null)
                .show();
    }

    private void insertCodeBlock(String language) {
        String langMap = getLangKey(language);
        String template = getCodeTemplate(language);
        StringBuilder sb = new StringBuilder();
        sb.append("```").append(langMap).append("\n");
        int start = editSource.getSelectionStart();
        int end = editSource.getSelectionEnd();
        if (start != end) {
            sb.append(editSource.getText().subSequence(start, end));
        } else {
            sb.append(template);
        }
        sb.append("\n```");
        editSource.getText().replace(start, end, sb.toString());
        int cursor = start + 3 + langMap.length() + 1;
        editSource.setSelection(cursor);
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

    private void togglePreview() {
        showingPreview = !showingPreview;
        if (showingPreview) {
            renderPreview();
            editSource.setVisibility(View.GONE);
            webPreview.setVisibility(View.VISIBLE);
        } else {
            editSource.setVisibility(View.VISIBLE);
            webPreview.setVisibility(View.GONE);
        }
    }

    private void checkSaveBeforeExit() {
        if (!modified) { finish(); return; }
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(Localization.getInstance(this).get("unsaved"))
                .setMessage(Localization.getInstance(this).get("unsaved_msg"))
                .setPositiveButton(Localization.getInstance(this).get("save"), (d, w) -> { saveFile(); finish(); })
                .setNegativeButton(Localization.getInstance(this).get("dont_save"), (d, w) -> finish())
                .setNeutralButton(Localization.getInstance(this).get("cancel"), null)
                .show();
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
            editSource.setText(sb.toString());
            modified = false;
        } catch (Exception e) {
            editSource.setText(Localization.getInstance(this).get("cant_open_file", e.getMessage()));
        }
    }

    private void saveFile() {
        if (filePath == null) { showSaveAsDialog(); return; }
        if (!checkStoragePermission()) return;
        writeFile(filePath, editSource.getText().toString());
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
            etName.setText("untitled.md");
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
                        writeFileWithEncoding(fullPath, editSource.getText().toString(), encoding);
                    } else {
                        writeFile(fullPath, editSource.getText().toString());
                    }
                    filePath = fullPath;
                    fileName = name;
                    tvTitle.setText(fileName);
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

    private void renderPreview() {
        String markdown = editSource.getText().toString();
        String html = markdownToHtml(markdown);
        webPreview.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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
                while (i + 1 < lines.length && lines[i + 1].trim().isEmpty()) {
                    i++;
                }
                sb.append("</p>\n");
                continue;
            }

            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (!inTable) {
                    sb.append("<table>");
                    inTable = true;
                }
                String[] cells = trimmed.split("\\|");
                boolean isHeader = i + 1 < lines.length && lines[i + 1].trim().matches("^[|\\s:-]+$");
                sb.append("<tr>");
                for (int c = 1; c < cells.length - 1; c++) {
                    String cell = cells[c].trim();
                    if (cell.isEmpty()) continue;
                    if (isHeader) {
                        sb.append("<th>").append(renderInline(cell)).append("</th>");
                    } else {
                        sb.append("<td>").append(renderInline(cell)).append("</td>");
                    }
                }
                sb.append("</tr>");
                if (isHeader) i++;
                continue;
            } else if (inTable) {
                sb.append("</table>");
                inTable = false;
            }

            if (trimmed.startsWith("###### ")) {
                sb.append("<h6>").append(renderInline(trimmed.substring(7))).append("</h6>\n");
            } else if (trimmed.startsWith("##### ")) {
                sb.append("<h5>").append(renderInline(trimmed.substring(6))).append("</h5>\n");
            } else if (trimmed.startsWith("#### ")) {
                sb.append("<h4>").append(renderInline(trimmed.substring(5))).append("</h4>\n");
            } else if (trimmed.startsWith("### ")) {
                sb.append("<h3>").append(renderInline(trimmed.substring(4))).append("</h3>\n");
            } else if (trimmed.startsWith("## ")) {
                sb.append("<h2>").append(renderInline(trimmed.substring(3))).append("</h2>\n");
            } else if (trimmed.startsWith("# ")) {
                sb.append("<h1>").append(renderInline(trimmed.substring(2))).append("</h1>\n");
            } else if (trimmed.startsWith("> ")) {
                sb.append("<blockquote>").append(renderInline(trimmed.substring(2))).append("</blockquote>\n");
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sb.append("<li>").append(renderInline(trimmed.substring(2))).append("</li>\n");
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                int dot = trimmed.indexOf('.');
                sb.append("<li>").append(renderInline(trimmed.substring(dot + 1).trim())).append("</li>\n");
            } else if (trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("___")) {
                sb.append("<hr />\n");
            } else {
                sb.append("<p>").append(renderInline(trimmed)).append("</p>\n");
            }
        }

        if (inCodeBlock) {
            sb.append("<pre><code>").append(escapeHtml(codeBlock.toString())).append("</code></pre>\n");
        }
        if (inTable) {
            sb.append("</table>");
        }

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

    @Override
    public void onBackPressed() {
        if (showingPreview) {
            togglePreview();
        } else {
            checkSaveBeforeExit();
        }
    }
}
