package com.Blabby.Co.UniHub.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Localization {

    private static final String PREFS_NAME = "localization";
    private static final String KEY_LANG = "language";
    private static final String DEFAULT_LANG = "zh_cn";

    private static Localization instance;
    private final Map<String, String> strings = new HashMap<>();
    private String currentLang;
    private final Context context;

    private Localization(Context context) {
        this.context = context.getApplicationContext();
        currentLang = getSavedLanguage();
        loadLanguage(currentLang);
    }

    public static synchronized Localization getInstance(Context context) {
        if (instance == null) {
            instance = new Localization(context);
        }
        return instance;
    }

    public static void reinit(Context context) {
        instance = new Localization(context);
    }

    public String get(String key) {
        String val = strings.get(key);
        return val != null ? val : key;
    }

    public String get(String key, Object... args) {
        String val = strings.get(key);
        if (val == null) return key;
        return String.format(val, args);
    }

    public String getCurrentLanguage() {
        return currentLang;
    }

    public String getSavedLanguage() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, DEFAULT_LANG);
    }

    public void setLanguage(String langCode) {
        currentLang = langCode;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean saved = prefs.edit().putString(KEY_LANG, langCode).commit();
        if (saved) {
            loadLanguage(langCode);
            Resources res = context.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration config = res.getConfiguration();
            String[] parts = langCode.split("_");
            if (parts.length == 2) {
                config.setLocale(new Locale(parts[0], parts[1]));
            } else {
                config.setLocale(new Locale(langCode));
            }
            res.updateConfiguration(config, dm);
        }
    }

    private void loadLanguage(String langCode) {
        strings.clear();
        try {
            String fileName = "languages/" + langCode + ".yaml";
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                strings.put(key, value);
            }
            reader.close();
            is.close();
        } catch (Exception e) {
            strings.put("error", "Failed to load language: " + langCode);
        }
    }

    public static String getDisplayName(String langCode) {
        switch (langCode) {
            case "zh_cn": return "简体中文 (中华人民共和国)";
            case "zh_tw": return "繁體中文 (台灣)";
            case "zh_ac": return "文言 (華夏)";
            case "en": return "English (United States)";
            case "ja": return "日本語 (日本国)";
            case "ko": return "한국어 (대한민국)";
            case "ms": return "Bahasa Melayu (Malaysia)";
            case "my": return "မြန်မာ (မြန်မာနိုင်ငံ)";
            case "de": return "Deutsch (Deutschland)";
            case "fr": return "Français (France)";
            case "ru": return "Русский (Россия)";
            case "uk": return "Українська (Україна)";
            case "da": return "Dansk (Danmark)";
            case "cs": return "Čeština (Česko)";
            case "el": return "Ελληνικά (Ελλάδα)";
            default: return langCode;
        }
    }

    public static String[][] getSupportedLanguages() {
        return new String[][] {
                {"zh_cn", "简体中文 (中华人民共和国)"},
                {"zh_tw", "繁體中文 (台灣)"},
                {"zh_ac", "文言 (華夏)"},
                {"en", "English (United States)"},
                {"ja", "日本語 (日本国)"},
                {"ko", "한국어 (대한민국)"},
                {"ms", "Bahasa Melayu (Malaysia)"},
                {"my", "မြန်မာ (မြန်မာနိုင်ငံ)"},
                {"de", "Deutsch (Deutschland)"},
                {"fr", "Français (France)"},
                {"ru", "Русский (Россия)"},
                {"uk", "Українська (Україна)"},
                {"da", "Dansk (Danmark)"},
                {"cs", "Čeština (Česko)"},
                {"el", "Ελληνικά (Ελλάδα)"}
        };
    }
}