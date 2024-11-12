package com.example.musicplayer.Model;

public class Song {
    private String title;
    private String artist;
    private String url;
    private String duration;

    // Constructor to initialize a Song object
    public Song(String title, String artist, String url, String duration) {
        this.title = title;
        this.artist = artist;
        this.url = url;
        this.duration = duration;
    }

    // Getter methods
    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getUrl() {
        return url;
    }

    public String getDuration() {
        return duration;
    }
}
