package com.Blabby.Co.UniHub.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import java.util.Locale;

public class Localization {

    private static final String PREFS_NAME = "localization";
    private static final String KEY_LANG = "language";
    private static final String DEFAULT_LANG = "zh_cn";

    private static Localization instance;
    private Context context;
    private String currentLang;

    private Localization(Context context) {
        this.context = context.getApplicationContext();
        this.currentLang = getSavedLanguage();
        applyLanguage(currentLang);
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
        try {
            int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            } else {
                return key;
            }
        } catch (Exception e) {
            return key;
        }
    }

    public String get(String key, Object... args) {
        String format = get(key);
        if (format.equals(key)) {
            return key;
        }
        return String.format(format, args);
    }

    public String getCurrentLanguage() {
        return currentLang;
    }

    public String getSavedLanguage() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, DEFAULT_LANG);
    }

    public void setLanguage(String langCode) {
        if (langCode.equals(currentLang)) return;
        currentLang = langCode;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, langCode).apply();
        applyLanguage(langCode);
    }

    private void applyLanguage(String langCode) {
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(getLocaleFromCode(langCode));
        config.setLocales(new LocaleList(getLocaleFromCode(langCode)));
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private Locale getLocaleFromCode(String langCode) {
        switch (langCode) {
            case "zh_cn": return Locale.SIMPLIFIED_CHINESE;
            case "zh_tw": return Locale.TRADITIONAL_CHINESE;
            case "en": return Locale.ENGLISH;
            case "ms": return new Locale("ms");
            default: return Locale.SIMPLIFIED_CHINESE;
        }
    }

    public static String getDisplayName(String langCode) {
        switch (langCode) {
            case "zh_cn": return "简体中文";
            case "zh_tw": return "繁體中文";
            case "en": return "English";
            case "ms": return "Bahasa Melayu";
            default: return langCode;
        }
    }

    public static String[][] getSupportedLanguages() {
        return new String[][] {
                {"zh_cn", "简体中文"},
                {"zh_tw", "繁體中文"},
                {"en", "English"},
                {"ms", "Bahasa Melayu"}
        };
    }
}