package com.example;

public class Fiction {
    String name;
    String description;

    public Fiction(){}

    public Fiction(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }


}
