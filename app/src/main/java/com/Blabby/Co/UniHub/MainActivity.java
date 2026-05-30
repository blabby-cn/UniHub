package com.Blabby.Co.UniHub;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.Blabby.Co.UniHub.network.FtpClient;
import com.Blabby.Co.UniHub.network.SftpClient;
import com.Blabby.Co.UniHub.data.model.BookmarkItem;
import com.Blabby.Co.UniHub.ui.dialogs.LoginDialog;
import com.Blabby.Co.UniHub.ui.fragments.FileBrowserFragment;
import com.Blabby.Co.UniHub.ui.fragments.RemoteFileBrowserFragment;
import com.Blabby.Co.UniHub.ui.fragments.WebBrowserFragment;
import com.Blabby.Co.UniHub.util.AccountManager;
import com.Blabby.Co.UniHub.util.BookmarkManager;
import com.Blabby.Co.UniHub.util.Localization;
import com.Blabby.Co.UniHub.util.PathParser;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements FileBrowserFragment.StatsListener {

    private static final int REQUEST_DOCS = 2001;

    private MaterialToolbar toolbar;
    private TextView addressPath, addressStats;
    private View sidebarOverlay;
    private ImageButton btnCloseSidebar;

    private FileBrowserFragment fileBrowserFragment;
    private RemoteFileBrowserFragment remoteFileBrowserFragment;
    private WebBrowserFragment webBrowserFragment;
    private Fragment currentFragment;
    private int currentMode = 0;

    private ImageButton btnBack, btnForward, btnNew, btnRefresh, btnUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        addressPath = findViewById(R.id.address_path);
        addressStats = findViewById(R.id.address_stats);
        sidebarOverlay = findViewById(R.id.sidebar_overlay);
        btnCloseSidebar = findViewById(R.id.btn_close_sidebar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        Localization.getInstance(this);

        toolbar.setNavigationIcon(R.drawable.ic_menu_24);
        toolbar.setNavigationOnClickListener(v -> openSidebar());

        applySidebarTexts();

        View sidebarHeader = sidebarOverlay.findViewById(R.id.sidebar_header);
        ImageView ivAvatar = sidebarHeader.findViewById(R.id.iv_sidebar_avatar);
        TextView tvUsername = sidebarHeader.findViewById(R.id.tv_sidebar_username);
        TextView tvEmail = sidebarHeader.findViewById(R.id.tv_sidebar_email);
        sidebarHeader.setOnClickListener(v -> showLoginDialog());

        AccountManager.getSavedMe(this, (username, email, avatar) -> {
            if (username != null) {
                tvUsername.setText(username);
                tvEmail.setText(email);
                if (avatar != null && !avatar.isEmpty()) {
                    Glide.with(this).load(avatar).circleCrop().into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_account_circle);
                }
            } else {
                tvUsername.setText(Localization.getInstance(this).get("not_logged_in"));
                tvEmail.setText(Localization.getInstance(this).get("click_to_login"));
                ivAvatar.setImageResource(R.drawable.ic_account_circle);
            }
        });

        sidebarOverlay.findViewById(R.id.menu_home).setOnClickListener(v -> {
            closeSidebar();
            String home = Environment.getExternalStorageDirectory().getAbsolutePath();
            switchToLocalMode(home);
        });
        sidebarOverlay.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            startActivityForResult(new Intent(this, SettingsActivity.class), 1001);
        });
        sidebarOverlay.findViewById(R.id.menu_docs).setOnClickListener(v -> {
            closeSidebar();
            Intent intent = new Intent(this, DocsActivity.class);
            startActivityForResult(intent, REQUEST_DOCS);
        });
        sidebarOverlay.findViewById(R.id.menu_download).setOnClickListener(v -> {
            closeSidebar();
            startActivity(new Intent(this, DownloadActivity.class));
        });
        sidebarOverlay.findViewById(R.id.menu_bookmarks).setOnClickListener(v -> {
            closeSidebar();
            showBookmarkDialog();
        });
        sidebarOverlay.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            closeSidebar();
            AccountManager.logout(this);
            tvUsername.setText(Localization.getInstance(this).get("not_logged_in"));
            tvEmail.setText(Localization.getInstance(this).get("click_to_login"));
            ivAvatar.setImageResource(R.drawable.ic_account_circle);
            Toast.makeText(this, Localization.getInstance(this).get("logged_out"), Toast.LENGTH_SHORT).show();
        });

        btnCloseSidebar.setOnClickListener(v -> closeSidebar());

        fileBrowserFragment = new FileBrowserFragment();
        fileBrowserFragment.setStatsListener(this);
        remoteFileBrowserFragment = new RemoteFileBrowserFragment();
        webBrowserFragment = new WebBrowserFragment();

        findViewById(R.id.address_container).setOnClickListener(v -> showAddressDialog());

        showFragment(fileBrowserFragment, 0);
        fileBrowserFragment.setOnPathChangeListener(path -> addressPath.setText(path));

        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnNew = findViewById(R.id.btn_new);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnUp = findViewById(R.id.btn_up);

        btnBack.setOnClickListener(v -> handleAction("back"));
        btnForward.setOnClickListener(v -> handleAction("forward"));
        btnNew.setOnClickListener(v -> handleAction("new"));
        btnRefresh.setOnClickListener(v -> handleAction("refresh"));
        btnUp.setOnClickListener(v -> handleAction("up"));

        if (getIntent().getBooleanExtra("open_menu", false)) {
            openSidebar();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DOCS && resultCode == RESULT_OK) {
            openSidebar();
        } else if (requestCode >= 1001 && requestCode <= 1005) {
            openSidebar();
        }
    }

    private void applySidebarTexts() {
        Localization l = Localization.getInstance(this);
        ((TextView) sidebarOverlay.findViewById(R.id.tv_sidebar_title)).setText(l.get("menu"));
        ((TextView) sidebarOverlay.findViewById(R.id.menu_home)).setText(l.get("home"));
        ((TextView) sidebarOverlay.findViewById(R.id.menu_settings)).setText(l.get("settings"));
        ((TextView) sidebarOverlay.findViewById(R.id.menu_docs)).setText(l.get("documents"));
        ((TextView) sidebarOverlay.findViewById(R.id.menu_download)).setText(l.get("download_file"));
        ((TextView) sidebarOverlay.findViewById(R.id.menu_bookmarks)).setText(l.get("bookmarks"));
        ((TextView) sidebarOverlay.findViewById(R.id.menu_logout)).setText(l.get("logout"));
    }

    private void openSidebar() {
        sidebarOverlay.setVisibility(View.VISIBLE);
        Animation slideIn = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f
        );
        slideIn.setDuration(200);
        sidebarOverlay.startAnimation(slideIn);
    }

    private void closeSidebar() {
        Animation slideOut = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f
        );
        slideOut.setDuration(200);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                sidebarOverlay.setVisibility(View.GONE);
            }
        });
        sidebarOverlay.startAnimation(slideOut);
    }

    @Override
    public void onStatsChanged(int folders, int files, long freeSpace, long totalSpace) {
        String free = formatSize(freeSpace);
        String total = formatSize(totalSpace);
        Localization l = Localization.getInstance(this);
        String stats = l.get("folder_colon", folders) + " " + l.get("file_colon", files) + " " + l.get("storage_colon", free, total);
        addressStats.setText(stats);
        addressStats.setVisibility(View.VISIBLE);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", size / Math.pow(1024, exp), pre);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            if (currentFragment == fileBrowserFragment) fileBrowserFragment.refreshActivePanel();
            else if (currentFragment == remoteFileBrowserFragment) remoteFileBrowserFragment.refreshActivePanel();
            else if (currentFragment == webBrowserFragment) webBrowserFragment.refresh();
        } else if (id == R.id.menu_search) {
            Toast.makeText(this, Localization.getInstance(this).get("search_not_impl"), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_select_all) {
            Toast.makeText(this, Localization.getInstance(this).get("select_all_not_impl"), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_filter) {
            Toast.makeText(this, Localization.getInstance(this).get("filter_not_impl"), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_sort) {
            Toast.makeText(this, Localization.getInstance(this).get("sort_not_impl"), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_show_hidden) {
            item.setChecked(!item.isChecked());
            Toast.makeText(this, Localization.getInstance(this).get("show_hidden_files", String.valueOf(item.isChecked())), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_add_bookmark) {
            if (currentMode == 2) {
                addCurrentAsBookmark();
            } else {
                Toast.makeText(this, Localization.getInstance(this).get("web_only_bookmark"), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.menu_set_home) {
            Toast.makeText(this, Localization.getInstance(this).get("set_home_not_impl"), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_swap_window) {
            Toast.makeText(this, Localization.getInstance(this).get("swap_window_not_impl"), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 1001);
        } else if (id == R.id.menu_exit) {
            finish();
        }
        return true;
    }

    private void handleAction(String action) {
        if (currentFragment == fileBrowserFragment) {
            if (action.equals("back")) fileBrowserFragment.goBack();
            else if (action.equals("forward")) fileBrowserFragment.goForward();
            else if (action.equals("new")) fileBrowserFragment.showCreateDialog();
            else if (action.equals("refresh")) fileBrowserFragment.refreshActivePanel();
            else if (action.equals("up")) fileBrowserFragment.goUpActive();
        } else if (currentFragment == remoteFileBrowserFragment) {
            if (action.equals("back")) remoteFileBrowserFragment.goBack();
            else if (action.equals("forward")) remoteFileBrowserFragment.goForward();
            else if (action.equals("new"))
                Toast.makeText(this, Localization.getInstance(this).get("remote_no_create"), Toast.LENGTH_SHORT).show();
            else if (action.equals("refresh")) remoteFileBrowserFragment.refreshActivePanel();
            else if (action.equals("up")) remoteFileBrowserFragment.goUpActive();
        } else if (currentFragment == webBrowserFragment) {
            if (action.equals("back")) webBrowserFragment.goBack();
            else if (action.equals("forward")) webBrowserFragment.goForward();
            else if (action.equals("new")) {
                webBrowserFragment.newTab("about:blank");
                addressPath.setText("about:blank");
            } else if (action.equals("refresh")) webBrowserFragment.refresh();
            else if (action.equals("up")) webBrowserFragment.showTabList();
        }
    }

    private void switchToLocalMode(String path) {
        if (currentFragment != fileBrowserFragment) {
            showFragment(fileBrowserFragment, 0);
            fileBrowserFragment.setOnPathChangeListener(p -> addressPath.setText(p));
        }
        fileBrowserFragment.loadFiles(path);
    }

    private void switchToRemoteMode(String prefix, String path) {
        showFragment(remoteFileBrowserFragment, 1);
        remoteFileBrowserFragment.setOnPathChangeListener(p -> addressPath.setText(prefix + p));
        addressStats.setVisibility(View.GONE);
    }

    private void switchToWebMode(String url) {
        if (currentFragment != webBrowserFragment) {
            showFragment(webBrowserFragment, 2);
            webBrowserFragment.setOnUrlChangeListener(urlText -> addressPath.setText(urlText));
        }
        addressStats.setVisibility(View.GONE);
        webBrowserFragment.loadUrl(url);
    }

    private void showFragment(Fragment fragment, int mode) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
        getSupportFragmentManager().executePendingTransactions();
        currentFragment = fragment;
        currentMode = mode;
    }

    private void showAddressDialog() {
        Localization l = Localization.getInstance(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(l.get("enter_path_or_url"));
        final EditText input = new EditText(this);
        input.setText(addressPath.getText().toString());
        input.setTextColor(getResources().getColor(R.color.on_surface));
        builder.setView(input);
        builder.setPositiveButton(l.get("confirm"), (d, w) -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) handleAddressInput(text);
        });
        builder.setNegativeButton(l.get("cancel"), null);
        builder.show();
    }

    private void handleAddressInput(String input) {
        if (!input.contains("://") && !input.startsWith("/")) {
            input = "https://" + input;
        }
        int type = PathParser.parse(input);
        if (type == PathParser.TYPE_LOCAL) {
            switchToLocalMode(input);
        } else if (type == PathParser.TYPE_FTP || type == PathParser.TYPE_SFTP) {
            String host = PathParser.getHost(input);
            int defaultPort = type == PathParser.TYPE_FTP ? 21 : 22;
            int port = PathParser.getPort(input, defaultPort);
            new LoginDialog(host, port, (h, p, user, pass) -> {
                new Thread(() -> {
                    try {
                        if (type == PathParser.TYPE_FTP) {
                            FtpClient ftp = new FtpClient(h, p, user, pass);
                            if (ftp.connect()) {
                                runOnUiThread(() -> {
                                    remoteFileBrowserFragment.setConnectionInfo(ftp, "/");
                                    switchToRemoteMode("ftp://" + h, "/");
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, Localization.getInstance(this).get("ftp_failed"), Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            SftpClient sftp = new SftpClient(h, p, user, pass);
                            if (sftp.connect()) {
                                runOnUiThread(() -> {
                                    remoteFileBrowserFragment.setConnectionInfo(sftp, "/");
                                    switchToRemoteMode("sftp://" + h, "/");
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, Localization.getInstance(this).get("sftp_failed"), Toast.LENGTH_SHORT).show());
                            }
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, Localization.getInstance(this).get("conn_error", e.getMessage()), Toast.LENGTH_LONG).show());
                    }
                }).start();
            }).show(getSupportFragmentManager(), "login");
        } else if (type == PathParser.TYPE_HTTP) {
            switchToWebMode(input);
        }
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_login_account, null);
        EditText etLogin = view.findViewById(R.id.et_login);
        EditText etPassword = view.findViewById(R.id.et_password);
        etLogin.setTextColor(getResources().getColor(R.color.on_surface));
        etPassword.setTextColor(getResources().getColor(R.color.on_surface));
        Localization l = Localization.getInstance(this);
        builder.setView(view)
                .setTitle(l.get("login_blabby"))
                .setPositiveButton(l.get("login"), (d, which) -> {
                    String login = etLogin.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    if (login.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, l.get("empty_account"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AccountManager.login(this, login, password, new AccountManager.LoginCallback() {
                        @Override
                        public void onSuccess(String username, String email, String avatar) {
                            Toast.makeText(MainActivity.this, l.get("login_success"), Toast.LENGTH_SHORT).show();
                            View header = sidebarOverlay.findViewById(R.id.sidebar_header);
                            TextView tvUser = header.findViewById(R.id.tv_sidebar_username);
                            TextView tvMail = header.findViewById(R.id.tv_sidebar_email);
                            ImageView ivAv = header.findViewById(R.id.iv_sidebar_avatar);
                            tvUser.setText(username);
                            tvMail.setText(email);
                            if (avatar != null && !avatar.isEmpty()) {
                                Glide.with(MainActivity.this).load(avatar).circleCrop().into(ivAv);
                            } else {
                                ivAv.setImageResource(R.drawable.ic_account_circle);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private void showDownloadDialog() {
        showDownloadDialog(null);
    }

    private void showDownloadDialog(String presetUrl) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_login, null);
        EditText etHost = view.findViewById(R.id.et_host);
        EditText etPort = view.findViewById(R.id.et_port);
        EditText etUser = view.findViewById(R.id.et_username);
        EditText etPass = view.findViewById(R.id.et_password);
        Localization l = Localization.getInstance(this);
        etHost.setHint(l.get("file_url"));
        etPort.setVisibility(View.GONE);
        etUser.setVisibility(View.GONE);
        etPass.setVisibility(View.GONE);

        if (presetUrl != null) etHost.setText(presetUrl);

        EditText etPath = new EditText(this);
        etPath.setTextColor(getResources().getColor(R.color.on_surface));
        etPath.setHint(l.get("output_path"));
        String defaultPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        etPath.setText(defaultPath + "/");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etPath.setLayoutParams(lp);
        ((LinearLayout) view).addView(etPath);

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(l.get("download_file"))
                .setView(view)
                .setPositiveButton(l.get("download"), (d, w) -> {
                    String url = etHost.getText().toString().trim();
                    String outputPath = etPath.getText().toString().trim();
                    if (url.isEmpty() || outputPath.isEmpty()) {
                        Toast.makeText(this, l.get("empty_url_path"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!hasStorage()) {
                        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                                .setTitle(l.get("download_failed"))
                                .setMessage(l.get("no_perm_or_full"))
                                .setPositiveButton(l.get("ok"), null)
                                .show();
                        return;
                    }
                    startDownload(url, outputPath);
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private boolean hasStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) return false;
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        }
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            long free = stat.getAvailableBytes();
            if (free < 1024 * 1024) return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void startDownload(String url, String outputPath) {
        Toast.makeText(this, Localization.getInstance(this).get("starting_download"), Toast.LENGTH_SHORT).show();
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        Localization.getInstance(MainActivity.this).get("download_failed") + ": " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            Localization.getInstance(MainActivity.this).get("download_failed_code", response.code()), Toast.LENGTH_LONG).show());
                    return;
                }
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                if (fileName.isEmpty()) fileName = "download";
                File outFile = new File(outputPath, fileName);
                try (okhttp3.ResponseBody body = response.body();
                     FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = body.byteStream().read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        Localization.getInstance(MainActivity.this).get("download_complete", outFile.getAbsolutePath()), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void addCurrentAsBookmark() {
        Localization l = Localization.getInstance(this);
        BookmarkManager bm = BookmarkManager.getInstance(this);
        if (!bm.canAdd()) {
            Toast.makeText(this, l.get("bookmark_limit_reached"), Toast.LENGTH_SHORT).show();
            return;
        }
        String currentAddr = addressPath.getText().toString();
        boolean isWeb = currentMode == 2;
        final String name;
        final String type;
        final String pathVal;
        final String browserVal;
        if (isWeb) {
            type = "web";
            pathVal = "nil";
            browserVal = currentAddr;
            name = currentAddr;
        } else {
            type = "path";
            pathVal = currentAddr;
            browserVal = "nil";
            String[] parts = currentAddr.split("/");
            String lastPart = parts.length > 0 ? parts[parts.length - 1] : currentAddr;
            name = lastPart.isEmpty() ? currentAddr : lastPart;
        }
        EditText etName = new EditText(this);
        etName.setText(name);
        etName.setTextColor(getResources().getColor(R.color.on_surface));
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(l.get("add_bookmark"))
                .setView(etName)
                .setPositiveButton(l.get("confirm"), (d, w) -> {
                    String n = etName.getText().toString().trim();
                    int id = bm.add(n.isEmpty() ? name : n, type, pathVal, browserVal);
                    if (id > 0) {
                        Toast.makeText(MainActivity.this, l.get("bookmark_added"), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, l.get("bookmark_limit_reached"), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private void showBookmarkDialog() {
        Localization l = Localization.getInstance(this);
        BookmarkManager bm = BookmarkManager.getInstance(this);
        List<BookmarkItem> items = bm.getAll();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(l.get("bookmarks") + " (" + items.size() + "/" + bm.getMax() + ")");
        if (items.isEmpty()) {
            builder.setMessage(l.get("no_bookmarks"));
            builder.setPositiveButton(l.get("add_bookmark"), (d, w) -> showAddBookmarkDialog());
            builder.setNegativeButton(l.get("cancel"), null);
            builder.show();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bookmarks, null);
        ListView listView = dialogView.findViewById(R.id.bookmark_list);
        TextView countView = dialogView.findViewById(R.id.bookmark_count);
        Button addBtn = dialogView.findViewById(R.id.btn_add_bookmark);
        List<BookmarkItem> finalItems = items;
        ArrayAdapter<BookmarkItem> adapter = new ArrayAdapter<BookmarkItem>(this, R.layout.item_bookmark, R.id.bookmark_text, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                BookmarkItem bi = getItem(position);
                ImageView icon = v.findViewById(R.id.bookmark_icon);
                if (bi.type.equals("web")) {
                    icon.setImageResource(R.drawable.ic_open_in_new);
                    icon.setColorFilter(getResources().getColor(R.color.on_surface));
                } else {
                    icon.setImageResource(R.drawable.ic_folder_24);
                    icon.setColorFilter(getResources().getColor(R.color.on_surface));
                }
                TextView text = v.findViewById(R.id.bookmark_text);
                text.setText("#" + bi.id + " " + bi.name);
                return v;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            showBookmarkActionDialog(finalItems.get(position));
        });
        countView.setText(items.size() + "/" + bm.getMax());
        addBtn.setOnClickListener(v -> showAddBookmarkDialog());
        addBtn.setText(l.get("add_bookmark"));
        builder.setView(dialogView);
        builder.setNegativeButton(l.get("cancel"), null);
        builder.show();
    }

    private void showBookmarkActionDialog(BookmarkItem item) {
        Localization l = Localization.getInstance(this);
        String typeLabel = item.type.equals("web") ? l.get("web_bookmark") : l.get("path_bookmark");
        String info = l.get("name_colon", item.name) + "\n"
                + l.get("bookmark_type") + ": " + typeLabel + "\n"
                + (item.type.equals("web") ? l.get("bookmark_url") + ": " + item.browser
                : l.get("bookmark_path") + ": " + item.path);
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("#" + item.id + " " + item.name)
                .setMessage(info)
                .setPositiveButton(l.get("jump"), (d, w) -> jumpToBookmark(item))
                .setNeutralButton(l.get("edit"), (d, w) -> showEditBookmarkDialog(item))
                .setNegativeButton(l.get("delete"), (d, w) -> {
                    BookmarkManager.getInstance(MainActivity.this).delete(item.id);
                    Toast.makeText(MainActivity.this, l.get("bookmark_deleted"), Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void jumpToBookmark(BookmarkItem item) {
        if (item.type.equals("web")) {
            switchToWebMode(item.browser);
        } else {
            File f = new File(item.path);
            if (f.isFile()) {
                switchToLocalMode(f.getParent());
            } else {
                switchToLocalMode(item.path);
            }
        }
    }

    private void showAddBookmarkDialog() {
        Localization l = Localization.getInstance(this);
        BookmarkManager bm = BookmarkManager.getInstance(this);
        if (!bm.canAdd()) {
            Toast.makeText(this, l.get("bookmark_limit_reached"), Toast.LENGTH_SHORT).show();
            return;
        }
        EditText etName = new EditText(this);
        etName.setHint(l.get("bookmark_name"));
        etName.setTextColor(getResources().getColor(R.color.on_surface));
        RadioGroup radioGroup = new RadioGroup(this);
        RadioButton rbWeb = new RadioButton(this);
        rbWeb.setText(l.get("web_bookmark"));
        rbWeb.setTextColor(getResources().getColor(R.color.on_surface));
        rbWeb.setId(View.generateViewId());
        RadioButton rbPath = new RadioButton(this);
        rbPath.setText(l.get("path_bookmark"));
        rbPath.setTextColor(getResources().getColor(R.color.on_surface));
        rbPath.setId(View.generateViewId());
        radioGroup.addView(rbWeb);
        radioGroup.addView(rbPath);
        radioGroup.check(rbWeb.getId());
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 0, 32, 0);
        layout.addView(etName);
        layout.addView(radioGroup);
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(l.get("add_bookmark"))
                .setView(layout)
                .setPositiveButton(l.get("confirm"), (d, w) -> {
                    String n = etName.getText().toString().trim();
                    if (n.isEmpty()) {
                        Toast.makeText(MainActivity.this, l.get("name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (radioGroup.getCheckedRadioButtonId() == rbWeb.getId()) {
                        showWebUrlInput(n);
                    } else {
                        showPathInput(n);
                    }
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private void showWebUrlInput(String name) {
        Localization l = Localization.getInstance(this);
        EditText input = new EditText(this);
        input.setHint(l.get("bookmark_url"));
        input.setTextColor(getResources().getColor(R.color.on_surface));
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(name)
                .setView(input)
                .setPositiveButton(l.get("confirm"), (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(MainActivity.this, l.get("name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    BookmarkManager bm = BookmarkManager.getInstance(MainActivity.this);
                    int id = bm.add(name, "web", "nil", url);
                    if (id > 0) Toast.makeText(MainActivity.this, l.get("bookmark_added"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private void showPathInput(String name) {
        Localization l = Localization.getInstance(this);
        EditText input = new EditText(this);
        input.setHint(l.get("bookmark_path"));
        input.setTextColor(getResources().getColor(R.color.on_surface));
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(name)
                .setView(input)
                .setPositiveButton(l.get("confirm"), (d, w) -> {
                    String path = input.getText().toString().trim();
                    if (path.isEmpty()) {
                        Toast.makeText(MainActivity.this, l.get("name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    BookmarkManager bm = BookmarkManager.getInstance(MainActivity.this);
                    int id = bm.add(name, "path", path, "nil");
                    if (id > 0) Toast.makeText(MainActivity.this, l.get("bookmark_added"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private void showEditBookmarkDialog(BookmarkItem item) {
        Localization l = Localization.getInstance(this);
        EditText etName = new EditText(this);
        etName.setText(item.name);
        etName.setTextColor(getResources().getColor(R.color.on_surface));
        EditText etValue = new EditText(this);
        etValue.setTextColor(getResources().getColor(R.color.on_surface));
        if (item.type.equals("web")) {
            etValue.setHint(l.get("bookmark_url"));
            etValue.setText(item.browser);
        } else {
            etValue.setHint(l.get("bookmark_path"));
            etValue.setText(item.path);
        }
        RadioGroup radioGroup = new RadioGroup(this);
        RadioButton rbWeb = new RadioButton(this);
        rbWeb.setText(l.get("web_bookmark"));
        rbWeb.setTextColor(getResources().getColor(R.color.on_surface));
        rbWeb.setId(View.generateViewId());
        RadioButton rbPath = new RadioButton(this);
        rbPath.setText(l.get("path_bookmark"));
        rbPath.setTextColor(getResources().getColor(R.color.on_surface));
        rbPath.setId(View.generateViewId());
        radioGroup.addView(rbWeb);
        radioGroup.addView(rbPath);
        if (item.type.equals("web")) {
            radioGroup.check(rbWeb.getId());
        } else {
            radioGroup.check(rbPath.getId());
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbWeb.getId()) {
                etValue.setHint(l.get("bookmark_url"));
                etValue.setText(item.browser);
            } else {
                etValue.setHint(l.get("bookmark_path"));
                etValue.setText(item.path);
            }
        });
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 0, 32, 0);
        layout.addView(etName);
        layout.addView(radioGroup);
        layout.addView(etValue);
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(l.get("edit"))
                .setView(layout)
                .setPositiveButton(l.get("confirm"), (d, w) -> {
                    String n = etName.getText().toString().trim();
                    String v = etValue.getText().toString().trim();
                    if (n.isEmpty() || v.isEmpty()) {
                        Toast.makeText(MainActivity.this, l.get("name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean ok;
                    if (radioGroup.getCheckedRadioButtonId() == rbWeb.getId()) {
                        ok = BookmarkManager.getInstance(MainActivity.this).update(item.id, n, "web", "nil", v);
                    } else {
                        ok = BookmarkManager.getInstance(MainActivity.this).update(item.id, n, "path", v, "nil");
                    }
                    if (ok) Toast.makeText(MainActivity.this, l.get("bookmark_updated"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }
}