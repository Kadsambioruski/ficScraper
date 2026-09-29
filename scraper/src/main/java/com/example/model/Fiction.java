package com.example.model;

import com.example.scrape.Site;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Fiction {
    private UUID id;
    private String ficLink;
    private Site site;
    private String title;
    private String author;
    private int chapAmount;
    private int readerProgress;
    private int wordCount;
    private String description;

    public Fiction() {
    }

    public Fiction(
            String ficLink,
            Site site,
            String title,
            String author,
            int chapAmount,
            int readerProgress,
            int wordCount,
            String description
    ) {
        this.id = UUID.randomUUID();
        this.ficLink = ficLink;
        this.site = site;
        this.title = title;
        this.author = author;
        this.chapAmount = chapAmount;
        this.readerProgress = readerProgress;
        this.wordCount = wordCount;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFicLink() {
        return ficLink;
    }

    public Site getSite() {
        if (site == null && ficLink != null) {
            site = Site.fromUrl(ficLink);
        }
        return site;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getChapAmount() {
        return chapAmount;
    }

    public void setChapAmount(int chapAmount) {
        this.chapAmount = chapAmount;
    }

    public int getReaderProgress() {
        return readerProgress;
    }

    public void setReaderProgress(int readerProgress) {
        this.readerProgress = readerProgress;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getDescription() {
        return description;
    }
}