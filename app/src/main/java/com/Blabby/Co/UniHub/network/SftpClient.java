package com.Blabby.Co.UniHub.network;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SftpClient {
    private Session session;
    private ChannelSftp channel;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public SftpClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public boolean connect() throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(5000);
        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect(5000);
        return true;
    }

    public List<RemoteFileItem> listFiles(String path) throws SftpException {
        List<RemoteFileItem> items = new ArrayList<>();
        Vector<ChannelSftp.LsEntry> entries = channel.ls(path);
        for (ChannelSftp.LsEntry entry : entries) {
            if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) continue;
            items.add(new RemoteFileItem(entry));
        }
        items.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return items;
    }

    public void disconnect() {
        if (channel != null) channel.disconnect();
        if (session != null) session.disconnect();
    }

    public static class RemoteFileItem {
        private final String name;
        private final String path;
        private final boolean isDirectory;
        private final long size;
        private final long lastModified;

        public RemoteFileItem(ChannelSftp.LsEntry entry) {
            this.name = entry.getFilename();
            this.path = entry.getFilename();
            this.isDirectory = entry.getAttrs().isDir();
            this.size = entry.getAttrs().getSize();
            this.lastModified = entry.getAttrs().getMTime() * 1000L;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
        public boolean isDirectory() { return isDirectory; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
    }
}