package com.redmusic.player.model;

import java.util.ArrayList;
import java.util.List;

/** 持久化的歌单：一个名字 + 一组歌曲 */
public class Playlist {
    public String id = "";
    public String name = "";
    public List<Track> tracks = new ArrayList<>();

    public Playlist() {
    }

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
