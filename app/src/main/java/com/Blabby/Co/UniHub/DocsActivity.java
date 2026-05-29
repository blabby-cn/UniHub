package com.Blabby.Co.UniHub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.Blabby.Co.UniHub.util.Localization;

public class DocsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvTitle;
    private ImageButton btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docs);

        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        listView = findViewById(R.id.list_docs);

        Localization l = Localization.getInstance(this);
        tvTitle.setText(l.get("documents"));

        btnBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        final String[] docTypes = {"term", "privacy", "log", "assets"};
        final String[] docNames = {
                l.get("user_agreement"),
                l.get("privacy_policy"),
                l.get("changelog"),
                l.get("third_party_assets")
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, docNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(DocsActivity.this, DocumentActivity.class);
            intent.putExtra(DocumentActivity.EXTRA_TYPE, docTypes[position]);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}