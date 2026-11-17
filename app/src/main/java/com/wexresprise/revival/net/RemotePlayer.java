package com.wexresprise.revival.net;

public class RemotePlayer {
    public int id;
    public String nick = "Player";
    public int shirt;
    public int pants;
    public int face;
    public int hair;
    public float x;
    public float y;
    public float z;
    public float yaw;

    public RemotePlayer copy() {
        RemotePlayer p = new RemotePlayer();
        p.id = id;
        p.nick = nick;
        p.shirt = shirt;
        p.pants = pants;
        p.face = face;
        p.hair = hair;
        p.x = x;
        p.y = y;
        p.z = z;
        p.yaw = yaw;
        return p;
    }
}
