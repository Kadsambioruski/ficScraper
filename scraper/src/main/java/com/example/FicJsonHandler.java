package com.example;
import java.util.List;


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

    public int getChapAmount(int ficId) {
        Fiction fiction = getFic(ficId);
        if (fiction != null) {
            return fiction.getChapAmount();
        }
        return -1;    
    }

    public String getFicLink(int ficId) {
        Fiction fiction = getFic(ficId);
        if (fiction != null) {
            return fiction.getFicLink();
        }
        return "Fiction link not found";
    }

    public String getFicTitle(int ficId) {
        Fiction fiction = getFic(ficId);
        if (fiction != null) {
            return fiction.getTitle();
        }
        return "Fiction title not found";
    }

    public boolean matchFicTitle(String ficTitle) {
        List<Fiction> fictions = getAllFics();
        for (Fiction fiction : fictions) {
            if (fiction.getTitle().equals(ficTitle)) {
                System.out.println("Matching fic title found.");
                return true; 
            }
        } 

        return false;
    }

    public void printJsonContent() {
        List<Fiction> fictions = getAllFics();
        int i = 1;
        if (fictions.isEmpty()) {
            System.out.println("No fictions found");
            return;
        }
           
        for (Fiction fic : fictions) {
            System.out.println("Fic Number: " + i);

            System.out.println("Title: " + fic.getTitle());
            System.out.println("Author: " + fic.getAuthor());
            System.out.println("chapAmount: " + fic.getChapAmount());
            System.out.println("description: " + fic.getDescription());
            System.out.println();
            i++;
        }
    }


}
