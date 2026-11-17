package com.wexresprise.revival;

public final class Avatar {
    public int shirt;
    public int pants;
    public int face;
    public int hair;

    public Avatar() {
    }

    public Avatar(int shirt, int pants, int face, int hair) {
        this.shirt = shirt;
        this.pants = pants;
        this.face = face;
        this.hair = hair;
    }

    public Avatar copy() {
        return new Avatar(shirt, pants, face, hair);
    }
}
