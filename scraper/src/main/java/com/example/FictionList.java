package com.example;

import java.util.ArrayList;
import java.util.List;

public class FictionList {
    private List<Fiction> fictions;

    public FictionList() {
        this.fictions = new ArrayList<>();
    }

    public List<Fiction> getFictions() {
        return fictions;
    }

    public Fiction getFiction(int ficId) {
        for (Fiction fiction : fictions) {
            if (fiction.getFicID() == ficId) {
                return fiction;
            }
        }
        return null;
    }

    public void setFictions(List<Fiction> fictions) {
        this.fictions = fictions;
    }

    public void addFiction(Fiction fiction) {
        this.fictions.add(fiction);
    }

    public void removeFiction(Fiction fiction) {
        this.fictions.remove(fiction);
    }
}
