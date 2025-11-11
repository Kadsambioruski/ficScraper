package com.example;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FicJsonHandler {
    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path jsonPath;

    public FicJsonHandler() {
        this.jsonSerializer = new JsonSerializer();
        this.jsonDeserializer = new JsonDeserializer();
        this.jsonPath = Paths.get(System.getProperty("user.dir"), "data", "fics.json");
    }

    public void addFic(Fiction newFic) {
        jsonSerializer.saveFicToJson(newFic);
    }

    public void moveFicToFinished(Fiction fiction) {
        jsonSerializer.moveFicToFinished(fiction);
    }

    public Fiction getFic(int ficId) {
        return jsonDeserializer.getFic(ficId);
    }
    
    public List<Fiction> getAllFics() {
       return jsonDeserializer.getAllFics();
    }

    public String setFicChapter(int ficId, int chapter) {
        Fiction fiction = getFic(ficId);
        boolean found = false;
        String returnStatement;
        JsonNode rootNode = jsonDeserializer.readJsonFile();
        JsonNode fictionsArray = rootNode.path("fictions");
        
        if (fiction != null) {
            
        }

        if (fictionsArray.isArray()) {
            for (JsonNode fictionNode : fictionsArray) {
                if (fictionNode.path("ficID").asInt() == ficId) {

                        ((ObjectNode) fictionNode).put("chapAmount", chapter);
                        found = true;
                    }
                }
                if (found) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(jsonPath.toFile())) {
                    
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, rootNode);
                        
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    returnStatement = "Chapter amount succesfully updated to: " + chapter;
                } else {
                    returnStatement = "Fic with specified name not found";
                }

            } else {
                returnStatement = String.format("ChapAmount not found.");
            }


        return returnStatement; 
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
