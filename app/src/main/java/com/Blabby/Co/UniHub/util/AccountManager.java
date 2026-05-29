package com.Blabby.Co.UniHub.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccountManager {
    private static final String PREFS_NAME = "blabby_account";
    private static final String KEY_COOKIE = "auth_cookie";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_AVATAR = "avatar"; 

    private static final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface LoginCallback {
        void onSuccess(String username, String email, String avatar);
        void onFailure(String error);
    }

    public interface MeCallback {
        void onResult(String username, String email, String avatar);
    }

    public static void login(Context context, String login, String password, LoginCallback callback) {
        JSONObject json = new JSONObject();
        try {
            json.put("login", login);
            json.put("password", password);
        } catch (Exception e) { }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://login.blabby.top/api/login")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure("网络错误: " + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("登录失败，状态码: " + response.code()));
                    return;
                }
                String setCookie = response.header("Set-Cookie");
                if (setCookie == null || setCookie.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("未收到认证 Cookie"));
                    return;
                }
                
                String authToken = setCookie.split(";")[0];
                
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_COOKIE, authToken).apply();

                
                fetchMe(context, authToken, callback);
            }
        });
    }

    private static void fetchMe(Context context, String cookie, LoginCallback callback) {
        Request request = new Request.Builder()
                .url("https://login.blabby.top/api/me")
                .header("Cookie", cookie)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure("获取用户信息失败: " + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("验证登录失败，状态码: " + response.code()));
                    return;
                }
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("loggedIn")) {
                        String username = json.optString("username", "");
                        String email = json.optString("email", "");
                        String avatar = json.optString("avatar", "");
                        
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString(KEY_USERNAME, username)
                                .putString(KEY_EMAIL, email)
                                .putString(KEY_AVATAR, avatar)
                                .apply();
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onSuccess(username, email, avatar));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onFailure("登录验证未通过"));
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("解析信息出错"));
                }
            }
        });
    }

    public static void getSavedMe(Context context, MeCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String username = prefs.getString(KEY_USERNAME, "");
        String email = prefs.getString(KEY_EMAIL, "");
        String avatar = prefs.getString(KEY_AVATAR, "");
        if (!username.isEmpty()) {
            callback.onResult(username, email, avatar);
        } else {
            callback.onResult(null, null, null);
        }
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static String getSavedCookie(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_COOKIE, "");
    }
}