package com.wexresprise.revival;

import com.wexresprise.revival.place.PlacePart;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class World {

    public volatile float x;
    public volatile float y;
    public volatile float z;
    public volatile float yaw;

    public volatile float camYaw;
    public volatile float camPitch = 0.32f;

    public volatile float moveX;
    public volatile float moveZ;

    public volatile boolean jumpRequested;

    public volatile int shirt;
    public volatile int pants;
    public volatile int face;
    public volatile int hair;

    public final List<PlacePart> parts = new CopyOnWriteArrayList<>();
}
