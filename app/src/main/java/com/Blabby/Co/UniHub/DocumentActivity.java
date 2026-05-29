package com.Blabby.Co.UniHub;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.Blabby.Co.UniHub.util.Localization;

public class DocumentActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "doc_type";

    private WebView webView;
    private TextView tvTitle;
    private ImageButton btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);

        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        webView = findViewById(R.id.web_view);

        btnBack.setOnClickListener(v -> finish());

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        if ("term".equals(type)) {
            tvTitle.setText(Localization.getInstance(this).get("user_agreement"));
            webView.loadUrl("file:///android_asset/term.html");
        } else if ("privacy".equals(type)) {
            tvTitle.setText(Localization.getInstance(this).get("privacy_policy"));
            webView.loadUrl("file:///android_asset/privacy.html");
        } else if ("log".equals(type)) {
            tvTitle.setText(Localization.getInstance(this).get("changelog"));
            webView.loadUrl("file:///android_asset/log.html");
        } else if ("assets".equals(type)) {
            tvTitle.setText(Localization.getInstance(this).get("third_party_assets"));
            webView.loadUrl("file:///android_asset/assets.html");
        }
    }
}