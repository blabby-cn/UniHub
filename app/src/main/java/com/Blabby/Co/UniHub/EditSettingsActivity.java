package com.Blabby.Co.UniHub;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class EditSettingsActivity extends AppCompatActivity {

    private SwitchCompat switchAutoSave;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_settings);

        prefs = getSharedPreferences("editor_settings", MODE_PRIVATE);

        ImageButton btnBack = findViewById(R.id.btn_back);
        switchAutoSave = findViewById(R.id.switch_auto_save);
        TextView btnHelp = findViewById(R.id.btn_help);

        switchAutoSave.setChecked(prefs.getBoolean("auto_save", true));

        btnBack.setOnClickListener(v -> finish());

        switchAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                prefs.edit().putBoolean("auto_save", true).apply();
            } else {
                showWarningDialog();
            }
        });

        btnHelp.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.auto_save_help_title))
                    .setMessage(getString(R.string.auto_save_help_msg))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
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
}