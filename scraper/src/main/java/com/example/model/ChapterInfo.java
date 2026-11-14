package com.example.model;

public class ChapterInfo {
    private final String title;
    private final String url;

    public ChapterInfo(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}