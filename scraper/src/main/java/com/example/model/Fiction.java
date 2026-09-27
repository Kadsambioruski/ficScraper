package com.example.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.scrape.Site;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Fiction {
    private UUID id;
    private String ficLink;
    private Site site;
    private String title;
    private String author;
    private int chapAmount;
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
            int wordCount,
            String description
    ) {
        this.id = UUID.randomUUID();
        this.ficLink = ficLink;
        this.site = site;
        this.title = title;
        this.author = author;
        this.chapAmount = chapAmount;
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

    public int getWordCount() {
        return wordCount;
    }

    public String getDescription() {
        return description;
    }

    public void setChapAmount(int chapter) {
        this.chapAmount = chapter;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }
}