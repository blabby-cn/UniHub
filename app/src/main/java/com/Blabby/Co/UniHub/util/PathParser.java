package com.Blabby.Co.UniHub.util;

public class PathParser {
    public static final int TYPE_LOCAL = 0;
    public static final int TYPE_FTP = 1;
    public static final int TYPE_SFTP = 2;
    public static final int TYPE_HTTP = 3;

    public static int parse(String input) {
        if (input == null) return TYPE_LOCAL;
        String lower = input.toLowerCase().trim();
        if (lower.startsWith("ftp://")) return TYPE_FTP;
        if (lower.startsWith("sftp://")) return TYPE_SFTP;
        if (lower.startsWith("http://") || lower.startsWith("https://")) return TYPE_HTTP;
        return TYPE_LOCAL;
    }

    
    public static String getHost(String url) {
        String noProtocol = url.substring(url.indexOf("://") + 3);
        int slash = noProtocol.indexOf('/');
        if (slash != -1) return noProtocol.substring(0, slash);
        int colon = noProtocol.indexOf(':');
        if (colon != -1) return noProtocol.substring(0, colon);
        return noProtocol;
    }

    public static int getPort(String url, int defaultPort) {
        String noProtocol = url.substring(url.indexOf("://") + 3);
        int slash = noProtocol.indexOf('/');
        String hostPart = slash != -1 ? noProtocol.substring(0, slash) : noProtocol;
        int colon = hostPart.indexOf(':');
        if (colon != -1) {
            try {
                return Integer.parseInt(hostPart.substring(colon + 1));
            } catch (NumberFormatException e) {
                return defaultPort;
            }
        }
        return defaultPort;
    }
}