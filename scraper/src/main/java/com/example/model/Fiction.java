package com.example.model;

import com.example.scrape.Site;

public class Fiction {
    String ficLink;
    Site site; 
    int ficID;
    String title;
    String author;
    int chapAmount;
    String description;


    public Fiction(){}

    public Fiction(String ficLink, Site site, int ficID, String title, String author, int chapAmount, String description) {
        this.ficLink = ficLink;
        this.site = site;
        this.ficID = ficID;
        this.title = title;
        this.author = author;
        this.chapAmount = chapAmount;
        this.description = description;
    }

    public String getFicLink() {
        return this.ficLink;
    }
    
    public Site getSite() {
        return this.site;
    }

    public int getFicID() {
        return this.ficID;
    }

    public String getTitle() {
        return this.title;
    }

    public String getAuthor() {
        return this.author;
    }

    public int getChapAmount() {
        return this.chapAmount;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setChapAmount(int chapter) {
        this.chapAmount = chapter;
    }

    public void setFicId(int ficId) {
        this.ficID = ficId;
    }




}
