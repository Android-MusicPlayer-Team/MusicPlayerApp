package com.redmusic.player.model;

public class Track {
    public String id = "";
    public String title = "";
    public String artist = "";
    public String album = "";
    public String artworkUrl = "";
    public String previewUrl = "";
<<<<<<< HEAD
    /** 本地歌曲同目录的同名 .lrc 歌词文件（file:// uri，无则空） */
    public String lyricFile = "";
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
    public long durationMs = 0;
    public String genre = "";
    public int rank = 0; // 排行榜序号

    public Track() {
    }

    public Track(String id, String title, String artist, String album, String artworkUrl, String previewUrl, long durationMs) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.artworkUrl = artworkUrl;
        this.previewUrl = previewUrl;
        this.durationMs = durationMs;
    }

    public String displayTitle() {
        return title == null || title.isEmpty() ? "未知歌曲" : title;
    }

    public String displayArtist() {
        return artist == null || artist.isEmpty() ? "未知歌手" : artist;
    }
}
