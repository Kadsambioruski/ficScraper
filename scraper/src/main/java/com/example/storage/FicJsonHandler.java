package com.example.storage;
import java.util.List;

import com.example.model.Fiction;


public class FicJsonHandler {
    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;

    public FicJsonHandler() {
        this.jsonSerializer = new JsonSerializer();
        this.jsonDeserializer = new JsonDeserializer();
    }

    public void addFic(Fiction newFic) {
        jsonSerializer.saveFicToJson(newFic);
    }

    public void moveFicToFinished(Fiction fiction) {
        jsonSerializer.moveFicToFinished(fiction);
        resetFicIds();
    }

    public Fiction getFic(int ficId) {
        return jsonDeserializer.getFic(ficId);
    }
    
    public List<Fiction> getAllFics() {
       return jsonDeserializer.getAllFics();
    }

    public void resetFicIds() {
        List<Fiction> fictions = getAllFics();
        int i = 0;

        for (Fiction fiction : fictions) {
            fiction.setFicId(i);
            i++;
        }
    }

    public void setFicChapter(Fiction fiction, int chapter) {
        if (fiction != null) {
            fiction.setChapAmount(chapter);
            System.out.println("Chapter amount succesfully updated to: " + chapter);
        } else {
            System.out.println("Fic with specified name not found");
        }
    }

    public String getFicTitle(int ficId) {
        Fiction fiction = getFic(ficId);
        if (fiction != null) {
            return fiction.getTitle();
        }
        return "Fiction title not found";
    }

}
