package com.Blabby.Co.UniHub.data.model;

public class BookmarkItem {
    public int id;
    public String name;
    public String type;
    public String path;
    public String browser;

    public BookmarkItem() {
    }

    public BookmarkItem(int id, String name, String type, String path, String browser) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.browser = browser;
    }
}
