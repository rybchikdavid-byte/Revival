package com.wexresprise.revival.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public final class NetCode {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private NetCode() {
    }

    public static String localAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            String fallback = null;
            for (NetworkInterface nif : all) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip == null) continue;
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.16.")
                            || ip.startsWith("172.17.") || ip.startsWith("172.18.") || ip.startsWith("172.19.")
                            || ip.startsWith("172.2") || ip.startsWith("172.30.") || ip.startsWith("172.31.")) {
                        return ip;
                    }
                    if (fallback == null && !ip.startsWith("169.254.")) fallback = ip;
                }
            }
            return fallback;
        } catch (Exception e) {
            return null;
        }
    }

    /** Код из IP и порта: 6 байт -> 10 символов base32. */
    public static String encode(String ip, int port) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            byte[] raw = addr.getAddress();
            if (raw.length != 4) return null;
            byte[] all = new byte[6];
            System.arraycopy(raw, 0, all, 0, 4);
            all[4] = (byte) ((port >> 8) & 0xFF);
            all[5] = (byte) (port & 0xFF);
            String s = base32(all);
            StringBuilder out = new StringBuilder("RVL");
            for (int i = 0; i < s.length(); i++) {
                if (i > 0 && i % 5 == 0) out.append('-');
                out.append(s.charAt(i));
            }
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Обратно в "ip:port". Принимает и код, и адрес вручную. */
    public static String decode(String code) {
        if (code == null) return null;
        String clean = code.trim().toUpperCase().replace(" ", "").replace("-", "");
        if (clean.startsWith("RVL")) clean = clean.substring(3);
        if (clean.contains(":") || clean.contains(".")) {
            return code.trim();
        }
        byte[] raw = unbase32(clean);
        if (raw == null || raw.length < 6) return null;
        String ip = (raw[0] & 0xFF) + "." + (raw[1] & 0xFF) + "." + (raw[2] & 0xFF) + "." + (raw[3] & 0xFF);
        int port = ((raw[4] & 0xFF) << 8) | (raw[5] & 0xFF);
        if (port <= 0) return null;
        return ip + ":" + port;
    }

    private static String base32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(ALPHABET[(buffer >> bits) & 31]);
            }
        }
        if (bits > 0) {
            sb.append(ALPHABET[(buffer << (5 - bits)) & 31]);
        }
        return sb.toString();
    }

    private static byte[] unbase32(String s) {
        int buffer = 0;
        int bits = 0;
        byte[] out = new byte[s.length() * 5 / 8 + 1];
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            int v = indexOf(s.charAt(i));
            if (v < 0) return null;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out[n++] = (byte) ((buffer >> bits) & 0xFF);
            }
        }
        byte[] res = new byte[n];
        System.arraycopy(out, 0, res, 0, n);
        return res;
    }

    private static int indexOf(char c) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == c) return i;
        }
        if (c == 'O') return 0;
        if (c == 'I' || c == 'L') return 1;
        return -1;
    }
}
