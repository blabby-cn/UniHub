package com.Blabby.Co.UniHub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.util.ArrayList;
import java.util.List;

public class EditSettingsActivity extends AppCompatActivity {

    private SwitchCompat switchAutoSave, switchWordWrap;
    private SharedPreferences prefs;
    private Spinner spinnerSyntax;

    private static final String[][] SYNTAX_OPTIONS = {
            {"auto", "自动识别（根据文件扩展名）"},
            {"none", "无高亮"},
            {"source.java", "Java"},
            {"source.kotlin", "Kotlin"},
            {"source.python", "Python"},
            {"source.javascript", "JavaScript"},
            {"source.typescript", "TypeScript"},
            {"text.html.basic", "HTML"},
            {"source.css", "CSS"},
            {"text.xml", "XML"},
            {"source.json", "JSON"},
            {"source.yaml", "YAML"},
            {"source.shell", "Shell / Bash"},
            {"source.c", "C"},
            {"source.cpp", "C++"},
            {"source.cs", "C#"},
            {"source.rust", "Rust"},
            {"source.go", "Go"},
            {"source.ruby", "Ruby"},
            {"source.swift", "Swift"},
            {"source.php", "PHP"},
            {"source.pl", "Perl"},
            {"source.sql", "SQL"},
            {"source.lua", "Lua"},
            {"source.scala", "Scala"},
            {"source.dart", "Dart"},
            {"source.groovy", "Groovy"},
            {"text.html.markdown", "Markdown"},
            {"source.toml", "TOML"},
            {"source.ini", "INI"},
            {"source.bat", "Batch"},
            {"source.tex", "LaTeX"},
            {"text.csv", "CSV"},
            {"source.log", "Log"},
            {"text.plain", "Plain Text"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_settings);

        prefs = getSharedPreferences("editor_settings", MODE_PRIVATE);

        ImageButton btnBack = findViewById(R.id.btn_back);
        switchAutoSave = findViewById(R.id.switch_auto_save);
        switchWordWrap = findViewById(R.id.switch_word_wrap);
        spinnerSyntax = findViewById(R.id.spinner_syntax);
        TextView btnHelp = findViewById(R.id.btn_help);

        switchAutoSave.setChecked(prefs.getBoolean("auto_save", true));
        switchWordWrap.setChecked(prefs.getBoolean("word_wrap", false));

        setupSyntaxSpinner();

        btnBack.setOnClickListener(v -> finish());

        switchAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                prefs.edit().putBoolean("auto_save", true).apply();
            } else {
                showWarningDialog();
            }
        });

        switchWordWrap.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("word_wrap", isChecked).apply();
            showWordWrapRestartDialog();
        });

        btnHelp.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.auto_save_help_title))
                    .setMessage(getString(R.string.auto_save_help_msg))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        });
    }

    private void setupSyntaxSpinner() {
        List<String> displayNames = new ArrayList<>();
        for (String[] opt : SYNTAX_OPTIONS) {
            displayNames.add(opt[1]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, displayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSyntax.setAdapter(adapter);

        String savedLang = prefs.getString("syntax_lang", "auto");
        int selectedIndex = 0;
        for (int i = 0; i < SYNTAX_OPTIONS.length; i++) {
            if (SYNTAX_OPTIONS[i][0].equals(savedLang)) {
                selectedIndex = i;
                break;
            }
        }
        spinnerSyntax.setSelection(selectedIndex);

        spinnerSyntax.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String langId = SYNTAX_OPTIONS[position][0];
                prefs.edit().putString("syntax_lang", langId).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showWarningDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.auto_save_warning_title))
                .setMessage(getString(R.string.auto_save_warning_msg_brief))
                .setPositiveButton(getString(R.string.ok), (d, w) -> {
                    prefs.edit().putBoolean("auto_save", false).apply();
                    switchAutoSave.setChecked(false);
                    finish();
                })
                .setNegativeButton(getString(R.string.cancel), (d, w) -> {
                    switchAutoSave.setChecked(true);
                })
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new CountDownTimer(10000, 1000) {
                public void onTick(long millisUntilFinished) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(String.format(getString(R.string.auto_save_warning_confirm), millisUntilFinished / 1000));
                }
                public void onFinish() {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(getString(R.string.ok));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }.start();
        });
        dialog.show();
    }

    private void showWordWrapRestartDialog() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.word_wrap_restart_required))
                .setPositiveButton(getString(R.string.restart_now), (d, w) -> {
                    setResult(RESULT_OK, new Intent().putExtra("restart_editor", true));
                    finish();
                })
                .setNegativeButton(getString(R.string.restart_later), null)
                .show();
    }
}
