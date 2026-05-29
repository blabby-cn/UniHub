package com.Blabby.Co.UniHub.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.Blabby.Co.UniHub.R;
import com.Blabby.Co.UniHub.data.model.RemoteFileEntry;
import com.Blabby.Co.UniHub.network.FtpClient;
import com.Blabby.Co.UniHub.network.SftpClient;
import com.Blabby.Co.UniHub.ui.adapters.RemoteFileListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteFileBrowserFragment extends Fragment {
    private static final int PANEL_LEFT = 0, PANEL_RIGHT = 1;
    private RecyclerView leftRecycler, rightRecycler;
    private SwipeRefreshLayout swipeRefresh;
    private View divider;

    private final String[] currentPaths = new String[2];
    private final Stack<String>[] backStacks = new Stack[]{new Stack<>(), new Stack<>()};
    private final Stack<String>[] forwardStacks = new Stack[]{new Stack<>(), new Stack<>()};
    private RemoteFileListAdapter leftAdapter, rightAdapter;
    private int activePanel = PANEL_LEFT;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Object client;
    private boolean connected = false;

    public interface OnPathChangeListener { void onPathChanged(String newPath); }
    private OnPathChangeListener pathChangeListener;
    public void setOnPathChangeListener(OnPathChangeListener l) { this.pathChangeListener = l; }

    public void setConnectionInfo(Object client, String initialPath) {
        this.client = client;
        this.connected = true;
        currentPaths[PANEL_LEFT] = initialPath;
        currentPaths[PANEL_RIGHT] = initialPath;
        loadPanel(PANEL_LEFT);
        loadPanel(PANEL_RIGHT);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        leftRecycler = view.findViewById(R.id.left_recycler);
        rightRecycler = view.findViewById(R.id.right_recycler);
        divider = view.findViewById(R.id.divider);
        leftRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        rightRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        swipeRefresh.setColorSchemeColors(android.graphics.Color.parseColor("#FF5722"));
        swipeRefresh.setProgressViewOffset(false, 0, 100);
        swipeRefresh.setOnRefreshListener(() -> refreshAll());

        leftRecycler.setOnTouchListener((v, e) -> { if (e.getAction() == MotionEvent.ACTION_DOWN) switchToPanel(PANEL_LEFT); return false; });
        rightRecycler.setOnTouchListener((v, e) -> { if (e.getAction() == MotionEvent.ACTION_DOWN) switchToPanel(PANEL_RIGHT); return false; });
        divider.setOnClickListener(v -> switchToPanel(activePanel == PANEL_LEFT ? PANEL_RIGHT : PANEL_LEFT));

        if (connected) {
            loadPanel(PANEL_LEFT);
            loadPanel(PANEL_RIGHT);
        }
        return view;
    }

    private void switchToPanel(int panel) {
        if (activePanel != panel) {
            activePanel = panel;
            if (pathChangeListener != null) pathChangeListener.onPathChanged(currentPaths[activePanel]);
        }
    }

    private void loadPanel(int panel) {
        if (!connected) return;
        String path = currentPaths[panel];
        swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            try {
                List<RemoteFileEntry> items = new ArrayList<>();

                if (!"/".equals(path)) {
                    String parent = path.substring(0, path.lastIndexOf('/'));
                    if (parent.isEmpty()) parent = "/";
                    items.add(new RemoteFileEntry("Parent Directory", parent, true));
                }

                List<?> rawItems;
                if (client instanceof FtpClient) {
                    rawItems = ((FtpClient) client).listFiles(path);
                } else if (client instanceof SftpClient) {
                    rawItems = ((SftpClient) client).listFiles(path);
                } else {
                    rawItems = new ArrayList<>();
                }

                for (Object obj : rawItems) {
                    String name = "", itemPath = "";
                    boolean isDir = false;
                    long size = 0, lastMod = 0;
                    if (obj instanceof FtpClient.RemoteFileItem) {
                        FtpClient.RemoteFileItem fi = (FtpClient.RemoteFileItem) obj;
                        name = fi.getName();
                        itemPath = fi.getPath();
                        isDir = fi.isDirectory();
                        size = fi.getSize();
                        lastMod = fi.getLastModified();
                    } else if (obj instanceof SftpClient.RemoteFileItem) {
                        SftpClient.RemoteFileItem si = (SftpClient.RemoteFileItem) obj;
                        name = si.getName();
                        itemPath = si.getPath();
                        isDir = si.isDirectory();
                        size = si.getSize();
                        lastMod = si.getLastModified();
                    }
                    String fullPath = path.endsWith("/") ? path + itemPath : path + "/" + itemPath;
                    items.add(new RemoteFileEntry(name, fullPath, isDir, size, lastMod));
                }

                mainHandler.post(() -> {
                    RemoteFileListAdapter adapter = new RemoteFileListAdapter(items,
                        item -> {
                            if ("Parent Directory".equals(item.getName())) goUp(panel);
                            else if (item.isDirectory()) goInto(panel, item.getPath());
                            else Toast.makeText(requireContext(), "文件: " + item.getName(), Toast.LENGTH_SHORT).show();
                        },
                        (entry, anchor) -> Toast.makeText(requireContext(), "长按菜单待实现", Toast.LENGTH_SHORT).show()
                    );
                    RecyclerView recycler = panel == PANEL_LEFT ? leftRecycler : rightRecycler;
                    recycler.setAdapter(adapter);
                    if (panel == PANEL_LEFT) leftAdapter = adapter; else rightAdapter = adapter;
                    if (panel == activePanel && pathChangeListener != null)
                        pathChangeListener.onPathChanged(path);
                    swipeRefresh.setRefreshing(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void refreshAll() {
        swipeRefresh.setRefreshing(true);
        loadPanel(PANEL_LEFT);
        loadPanel(PANEL_RIGHT);
    }

    private void goInto(int panel, String path) {
        backStacks[panel].push(currentPaths[panel]);
        forwardStacks[panel].clear();
        currentPaths[panel] = path;
        loadPanel(panel);
    }

    private void goUp(int panel) {
        String path = currentPaths[panel];
        if (!"/".equals(path)) {
            String parent = path.substring(0, path.lastIndexOf('/'));
            if (parent.isEmpty()) parent = "/";
            backStacks[panel].push(path);
            forwardStacks[panel].clear();
            currentPaths[panel] = parent;
            loadPanel(panel);
        } else {
            Toast.makeText(requireContext(), "已经是根目录", Toast.LENGTH_SHORT).show();
        }
    }

    public void goBack() {
        int p = activePanel;
        if (!backStacks[p].isEmpty()) {
            forwardStacks[p].push(currentPaths[p]);
            currentPaths[p] = backStacks[p].pop();
            loadPanel(p);
        } else {
            Toast.makeText(requireContext(), "没有更早的记录", Toast.LENGTH_SHORT).show();
        }
    }

    public void goForward() {
        int p = activePanel;
        if (!forwardStacks[p].isEmpty()) {
            backStacks[p].push(currentPaths[p]);
            currentPaths[p] = forwardStacks[p].pop();
            loadPanel(p);
        } else {
            Toast.makeText(requireContext(), "没有更新的记录", Toast.LENGTH_SHORT).show();
        }
    }

    public void goUpActive() {
        goUp(activePanel);
    }

    public void refreshActivePanel() {
        refreshAll();
    }

    public void loadFiles(String path) {
        backStacks[activePanel].push(currentPaths[activePanel]);
        forwardStacks[activePanel].clear();
        currentPaths[activePanel] = path;
        loadPanel(activePanel);
    }

    public String getCurrentPath() {
        return currentPaths[activePanel];
    }
}
