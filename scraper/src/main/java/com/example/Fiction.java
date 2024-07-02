package com.example;

public class Fiction {
    String ficLink;
    int ficID;
    String title;
    String author;
    String chapAmount;
    String description;


    public Fiction(){}

    public Fiction(String ficLink, int ficID, String title, String author, String chapAmount, String description) {
        this.ficLink = ficLink;
        this.ficID = ficID;
        this.title = title;
        this.author = author;
        this.chapAmount = chapAmount;
        this.description = description;
    }

    public String getFicLink() {
        return this.ficLink;
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

    public String getChapAmount() {
        return this.chapAmount;
    }

    public String getDescription() {
        return this.description;
    }


}
