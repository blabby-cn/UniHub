package com.Blabby.Co.UniHub.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileOperations {

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFile(child);
                }
            }
        }
        return file.delete();
    }

    public static boolean renameFile(File file, String newName) {
        File newFile = new File(file.getParent(), newName);
        return file.renameTo(newFile);
    }

    public static boolean copyFile(File source, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(destDir, source.getName());
        if (source.isDirectory()) {
            destFile.mkdirs();
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFile(child, destFile);
                }
            }
            return true;
        } else {
            try (FileInputStream fis = new FileInputStream(source);
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
            return true;
        }
    }

    public static boolean moveFile(File source, File destDir) throws IOException {
        if (!source.exists()) return false;
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(destDir, source.getName());

        if (source.renameTo(destFile)) {
            return true;
        }

        boolean copied = copyFile(source, destDir);
        if (copied) {
            boolean deleted = deleteFile(source);
            return true;
        }
        return false;
    }

    public static void shareFile(Context context, File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(getMimeType(file));
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "分享文件"));
    }

    public static void openWith(Context context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        intent.setDataAndType(uri, getMimeType(file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "打开方式"));
    }

    private static String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return "*/*";
    }
}
