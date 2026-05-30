package com.Blabby.Co.UniHub.util;

import android.content.Context;

import com.Blabby.Co.UniHub.data.model.BookmarkItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookmarkManager {

    private static final String FILE_NAME = "collection.yaml"; // Blabby fuck you
    private static final int MAX_BOOKMARKS = 150;
    private static BookmarkManager instance;
    private Context context;
    private List<BookmarkItem> bookmarks;

    private BookmarkManager(Context context) {
        this.context = context.getApplicationContext();
        this.bookmarks = loadBookmarks();
    }

    public static synchronized BookmarkManager getInstance(Context context) {
        if (instance == null) {
            instance = new BookmarkManager(context);
        }
        return instance;
    }

    public static void reinit(Context context) {
        instance = new BookmarkManager(context); // WTF???
    }

    public List<BookmarkItem> getAll() {
        return new ArrayList<>(bookmarks);
    }

    public boolean canAdd() {
        return bookmarks.size() < MAX_BOOKMARKS;
    }

    public int getCount() {
        return bookmarks.size();
    }

    public int getMax() {
        return MAX_BOOKMARKS;
    }

    public int add(String name, String type, String path, String browser) {
        if (bookmarks.size() >= MAX_BOOKMARKS) return -1;
        int newId = 1;
        for (BookmarkItem b : bookmarks) {
            if (b.id >= newId) newId = b.id + 1;
        }
        BookmarkItem item = new BookmarkItem(newId, name, type, path, browser);
        bookmarks.add(item);
        saveBookmarks();
        return newId;
    }

    public boolean update(int id, String name, String type, String path, String browser) {
        for (BookmarkItem b : bookmarks) {
            if (b.id == id) {
                b.name = name;
                b.type = type;
                b.path = path;
                b.browser = browser;
                saveBookmarks();
                return true;
            }
        }
        return false;
    }

    public boolean delete(int id) {
        BookmarkItem toRemove = null;
        for (BookmarkItem b : bookmarks) {
            if (b.id == id) {
                toRemove = b;
                break;
            }
        }
        if (toRemove != null) {
            bookmarks.remove(toRemove);
            saveBookmarks();
            return true;
        }
        return false;
    }

    public BookmarkItem getById(int id) {
        for (BookmarkItem b : bookmarks) {
            if (b.id == id) return b;
        }
        return null;
    }

    private List<BookmarkItem> loadBookmarks() {
        List<BookmarkItem> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            String line;
            BookmarkItem current = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("collection") && line.contains(":")) {
                    if (current != null) list.add(current);
                    int colonIdx = line.indexOf(':');
                    String key = line.substring(0, colonIdx).trim().substring("collection".length());
                    try {
                        current = new BookmarkItem();
                        current.id = Integer.parseInt(key);
                    } catch (NumberFormatException e) {
                        current = null;
                    }
                } else if (current != null && line.startsWith("name:")) {
                    current.name = extractQuotedValue(line);
                } else if (current != null && line.startsWith("type:")) {
                    current.type = extractQuotedValue(line);
                } else if (current != null && line.startsWith("path:")) {
                    current.path = extractQuotedValue(line);
                } else if (current != null && line.startsWith("browser:")) {
                    current.browser = extractQuotedValue(line);
                }
            }
            if (current != null) list.add(current);
            reader.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(list, (a, b) -> Integer.compare(a.id, b.id));
        return list;
    }

    private String extractQuotedValue(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0) return "";
        String rest = line.substring(colonIdx + 1).trim();
        if (rest.startsWith("\"") && rest.endsWith("\"")) {
            return rest.substring(1, rest.length() - 1);
        }
        return rest;
    }

    private void saveBookmarks() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            Collections.sort(bookmarks, (a, b) -> Integer.compare(a.id, b.id));
            for (BookmarkItem b : bookmarks) {
                writer.write("collection" + b.id + ":\n");
                writer.write("  name: \"" + escapeYaml(b.name) + "\"\n");
                writer.write("  type: \"" + escapeYaml(b.type) + "\"\n");
                writer.write("  path: \"" + escapeYaml(b.path) + "\"\n");
                writer.write("  browser: \"" + escapeYaml(b.browser) + "\"\n");
            }
            writer.flush();
            fos.flush();
            writer.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeYaml(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
