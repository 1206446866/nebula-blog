package com.nebula.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RequestUtils {
    private RequestUtils() {
    }

    /**
     * 获取客户端IP
     */
    public static String getIp(HttpServletRequest request) {
        String remote = normalizeIp(request.getRemoteAddr());
        return remote == null ? "Unknown" : remote;
    }

    static String normalizeIp(String value) {
        if (value == null || value.length() > 64) return null;
        String ip = value.strip();
        // Numeric literals only: never resolve client-supplied hostnames through DNS.
        if (!(ip.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")
                || (ip.contains(":") && ip.matches("[0-9a-fA-F:.]+")))) return null;
        try {
            return InetAddress.getByName(ip).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
