package com.Blabby.Co.UniHub.network;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FtpClient {
    private final FTPClient ftp;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public FtpClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        ftp = new FTPClient();
    }

    public boolean connect() throws IOException {
        ftp.connect(host, port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            return false;
        }
        if (!ftp.login(username, password)) {
            ftp.logout();
            return false;
        }
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        return true;
    }

    public List<RemoteFileItem> listFiles(String path) throws IOException {
        List<RemoteFileItem> items = new ArrayList<>();
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile f : files) {
            items.add(new RemoteFileItem(f));
        }
        
        items.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return items;
    }

    public void disconnect() {
        if (ftp.isConnected()) {
            try {
                ftp.logout();
                ftp.disconnect();
            } catch (IOException ignored) {}
        }
    }

    
    public static class RemoteFileItem {
        private final String name;
        private final String path;
        private final boolean isDirectory;
        private final long size;
        private final long lastModified;

        public RemoteFileItem(FTPFile file) {
            this.name = file.getName();
            this.path = file.getName();  
            this.isDirectory = file.isDirectory();
            this.size = file.getSize();
            this.lastModified = file.getTimestamp().getTimeInMillis();
        }

        public String getName() { return name; }
        public String getPath() { return path; }
        public boolean isDirectory() { return isDirectory; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
    }
}