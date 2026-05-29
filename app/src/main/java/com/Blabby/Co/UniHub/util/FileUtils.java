package com.Blabby.Co.UniHub.util;

import android.os.Environment;

import com.Blabby.Co.UniHub.data.model.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileUtils {

    
    public static List<FileItem> listFiles(String path) {
        File dir = new File(path);
        List<FileItem> items = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                List<File> fileList = Arrays.asList(files);
                Collections.sort(fileList, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                for (File f : fileList) {
                    items.add(new FileItem(f));
                }
            }
        }
        return items;
    }

    
    public static String getParentPath(String path) {
        if (path == null || path.equals("/")) return null;
        File file = new File(path);
        String parent = file.getParent();
        if (parent == null) return "/";
        return parent;
    }

    
    public static String getDefaultPath() {
        
        File externalDir = Environment.getExternalStorageDirectory();
        if (externalDir != null && externalDir.exists() && externalDir.isDirectory()) {
            return externalDir.getAbsolutePath();
        }

        
        
        
        
        
        return "/storage/emulated/0";
    }
}