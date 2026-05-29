package com.Blabby.Co.UniHub;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.Blabby.Co.UniHub.util.Localization;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class AiChatActivity extends AppCompatActivity {

    private static final String PREFS_AI = "ai_config";

    private Localization l;
    private LinearLayout containerMessages;
    private ScrollView scrollMessages;
    private EditText etInput, etBaseUrl, etApiKey, etApiVersion, etModel;
    private Spinner spinnerProvider;
    private LinearLayout configPanel;
    private Button btnSend, btnSaveConfig;
    private View btnBack, btnConfigToggle, btnClear;

    private final String[] providers = {"OpenAI", "Anthropic", "Zhipu", "MiniMax"};
    private final String[] defaultBaseUrls = {
            "https://api.openai.com/v1",
            "https://api.anthropic.com",
            "https://open.bigmodel.cn/api/paas/v4",
            "https://api.minimax.chat/v1"
    };
    private final String[] defaultModels = {
            "gpt-4o",
            "claude-sonnet-4-20250514",
            "glm-4-plus",
            "MiniMax-Text-01"
    };

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        l = Localization.getInstance(this);
        client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        initViews();
        setupSpinner();
        loadConfig(0);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnConfigToggle = findViewById(R.id.btn_config_toggle);
        btnClear = findViewById(R.id.btn_clear);
        configPanel = findViewById(R.id.config_panel);
        spinnerProvider = findViewById(R.id.spinner_provider);
        etBaseUrl = findViewById(R.id.et_base_url);
        etApiKey = findViewById(R.id.et_api_key);
        etApiVersion = findViewById(R.id.et_api_version);
        etModel = findViewById(R.id.et_model);
        btnSaveConfig = findViewById(R.id.btn_save_config);
        containerMessages = findViewById(R.id.container_messages);
        scrollMessages = findViewById(R.id.scroll_messages);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);

        ((TextView) findViewById(R.id.tv_ai_title)).setText(l.get("ai_title"));
        etInput.setHint(l.get("ai_input_hint"));
        btnSend.setText(l.get("ai_send"));
        etBaseUrl.setHint(l.get("ai_base_url"));
        etApiKey.setHint(l.get("ai_api_key"));
        etApiVersion.setHint(l.get("ai_api_version"));
        etModel.setHint(l.get("ai_model_hint"));
        btnSaveConfig.setText(l.get("ai_save_config"));

        btnBack.setOnClickListener(v -> finish());

        btnConfigToggle.setOnClickListener(v -> {
            configPanel.setVisibility(configPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        btnClear.setOnClickListener(v -> {
            containerMessages.removeAllViews();
        });

        btnSaveConfig.setOnClickListener(v -> saveConfig());

        btnSend.setOnClickListener(v -> sendMessage());

        findViewById(R.id.config_panel).setVisibility(View.GONE);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvider.setAdapter(adapter);
        spinnerProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadConfig(position);
                boolean isAnthropic = providers[position].equals("Anthropic");
                etApiVersion.setVisibility(isAnthropic ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadConfig(int providerIndex) {
        SharedPreferences prefs = getSharedPreferences(PREFS_AI, MODE_PRIVATE);
        String prefix = providers[providerIndex];
        etBaseUrl.setText(prefs.getString(prefix + "_base_url", defaultBaseUrls[providerIndex]));
        etApiKey.setText(prefs.getString(prefix + "_api_key", ""));
        etApiVersion.setText(prefs.getString(prefix + "_api_version", "2023-06-01"));
        etModel.setText(prefs.getString(prefix + "_model", defaultModels[providerIndex]));
        boolean isAnthropic = providers[providerIndex].equals("Anthropic");
        etApiVersion.setVisibility(isAnthropic ? View.VISIBLE : View.GONE);
    }

    private void saveConfig() {
        int pos = spinnerProvider.getSelectedItemPosition();
        SharedPreferences prefs = getSharedPreferences(PREFS_AI, MODE_PRIVATE);
        String prefix = providers[pos];
        prefs.edit()
                .putString(prefix + "_base_url", etBaseUrl.getText().toString().trim())
                .putString(prefix + "_api_key", etApiKey.getText().toString().trim())
                .putString(prefix + "_api_version", etApiVersion.getText().toString().trim())
                .putString(prefix + "_model", etModel.getText().toString().trim())
                .apply();
        Toast.makeText(this, l.get("settings") + " " + l.get("save"), Toast.LENGTH_SHORT).show();
        configPanel.setVisibility(View.GONE);
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage(text, true);
        etInput.setText("");

        int pos = spinnerProvider.getSelectedItemPosition();
        String baseUrl = etBaseUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String apiVersion = etApiVersion.getText().toString().trim();
        String model = etModel.getText().toString().trim();

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            Toast.makeText(this, l.get("ai_error", "Base URL / API Key " + l.get("name_empty")), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        btnSend.setText(l.get("ai_connecting"));

        if (providers[pos].equals("Anthropic")) {
            callAnthropic(baseUrl, apiKey, apiVersion, model, text);
        } else {
            callOpenAI(baseUrl, apiKey, model, text);
        }
    }

    private void addMessage(String content, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setTextColor(getResources().getColor(R.color.on_surface));
        tv.setTextSize(15);
        tv.setPadding(12, 10, 12, 10);
        tv.setBackgroundResource(R.drawable.bg_card);
        tv.setMaxWidth(dp(280));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 4, 0, 4);
        lp.gravity = isUser ? Gravity.END : Gravity.START;
        tv.setLayoutParams(lp);

        containerMessages.addView(tv);
        scrollMessages.post(() -> scrollMessages.fullScroll(View.FOCUS_DOWN));
    }

    private void callOpenAI(String baseUrl, String apiKey, String model, String userMessage) {
        String url = baseUrl + "/chat/completions";

        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("stream", false);

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);
            body.put("messages", messages);

            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        btnSend.setText(l.get("ai_send"));
                        addMessage(l.get("ai_error", e.getMessage()), false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        btnSend.setText(l.get("ai_send"));
                        try {
                            if (response.isSuccessful()) {
                                JSONObject json = new JSONObject(respBody);
                                String reply = json.getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content");
                                addMessage(reply, false);
                            } else {
                                addMessage(l.get("ai_error", respBody), false);
                            }
                        } catch (Exception e) {
                            addMessage(l.get("ai_error", e.getMessage()), false);
                        }
                    });
                }
            });
        } catch (Exception e) {
            btnSend.setEnabled(true);
            btnSend.setText(l.get("ai_send"));
            addMessage(l.get("ai_error", e.getMessage()), false);
        }
    }

    private void callAnthropic(String baseUrl, String apiKey, String apiVersion, String model, String userMessage) {
        String url = baseUrl + "/v1/messages";

        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("max_tokens", 4096);

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);
            body.put("messages", messages);

            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", apiVersion)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        btnSend.setText(l.get("ai_send"));
                        addMessage(l.get("ai_error", e.getMessage()), false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        btnSend.setText(l.get("ai_send"));
                        try {
                            if (response.isSuccessful()) {
                                JSONObject json = new JSONObject(respBody);
                                JSONArray content = json.getJSONArray("content");
                                StringBuilder reply = new StringBuilder();
                                for (int i = 0; i < content.length(); i++) {
                                    JSONObject block = content.getJSONObject(i);
                                    if (block.getString("type").equals("text")) {
                                        reply.append(block.getString("text"));
                                    }
                                }
                                addMessage(reply.toString(), false);
                            } else {
                                addMessage(l.get("ai_error", respBody), false);
                            }
                        } catch (Exception e) {
                            addMessage(l.get("ai_error", e.getMessage()), false);
                        }
                    });
                }
            });
        } catch (Exception e) {
            btnSend.setEnabled(true);
            btnSend.setText(l.get("ai_send"));
            addMessage(l.get("ai_error", e.getMessage()), false);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
