package com.Blabby.Co.UniHub.data.model;

import java.io.File;

public class FileItem {
    private String name;
    private String path;
    private boolean isDirectory;
    private boolean isParent;
    private boolean isVirtual;   
    private long size;
    private long lastModified;

    
    public FileItem(File file) {
        this.name = file.getName();
        this.path = file.getAbsolutePath();
        this.isDirectory = file.isDirectory();
        this.isParent = false;
        this.isVirtual = false;
        this.size = file.isFile() ? file.length() : 0;
        this.lastModified = file.lastModified();
    }

    
    public static FileItem createParentItem(String parentPath) {
        FileItem item = new FileItem();
        item.name = "..";
        item.path = parentPath;
        item.isDirectory = true;
        item.isParent = true;
        item.isVirtual = false;
        item.size = 0;
        item.lastModified = 0;
        return item;
    }

    
    public static FileItem createVirtualDirectory(String name, String fullPath) {
        FileItem item = new FileItem();
        item.name = name;
        item.path = fullPath;
        item.isDirectory = true;
        item.isParent = false;
        item.isVirtual = true;
        item.size = 0;
        item.lastModified = 0;
        return item;
    }

    private FileItem() {}

    
    public String getName() { return name; }
    public String getPath() { return path; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isParent() { return isParent; }
    public boolean isVirtual() { return isVirtual; }
    public long getSize() { return size; }
    public long getLastModified() { return lastModified; }
}