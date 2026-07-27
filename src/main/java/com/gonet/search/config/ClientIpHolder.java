package com.gonet.search.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 감사 컬럼(created_ip/updated_ip)용 요청자 IP 보관소.
 * 웹 요청 스레드: ClientIpFilter가 설정. 비요청 스레드(@Async/@Scheduled): 서버 IP로 폴백.
 */
public final class ClientIpHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private static final String SERVER_IP = resolveServerIp();

    private ClientIpHolder() {
    }

    public static void set(String ip) {
        CURRENT.set(ip);
    }

    public static String get() {
        String ip = CURRENT.get();
        return (ip != null && !ip.isBlank()) ? ip : SERVER_IP;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static String resolveServerIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
