package com.Blabby.Co.UniHub.ui.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.Blabby.Co.UniHub.DownloadActivity;
import com.Blabby.Co.UniHub.R;
import com.Blabby.Co.UniHub.download.DownloadManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WebBrowserFragment extends Fragment {
    private WebView webView;
    private ProgressBar progressBar;
    private List<String> tabs = new ArrayList<>();
    private int currentTab = -1;
    private View bottomHintView;
    private Runnable hideHintRunnable;

    public interface OnUrlChangeListener { void onUrlChanged(String url); }
    private OnUrlChangeListener urlChangeListener;
    public void setOnUrlChangeListener(OnUrlChangeListener l) { this.urlChangeListener = l; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_browser, container, false);
        webView = view.findViewById(R.id.web_view);
        progressBar = view.findViewById(R.id.progress_bar);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                if (urlChangeListener != null) urlChangeListener.onUrlChanged(url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
            showDownloadOptionsDialog(url, fileName);
        });

        initBottomHint(view);

        return view;
    }

    private void initBottomHint(View rootView) {
        LinearLayout container = rootView.findViewById(android.R.id.content);
        if (container == null) {
            container = (LinearLayout) rootView;
        }
        bottomHintView = LayoutInflater.from(getContext()).inflate(R.layout.view_bottom_hint, container, false);
        bottomHintView.setVisibility(View.GONE);
        container.addView(bottomHintView);
        hideHintRunnable = () -> bottomHintView.setVisibility(View.GONE);
        bottomHintView.setOnClickListener(v -> {
            startActivity(new android.content.Intent(getContext(), DownloadActivity.class));
            bottomHintView.setVisibility(View.GONE);
        });
    }

    private void showDownloadOptionsDialog(String url, String defaultFileName) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_download_options, null);
        dialog.setContentView(content);

        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) content.getParent());
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);

        EditText etPath = content.findViewById(R.id.et_save_path);
        EditText etFileName = content.findViewById(R.id.et_file_name);
        Button btnCancel = content.findViewById(R.id.btn_cancel);
        Button btnConfirm = content.findViewById(R.id.btn_confirm);

        String defaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        etPath.setText(defaultPath);
        etFileName.setText(defaultFileName);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String savePath = etPath.getText().toString().trim();
            String fileName = etFileName.getText().toString().trim();
            if (savePath.isEmpty()) {
                savePath = defaultPath;
            }
            if (fileName.isEmpty()) {
                fileName = defaultFileName;
            }
            File targetFile = new File(savePath, fileName);
            String finalSavePath = targetFile.getAbsolutePath();
            DownloadManager.getInstance().addTask(url, fileName, finalSavePath);
            dialog.dismiss();
            showBottomHint();
        });

        dialog.show();
    }

    private void showBottomHint() {
        if (bottomHintView == null) return;
        bottomHintView.removeCallbacks(hideHintRunnable);
        bottomHintView.setVisibility(View.VISIBLE);
        bottomHintView.postDelayed(hideHintRunnable, 3000);
    }

    public void loadUrl(String url) {
        if (webView == null) return;
        if (!url.startsWith("http")) url = "https://" + url;
        webView.loadUrl(url);
        if (currentTab == -1 || !tabs.contains(url)) {
            tabs.add(url);
            currentTab = tabs.size() - 1;
        } else {
            currentTab = tabs.indexOf(url);
        }
    }

    public void newTab(String url) {
        if (webView == null) return;
        if (!url.startsWith("http")) url = "https://" + url;
        webView.loadUrl(url);
        tabs.add(url);
        currentTab = tabs.size() - 1;
    }

    public void showTabList() {
        if (tabs.isEmpty()) return;
        String[] tabArr = tabs.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("标签页")
                .setItems(tabArr, (d, which) -> {
                    webView.loadUrl(tabArr[which]);
                    currentTab = which;
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    public boolean canGoBack() { return webView != null && webView.canGoBack(); }
    public boolean canGoForward() { return webView != null && webView.canGoForward(); }
    public void goBack() { if (webView != null && webView.canGoBack()) webView.goBack(); }
    public void goForward() { if (webView != null && webView.canGoForward()) webView.goForward(); }
    public void refresh() { if (webView != null) webView.reload(); }

    public String[] getTabs() {
        return tabs.toArray(new String[0]);
    }

    public void switchToTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            currentTab = index;
            webView.loadUrl(tabs.get(index));
        }
    }
}