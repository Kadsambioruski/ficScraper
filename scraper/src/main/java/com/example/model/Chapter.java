package com.example.model;

import java.util.UUID;

public class Chapter {
    private UUID id;
    private UUID fictionId;
    private int chapterNumber;
    private String title;
    private String url;
    private String content;

    public Chapter() {
    }

    public Chapter(UUID fictionId, int chapterNumber, String title, String url, String content) {
        this.id = UUID.randomUUID();
        this.fictionId = fictionId;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.url = url;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFictionId() {
        return fictionId;
    }

    public void setFictionId(UUID fictionId) {
        this.fictionId = fictionId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}