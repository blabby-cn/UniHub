package com.Blabby.Co.UniHub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.Blabby.Co.UniHub.util.Localization;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Localization l = Localization.getInstance(this);
        ((TextView) findViewById(R.id.tv_settings_title)).setText(l.get("settings"));

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_menu", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layout_language).setOnClickListener(v -> showLanguageDialog());
        updateLanguageDisplay();
    }

    private void updateLanguageDisplay() {
        Localization l = Localization.getInstance(this);
        String currentCode = l.getCurrentLanguage();
        String displayName = Localization.getDisplayName(currentCode);
        TextView tvCurrentLanguage = findViewById(R.id.tv_current_language);
        if (tvCurrentLanguage != null) {
            tvCurrentLanguage.setText(displayName);
        }
    }

    private void showLanguageDialog() {
        Localization l = Localization.getInstance(this);
        String[][] langs = Localization.getSupportedLanguages();
        String[] names = new String[langs.length];
        int currentIndex = 0;
        String current = l.getCurrentLanguage();
        for (int i = 0; i < langs.length; i++) {
            names[i] = langs[i][1];
            if (langs[i][0].equals(current)) currentIndex = i;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(l.get("language"));
        builder.setSingleChoiceItems(names, currentIndex, (dialog, which) -> {
            String selectedCode = langs[which][0];
            if (selectedCode.equals(current)) {
                dialog.dismiss();
                return;
            }

            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle(l.get("language_restart_title"))
                    .setMessage(l.get("language_restart_message"))
                    .setPositiveButton(l.get("restart_now"), (d, w) -> {
                        l.setLanguage(selectedCode);
                        restartApp();
                    })
                    .setNegativeButton(l.get("cancel"), (d, w) -> {
                        dialog.dismiss();
                    })
                    .show();
        });
        builder.setNegativeButton(l.get("cancel"), null);
        builder.show();
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }
}