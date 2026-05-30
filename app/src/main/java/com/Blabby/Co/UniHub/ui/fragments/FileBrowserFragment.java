package com.Blabby.Co.UniHub.ui.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.Blabby.Co.UniHub.R;
import com.Blabby.Co.UniHub.GeshihuaBianji;
import com.Blabby.Co.UniHub.VideoSee;
import com.Blabby.Co.UniHub.ZipLiulan;
import com.Blabby.Co.UniHub.WenjianTanchuang;
import com.Blabby.Co.UniHub.data.model.FileItem;
import com.Blabby.Co.UniHub.ui.adapters.FileListAdapter;
import com.Blabby.Co.UniHub.util.BookmarkManager;
import com.Blabby.Co.UniHub.util.FileOperations;
import com.Blabby.Co.UniHub.util.FileUtils;
import com.Blabby.Co.UniHub.util.Localization;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

public class FileBrowserFragment extends Fragment {

    private static final int PANEL_LEFT = 0, PANEL_RIGHT = 1;
    private RecyclerView leftRecycler, rightRecycler;
    private SwipeRefreshLayout swipeRefresh;
    private View divider;
    private final String[] currentPaths = new String[2];
    private final Stack<String>[] backStacks = new Stack[]{new Stack<>(), new Stack<>()};
    private final Stack<String>[] forwardStacks = new Stack[]{new Stack<>(), new Stack<>()};
    private FileListAdapter leftAdapter, rightAdapter;
    private int activePanel = PANEL_LEFT;
    private OnPathChangeListener pathChangeListener;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;

    private final Map<String, Integer>[] scrollPositions = new Map[]{new HashMap<>(), new HashMap<>()};

    public interface StatsListener {
        void onStatsChanged(int folders, int files, long freeSpace, long totalSpace);
    }
    private StatsListener statsListener;
    public void setStatsListener(StatsListener listener) { this.statsListener = listener; }

    public interface OnPathChangeListener { void onPathChanged(String newPath); }
    public void setOnPathChangeListener(OnPathChangeListener l) { this.pathChangeListener = l; }

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

        swipeRefresh.setColorSchemeColors(Color.parseColor("#FF5722"));
        swipeRefresh.setProgressViewOffset(false, 0, 100);
        swipeRefresh.setOnRefreshListener(() -> refreshAll());

        leftRecycler.setOnTouchListener((v, e) -> { if (e.getAction() == MotionEvent.ACTION_DOWN) switchToPanel(PANEL_LEFT); return false; });
        rightRecycler.setOnTouchListener((v, e) -> { if (e.getAction() == MotionEvent.ACTION_DOWN) switchToPanel(PANEL_RIGHT); return false; });
        divider.setOnClickListener(v -> switchToPanel(activePanel == PANEL_LEFT ? PANEL_RIGHT : PANEL_LEFT));

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> { if (isGranted) refreshAll(); else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("need_storage_perm"), Toast.LENGTH_SHORT).show(); });
        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (hasStoragePermission()) refreshAll(); });

        currentPaths[PANEL_LEFT] = FileUtils.getDefaultPath();
        currentPaths[PANEL_RIGHT] = FileUtils.getDefaultPath();

        if (hasStoragePermission()) { loadPanel(PANEL_LEFT); loadPanel(PANEL_RIGHT); }
        else requestStoragePermission();

        updatePanelElevation();
        return view;
    }

    private void switchToPanel(int panel) {
        if (activePanel != panel) {
            activePanel = panel;
            updatePanelElevation();
            if (pathChangeListener != null) pathChangeListener.onPathChanged(currentPaths[activePanel]);
        }
    }

    private void updatePanelElevation() {
        if (leftRecycler != null && rightRecycler != null) {
            leftRecycler.setElevation(activePanel == PANEL_LEFT ? 12f : 4f);
            rightRecycler.setElevation(activePanel == PANEL_RIGHT ? 12f : 4f);
        }
    }

    private void loadPanel(int panel) {
        RecyclerView recycler = (panel == PANEL_LEFT) ? leftRecycler : rightRecycler;
        String oldPath = currentPaths[panel];
        if (oldPath != null && recycler.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager llm = (LinearLayoutManager) recycler.getLayoutManager();
            int pos = llm.findFirstVisibleItemPosition();
            View firstView = llm.findViewByPosition(pos);
            int offset = (firstView != null) ? firstView.getTop() : 0;
            scrollPositions[panel].put(oldPath, (pos << 16) | (offset & 0xFFFF));
        }

        recycler.setAdapter(new FileListAdapter(new ArrayList<>(), item -> {}, (item, anchor) -> {}));
        recycler.postDelayed(() -> {
            String path = currentPaths[panel];
            List<FileItem> items = FileUtils.listFiles(path);
            if (!path.equals("/")) {
                String parent = FileUtils.getParentPath(path);
                if (parent != null) items.add(0, FileItem.createParentItem(parent));
            }
            injectVirtualFolders(path, items);

            int folders = 0, files = 0;
            for (FileItem item : items) {
                if (item.isParent()) continue;
                if (item.isDirectory()) folders++;
                else files++;
            }
            long free = 0, total = 0;
            try {
                StatFs stat = new StatFs(path);
                free = stat.getAvailableBytes();
                total = stat.getTotalBytes();
            } catch (Exception ignored) {}
            if (statsListener != null && panel == activePanel) {
                statsListener.onStatsChanged(folders, files, free, total);
            }

            FileListAdapter adapter = new FileListAdapter(items,
                    item -> {
                        if (item.isParent()) goUp(panel);
                        else if (item.isDirectory()) {
                            if (item.isVirtual() && !new File(item.getPath()).exists()) {
                                Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("need_storage_perm"), Toast.LENGTH_SHORT).show();
                                requestStoragePermission();
                            } else goInto(panel, item.getPath());
                        } else {
                            showOpenChooser(item, panel);
                        }
                    },
                    (item, anchor) -> showActionDialog(item, panel)
            );
            recycler.setAdapter(adapter);
            if (panel == PANEL_LEFT) leftAdapter = adapter;
            else rightAdapter = adapter;

            Integer saved = scrollPositions[panel].get(path);
            if (saved != null) {
                int pos = (saved >> 16) & 0xFFFF;
                int offset = saved & 0xFFFF;
                ((LinearLayoutManager) recycler.getLayoutManager()).scrollToPositionWithOffset(pos, offset);
            }

            updatePanelElevation();
            if (panel == activePanel && pathChangeListener != null) pathChangeListener.onPathChanged(path);

            swipeRefresh.setRefreshing(false);
        }, 32);
    }

    private void refreshAll() {
        swipeRefresh.setRefreshing(true);
        loadPanel(PANEL_LEFT);
        loadPanel(PANEL_RIGHT);
    }

    private void injectVirtualFolders(String path, List<FileItem> items) {
        if (path.equals("/storage")) {
            boolean hasEmulated = items.stream().anyMatch(i -> "emulated".equals(i.getName()));
            if (!hasEmulated) items.add(FileItem.createVirtualDirectory("emulated", "/storage/emulated"));
        } else if (path.equals("/storage/emulated")) {
            boolean has0 = items.stream().anyMatch(i -> "0".equals(i.getName()));
            if (!has0) items.add(FileItem.createVirtualDirectory("0", "/storage/emulated/0"));
        }
    }

    private void goInto(int panel, String path) {
        backStacks[panel].push(currentPaths[panel]);
        forwardStacks[panel].clear();
        currentPaths[panel] = path;
        loadPanel(panel);
    }

    private void goUp(int panel) {
        String parent = FileUtils.getParentPath(currentPaths[panel]);
        if (parent != null) {
            backStacks[panel].push(currentPaths[panel]);
            forwardStacks[panel].clear();
            currentPaths[panel] = parent;
            loadPanel(panel);
        } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("already_root"), Toast.LENGTH_SHORT).show();
    }

    public void loadFiles(String path) {
        backStacks[activePanel].push(currentPaths[activePanel]);
        forwardStacks[activePanel].clear();
        currentPaths[activePanel] = path;
        loadPanel(activePanel);
    }

    public void goBack() {
        int p = activePanel;
        if (!backStacks[p].isEmpty()) {
            forwardStacks[p].push(currentPaths[p]);
            currentPaths[p] = backStacks[p].pop();
            loadPanel(p);
        } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("no_earlier"), Toast.LENGTH_SHORT).show();
    }

    public void goForward() {
        int p = activePanel;
        if (!forwardStacks[p].isEmpty()) {
            backStacks[p].push(currentPaths[p]);
            currentPaths[p] = forwardStacks[p].pop();
            loadPanel(p);
        } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("no_later"), Toast.LENGTH_SHORT).show();
    }

    public void goUpActive() { goUp(activePanel); }
    public void refreshActivePanel() { refreshAll(); }

    private void showActionDialog(FileItem item, int panel) {
        if (!(getContext() instanceof android.content.Context)) return;
        File file = new File(item.getPath());
        WenjianTanchuang.show(requireContext(), file, new WenjianTanchuang.ActionListener() {
            @Override public void onCopy(File f) { performCopyOrMove(f, false); }
            @Override public void onMove(File f) { performCopyOrMove(f, true); }
            @Override public void onDelete(File f) { confirmDelete(f); }
            @Override public void onRename(File f) { showRenameDialog(f); }
            @Override public void onProperties(File f) { showProperties(f); }
            @Override public void onShare(File f) { FileOperations.shareFile(requireContext(), f); }
            @Override public void onOpenWith(File f) { FileOperations.openWith(requireContext(), f); }
            @Override public void onCompress(File f) { showCompressDialog(f); }
            @Override public void onAddBookmark(File f) {
                BookmarkManager bm = BookmarkManager.getInstance(requireContext());
                if (!bm.canAdd()) {
                    Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("bookmark_limit_reached"), Toast.LENGTH_SHORT).show();
                    return;
                }
                bm.add(f.getName(), "path", f.getAbsolutePath(), "nil");
                Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("bookmark_added"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCopyOrMove(File source, boolean move) {
        int targetPanel = (activePanel == PANEL_LEFT) ? PANEL_RIGHT : PANEL_LEFT;
        String destPath = currentPaths[targetPanel];
        File destDir = new File(destPath);
        new Thread(() -> {
            try {
                boolean success = move ? FileOperations.moveFile(source, destDir) : FileOperations.copyFile(source, destDir);
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("move_success", Localization.getInstance(requireContext()).get(move ? "moving" : "copying")), Toast.LENGTH_SHORT).show();
                        loadPanel(PANEL_LEFT);
                        loadPanel(PANEL_RIGHT);
                    } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("op_failed"), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("error_colon", e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(Localization.getInstance(requireContext()).get("confirm_delete"))
                .setMessage(Localization.getInstance(requireContext()).get("confirm_delete_msg", file.getName()))
                .setPositiveButton(Localization.getInstance(requireContext()).get("delete"), (d, w) -> {
                    if (FileOperations.deleteFile(file)) {
                        Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("deleted"), Toast.LENGTH_SHORT).show();
                        loadPanel(activePanel);
                    } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("delete_failed"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(Localization.getInstance(requireContext()).get("cancel"), null).show();
    }

    private void showRenameDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme);
        builder.setTitle(Localization.getInstance(requireContext()).get("rename"));
        final EditText input = new EditText(requireContext());
        input.setText(file.getName());
        input.setTextColor(getResources().getColor(R.color.on_surface));
        builder.setView(input);
        builder.setPositiveButton(Localization.getInstance(requireContext()).get("ok"), (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty() || newName.equals(file.getName())) return;
            if (FileOperations.renameFile(file, newName)) {
                Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("rename_success"), Toast.LENGTH_SHORT).show();
                loadPanel(activePanel);
            } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("rename_failed"), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(Localization.getInstance(requireContext()).get("cancel"), null);
        builder.show();
    }

    private void showProperties(File file) {
        String info = Localization.getInstance(requireContext()).get("name_colon", file.getName()) +
                "\n" + Localization.getInstance(requireContext()).get("path_colon", file.getAbsolutePath()) +
                "\n" + Localization.getInstance(requireContext()).get("size_colon", file.isDirectory() ? Localization.getInstance(requireContext()).get("folder") : file.length() + Localization.getInstance(requireContext()).get("bytes")) +
                "\n" + Localization.getInstance(requireContext()).get("modified_colon", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(file.lastModified())) +
                "\n" + Localization.getInstance(requireContext()).get("readable_colon", file.canRead()) +
                "\n" + Localization.getInstance(requireContext()).get("writable_colon", file.canWrite());
        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(Localization.getInstance(requireContext()).get("properties"))
                .setMessage(info)
                .setPositiveButton(Localization.getInstance(requireContext()).get("ok"), null).show();
    }

    public void showCreateDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_file, null);
        EditText etName = view.findViewById(R.id.et_name);
        RadioGroup radioGroup = view.findViewById(R.id.radio_type);
        radioGroup.check(R.id.radio_file);

        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(Localization.getInstance(requireContext()).get("create_new"))
                .setView(view)
                .setPositiveButton(Localization.getInstance(requireContext()).get("create"), (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean isFolder = radioGroup.getCheckedRadioButtonId() == R.id.radio_folder;
                    File newFile = new File(currentPaths[activePanel], name);
                    if (newFile.exists()) {
                        Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("already_exists"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean success;
                    if (isFolder) {
                        success = newFile.mkdirs();
                    } else {
                        try { success = newFile.createNewFile(); } catch (IOException e) { success = false; }
                    }
                    if (success) {
                        Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("create_success"), Toast.LENGTH_SHORT).show();
                        loadPanel(activePanel);
                    } else Toast.makeText(requireContext(), Localization.getInstance(requireContext()).get("create_failed"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(Localization.getInstance(requireContext()).get("cancel"), null)
                .show();
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Environment.isExternalStorageManager();
        else return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                manageStorageLauncher.launch(intent);
            }
        } else requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public String getCurrentPath() { return currentPaths[activePanel]; }

    private boolean isVideoFile(String name) {
        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv")
                || name.endsWith(".mov") || name.endsWith(".wmv") || name.endsWith(".flv")
                || name.endsWith(".3gp") || name.endsWith(".webm");
    }

    private boolean isZipFile(String name) {
        return name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".apk");
    }

    private boolean isTextFile(String name) {
        String low = name.toLowerCase();
        // 所有文本文件（包括 md）都用格式化编辑器
        return low.endsWith(".txt") || low.endsWith(".md") || low.endsWith(".markdown")
                || low.endsWith(".log") || low.endsWith(".json") || low.endsWith(".xml")
                || low.endsWith(".csv") || low.endsWith(".ini") || low.endsWith(".cfg")
                || low.endsWith(".conf") || low.endsWith(".bat") || low.endsWith(".sh")
                || low.endsWith(".py") || low.endsWith(".js") || low.endsWith(".ts")
                || low.endsWith(".html") || low.endsWith(".css") || low.endsWith(".java")
                || low.endsWith(".kt") || low.endsWith(".cpp") || low.endsWith(".c")
                || low.endsWith(".h") || low.endsWith(".yml") || low.endsWith(".yaml")
                || low.endsWith(".toml") || low.endsWith(".properties") || low.endsWith(".gradle");
    }

    private static boolean dontAskOpenWithWarning = false;

    private void showOpenChooser(FileItem item, int panel) {
        Localization l = Localization.getInstance(requireContext());
        String lowName = item.getName().toLowerCase();
        boolean isZip = isZipFile(lowName);
        boolean isVideo = isVideoFile(lowName);
        boolean isText = isTextFile(lowName);

        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        if (isZip) {
            labels.add(l.get("zip_browser"));
            actions.add(() -> {
                Intent intent = new Intent(requireContext(), ZipLiulan.class);
                intent.putExtra("zip_path", item.getPath());
                intent.putExtra("zip_name", item.getName());
                startActivity(intent);
            });
        }
        if (isText) {
            // 统一使用格式化编辑器
            labels.add(l.get("md_editor"));
            actions.add(() -> {
                Intent intent = new Intent(requireContext(), GeshihuaBianji.class);
                intent.putExtra("file_path", item.getPath());
                intent.putExtra("file_name", item.getName());
                startActivity(intent);
            });
        }
        if (isVideo) {
            labels.add(l.get("video_player"));
            actions.add(() -> {
                Intent intent = new Intent(requireContext(), VideoSee.class);
                intent.putExtra("video_path", item.getPath());
                intent.putExtra("video_name", item.getName());
                startActivity(intent);
            });
        }
        labels.add(l.get("other_open_with"));
        actions.add(() -> openWithExternalAppWithWarning(item));

        labels.add(l.get("cancel"));
        actions.add(null);

        String[] arr = labels.toArray(new String[0]);
        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(l.get("choose_open_with"))
                .setItems(arr, (dialog, which) -> {
                    Runnable action = actions.get(which);
                    if (action != null) action.run();
                })
                .show();
    }

    private void showCompressDialog(File file) {
        Localization l = Localization.getInstance(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_compress, null);
        EditText etPath = view.findViewById(R.id.et_compress_path);
        EditText etName = view.findViewById(R.id.et_compress_name);
        Spinner spFormat = view.findViewById(R.id.sp_compress_format);

        String defaultName = file.isDirectory() ? file.getName() : file.getName().replaceAll("\\.[^.]+$", "");
        etPath.setText(currentPaths[activePanel]);
        etName.setText(defaultName);

        String[] formats = {".zip", ".xz"};
        spFormat.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, formats));

        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(l.get("compress_title", file.getName()))
                .setView(view)
                .setPositiveButton(l.get("start_compress"), (d, w) -> {
                    String dir = etPath.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    if (dir.isEmpty() || name.isEmpty()) {
                        Toast.makeText(requireContext(), l.get("path_name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean useXz = spFormat.getSelectedItemPosition() == 1;
                    String ext = useXz ? ".xz" : ".zip";
                    String fullPath = dir.endsWith("/") ? dir + name + ext : dir + "/" + name + ext;

                    Toast.makeText(requireContext(), l.get("compressing"), Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            if (useXz) {
                                compressXz(file, new File(fullPath));
                            } else {
                                compressZip(file, new File(fullPath));
                            }
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), l.get("compress_done", fullPath), Toast.LENGTH_LONG).show();
                                loadPanel(activePanel);
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), l.get("compress_failed", e.getMessage()), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }

    private void compressZip(File source, File dest) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        ZipOutputStream zos = new ZipOutputStream(fos);
        byte[] buffer = new byte[8192];
        if (source.isDirectory()) {
            for (File child : source.listFiles()) {
                addToZip(child, child.getName(), zos, buffer);
            }
        } else {
            addToZip(source, source.getName(), zos, buffer);
        }
        zos.close();
        fos.close();
    }

    private void addToZip(File file, String entryName, ZipOutputStream zos, byte[] buffer) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                addToZip(child, entryName + "/" + child.getName(), zos, buffer);
            }
        } else {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            FileInputStream fis = new FileInputStream(file);
            int len;
            while ((len = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
            fis.close();
            zos.closeEntry();
        }
    }

    private void compressXz(File source, File dest) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        LZMA2Options options = new LZMA2Options();
        XZOutputStream xos = new XZOutputStream(fos, options);
        byte[] buffer = new byte[8192];
        if (source.isDirectory()) {
            for (File child : source.listFiles()) {
                addToXz(child, xos, buffer);
            }
        } else {
            FileInputStream fis = new FileInputStream(source);
            int len;
            while ((len = fis.read(buffer)) != -1) {
                xos.write(buffer, 0, len);
            }
            fis.close();
        }
        xos.close();
        fos.close();
    }

    private void addToXz(File file, XZOutputStream xos, byte[] buffer) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                addToXz(child, xos, buffer);
            }
        } else {
            FileInputStream fis = new FileInputStream(file);
            int len;
            while ((len = fis.read(buffer)) != -1) {
                xos.write(buffer, 0, len);
            }
            fis.close();
        }
    }

    private void openWithExternalAppWithWarning(FileItem item) {
        Localization l = Localization.getInstance(requireContext());
        if (dontAskOpenWithWarning) {
            FileOperations.openWith(requireContext(), new File(item.getPath()));
            return;
        }

        View checkView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_external_warning, null);
        CheckBox cbDontAsk = checkView.findViewById(R.id.cb_dont_ask);

        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle(l.get("external_app_title"))
                .setMessage(l.get("external_app_warning"))
                .setView(checkView)
                .setPositiveButton(l.get("ok"), (d, w) -> {
                    if (cbDontAsk.isChecked()) dontAskOpenWithWarning = true;
                    FileOperations.openWith(requireContext(), new File(item.getPath()));
                })
                .setNegativeButton(l.get("cancel"), null)
                .show();
    }
}