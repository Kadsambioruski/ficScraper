package com.example;

public class Fiction {
    String title;
    String author;
    String chapAmount;
    String description;

    public Fiction(){}

    public Fiction(String title, String author, String chapAmount, String description) {
        this.title = title;
        this.author = author;
        this.chapAmount = chapAmount;
        this.description = description;
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
