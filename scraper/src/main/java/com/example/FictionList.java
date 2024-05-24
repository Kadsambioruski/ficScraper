package com.example;

import java.util.List;
import java.util.ArrayList;

public class FictionList {
    private List<Fiction> fictions;

    public FictionList() {
        this.fictions = new ArrayList<>();
    }

    public List<Fiction> getFictions() {
        return fictions;
    }

    public void setFictions(List<Fiction> fictions) {
        this.fictions = fictions;
    }

    public void addFiction(Fiction fiction) {
        this.fictions.add(fiction);
    }
}
