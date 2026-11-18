package com.wexresprise.revival.net;

import android.os.Handler;
import android.os.Looper;

import com.wexresprise.revival.Avatar;
import com.wexresprise.revival.Prefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Комната: хост держит сокет, игроки подключаются по коду. */
public class RevivalSession {

    public static final int SOLO = 0;
    public static final int HOST = 1;
    public static final int CLIENT = 2;

    public static volatile RevivalSession current;

    public final int role;
    public String nick;
    public Avatar avatar;
    public String code = "";
    public String address = "";
    public int localId = 1;
    public int port;

    public final Map<Integer, RemotePlayer> players = new ConcurrentHashMap<>();

    public interface Listener {
        void onRoster();
        void onChat(String nick, String text);
        void onError(String message);
    }

    public volatile Listener listener;
    private volatile Runnable readyCallback;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Все записи в сокеты идут здесь. Писать в сокет из UI-потока нельзя:
     * Android бросает NetworkOnMainThreadException, и приложение падает.
     */
    private final ExecutorService writer = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(32),
            r -> {
                Thread t = new Thread(r, "revival-writer");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private ServerSocket server;
    private final List<Peer> peers = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(2);
    private Socket socket;
    private PrintWriter out;

    private RevivalSession(int role, String nick, Avatar avatar) {
        this.role = role;
        this.nick = sanitize(nick, 24);
        this.avatar = avatar == null ? new Avatar() : avatar.copy();
    }

    public static RevivalSession solo(String nick, Avatar avatar) {
        RevivalSession s = new RevivalSession(SOLO, nick, avatar);
        current = s;
        return s;
    }

    public static RevivalSession host(String nick, Avatar avatar) throws IOException {
        RevivalSession s = new RevivalSession(HOST, nick, avatar);
        s.startHost();
        current = s;
        return s;
    }

    public static RevivalSession join(String ipPort, String nick, Avatar avatar) throws IOException {
        RevivalSession s = new RevivalSession(CLIENT, nick, avatar);
        s.connect(ipPort);
        current = s;
        return s;
    }

    public void setReadyCallback(Runnable r) {
        this.readyCallback = r;
    }

    private void fireReady() {
        Runnable r = readyCallback;
        if (r == null) return;
        readyCallback = null;
        main.post(r);
    }

    // ---------------------------------------------------------------- host

    private void startHost() throws IOException {
        server = new ServerSocket(0);
        port = server.getLocalPort();
        String ip = NetCode.localAddress();
        if (ip == null) ip = "127.0.0.1";
        code = NetCode.encode(ip, port);
        if (code == null) code = ip + ":" + port;
        address = ip + ":" + port;

        RemotePlayer me = new RemotePlayer();
        me.id = 1;
        me.nick = nick;
        me.shirt = avatar.shirt;
        me.pants = avatar.pants;
        me.face = avatar.face;
        me.hair = avatar.hair;

        Thread t = new Thread(() -> {
            while (!closed.get()) {
                try {
                    Socket so = server.accept();
                    so.setTcpNoDelay(true);
                    Peer peer = new Peer(so);
                    peers.add(peer);
                    startReader(peer);
                } catch (IOException e) {
                    break;
                }
            }
        }, "revival-accept");
        t.setDaemon(true);
        t.start();
    }

    private void startReader(Peer peer) {
        Thread t = new Thread(() -> {
            try {
                String line;
                while (!closed.get() && (line = peer.in.readLine()) != null) {
                    handleHostLine(peer, line.trim());
                }
            } catch (IOException ignored) {
            } finally {
                dropPeer(peer);
            }
        }, "revival-peer");
        t.setDaemon(true);
        t.start();
    }

    private void handleHostLine(Peer peer, String line) {
        if (line.isEmpty()) return;
        String[] p = line.split("\\|", -1);
        switch (p[0]) {
            case "HELLO": {
                if (peer.joined) return;
                peer.joined = true;
                peer.id = nextId.getAndIncrement();
                peer.nick = sanitize(p.length > 1 ? p[1] : "Player", 24);
                peer.avatar.shirt = parseInt(p, 2);
                peer.avatar.pants = parseInt(p, 3);
                peer.avatar.face = parseInt(p, 4);
                peer.avatar.hair = parseInt(p, 5);

                RemotePlayer rp = toRemote(peer);
                players.put(peer.id, rp);

                send(peer, "WELCOME|" + peer.id + "|1|" + nick + "|" + avatar.shirt + "|"
                        + avatar.pants + "|" + avatar.face + "|" + avatar.hair);

                for (Peer other : peers) {
                    if (other == peer || !other.joined) continue;
                    send(peer, joinLine(toRemote(other)));
                }
                broadcast(joinLine(rp), peer);
                notifyRoster();
                fireReady();
                break;
            }
            case "POS": {
                if (!peer.joined) return;
                peer.x = parseFloat(p, 1);
                peer.y = parseFloat(p, 2);
                peer.z = parseFloat(p, 3);
                peer.yaw = parseFloat(p, 4);
                RemotePlayer rp = players.get(peer.id);
                if (rp != null) {
                    rp.x = peer.x;
                    rp.y = peer.y;
                    rp.z = peer.z;
                    rp.yaw = peer.yaw;
                }
                broadcast("POS|" + peer.id + "|" + peer.x + "|" + peer.y + "|" + peer.z + "|" + peer.yaw, peer);
                break;
            }
            case "CHAT": {
                if (!peer.joined) return;
                String text = sanitize(p.length > 1 ? p[1] : "", 200);
                if (text.isEmpty()) return;
                broadcast("CHAT|" + peer.id + "|" + text, peer);
                final String who = peer.nick;
                main.post(() -> {
                    Listener l = listener;
                    if (l != null) l.onChat(who, text);
                });
                break;
            }
            default:
                break;
        }
    }

    private void dropPeer(Peer peer) {
        if (!peers.remove(peer)) return;
        if (peer.joined) {
            players.remove(peer.id);
            broadcast("LEAVE|" + peer.id, null);
            notifyRoster();
        }
        try {
            peer.socket.close();
        } catch (IOException ignored) {
        }
    }

    // -------------------------------------------------------------- client

    private void connect(String ipPort) throws IOException {
        String target = NetCode.decode(ipPort);
        if (target == null) throw new IOException("Код не распознан");
        address = target;
        int idx = target.lastIndexOf(':');
        if (idx <= 0) throw new IOException("Нужен адрес вида 192.168.1.5:12345");
        String ip = target.substring(0, idx);
        int pt = Integer.parseInt(target.substring(idx + 1));

        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, pt), 6000);
        socket.setTcpNoDelay(true);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out.println("HELLO|" + nick + "|" + avatar.shirt + "|" + avatar.pants + "|" + avatar.face + "|" + avatar.hair);

        Thread t = new Thread(() -> {
            try {
                String line;
                while (!closed.get() && (line = in.readLine()) != null) {
                    handleClientLine(line.trim());
                }
            } catch (IOException ignored) {
            } finally {
                if (!closed.get()) notifyError("Соединение потеряно");
            }
        }, "revival-client");
        t.setDaemon(true);
        t.start();
    }

    private void handleClientLine(String line) {
        if (line.isEmpty()) return;
        String[] p = line.split("\\|", -1);
        switch (p[0]) {
            case "WELCOME": {
                localId = parseInt(p, 1);
                int hostId = parseInt(p, 2);
                RemotePlayer host = new RemotePlayer();
                host.id = hostId;
                host.nick = p.length > 3 ? p[3] : "Host";
                host.shirt = parseInt(p, 4);
                host.pants = parseInt(p, 5);
                host.face = parseInt(p, 6);
                host.hair = parseInt(p, 7);
                players.put(host.id, host);
                notifyRoster();
                fireReady();
                break;
            }
            case "JOIN": {
                RemotePlayer rp = new RemotePlayer();
                rp.id = parseInt(p, 1);
                rp.nick = p.length > 2 ? p[2] : "Player";
                rp.shirt = parseInt(p, 3);
                rp.pants = parseInt(p, 4);
                rp.face = parseInt(p, 5);
                rp.hair = parseInt(p, 6);
                players.put(rp.id, rp);
                notifyRoster();
                break;
            }
            case "LEAVE": {
                players.remove(parseInt(p, 1));
                notifyRoster();
                break;
            }
            case "POS": {
                int id = parseInt(p, 1);
                RemotePlayer rp = players.get(id);
                if (rp != null) {
                    rp.x = parseFloat(p, 2);
                    rp.y = parseFloat(p, 3);
                    rp.z = parseFloat(p, 4);
                    rp.yaw = parseFloat(p, 5);
                }
                break;
            }
            case "CHAT": {
                int id = parseInt(p, 1);
                RemotePlayer rp = players.get(id);
                String who = rp != null ? rp.nick : "Player";
                String text = p.length > 2 ? p[2] : "";
                main.post(() -> {
                    Listener l = listener;
                    if (l != null) l.onChat(who, text);
                });
                break;
            }
            default:
                break;
        }
    }

    // ------------------------------------------------------------ outgoing

    public void sendPosition(final float x, final float y, final float z, final float yaw) {
        if (role == SOLO || closed.get()) return;
        try {
            writer.execute(() -> {
                if (closed.get()) return;
                if (role == HOST) {
                    broadcast("POS|1|" + x + "|" + y + "|" + z + "|" + yaw, null);
                } else if (out != null) {
                    out.println("POS|" + x + "|" + y + "|" + z + "|" + yaw);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    public void sendChat(String text) {
        final String clean = sanitize(text, 200);
        if (clean.isEmpty()) return;
        if (role != SOLO && !closed.get()) {
            try {
                writer.execute(() -> {
                    if (closed.get()) return;
                    if (role == HOST) {
                        broadcast("CHAT|1|" + clean, null);
                    } else if (out != null) {
                        out.println("CHAT|" + clean);
                    }
                });
            } catch (Throwable ignored) {
            }
        }
        dispatchChat(nick, clean);
    }

    private void dispatchChat(final String who, final String text) {
        main.post(() -> {
            Listener l = listener;
            if (l != null) l.onChat(who, text);
        });
    }

    private void broadcast(String line, Peer except) {
        for (Peer peer : peers) {
            if (peer == except || !peer.joined) continue;
            send(peer, line);
        }
    }

    private void send(Peer peer, String line) {
        synchronized (peer) {
            if (peer.out != null) peer.out.println(line);
        }
    }

    private String joinLine(RemotePlayer p) {
        return "JOIN|" + p.id + "|" + p.nick + "|" + p.shirt + "|" + p.pants + "|" + p.face + "|" + p.hair;
    }

    private RemotePlayer toRemote(Peer peer) {
        RemotePlayer rp = new RemotePlayer();
        rp.id = peer.id;
        rp.nick = peer.nick;
        rp.shirt = peer.avatar.shirt;
        rp.pants = peer.avatar.pants;
        rp.face = peer.avatar.face;
        rp.hair = peer.avatar.hair;
        rp.x = peer.x;
        rp.y = peer.y;
        rp.z = peer.z;
        rp.yaw = peer.yaw;
        return rp;
    }

    private void notifyRoster() {
        main.post(() -> {
            Listener l = listener;
            if (l != null) l.onRoster();
        });
    }

    private void notifyError(String message) {
        main.post(() -> {
            Listener l = listener;
            if (l != null) l.onError(message);
        });
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {
        }
        for (Peer peer : peers) {
            try {
                peer.socket.close();
            } catch (IOException ignored) {
            }
        }
        peers.clear();
        players.clear();
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        try {
            writer.shutdownNow();
        } catch (Throwable ignored) {
        }
        if (current == this) current = null;
    }

    public List<RemotePlayer> snapshot() {
        return new ArrayList<>(players.values());
    }

    // ------------------------------------------------------------- helpers

    private static int parseInt(String[] p, int i) {
        try {
            return Integer.parseInt(p[i]);
        } catch (Exception e) {
            return 0;
        }
    }

    private static float parseFloat(String[] p, int i) {
        try {
            return Float.parseFloat(p[i]);
        } catch (Exception e) {
            return 0f;
        }
    }

    public static String sanitize(String s, int max) {
        if (s == null) return "";
        String v = s.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        if (v.length() > max) v = v.substring(0, max);
        return v;
    }

    private static class Peer {
        final Socket socket;
        final PrintWriter out;
        final BufferedReader in;
        final Avatar avatar = new Avatar();
        int id;
        String nick = "Player";
        float x;
        float y;
        float z;
        float yaw;
        boolean joined;

        Peer(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        }
    }

    public static String currentCodeOrEmpty() {
        RevivalSession s = current;
        return s == null ? "" : s.code;
    }

    public static String defaultNick(android.content.Context c) {
        return Prefs.nick(c);
    }
}
