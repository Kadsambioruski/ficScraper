package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class JsonDeserializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FILEPATH = "scraper\\src\\main\\java\\com\\example\\data\\fics.json";
    public JsonDeserializer(){}

    public JsonNode readJsonFile() {
        try (FileInputStream fileInputStream = new FileInputStream(new File(FILEPATH))) { // Translates the text to UTF_8
            return objectMapper.readTree(fileInputStream);
            
        } catch (IOException e) {
            e.printStackTrace(); 
            return null;       
        }
    }


    public void printJsonContent() {
        int i = 1;
        JsonNode rootNode = readJsonFile();
           
        JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    System.out.println("Fic Number: " + i);
                    String title = fictionNode.path("title").asText();
                    String author = fictionNode.path("author").asText();
                    String chapAmount = fictionNode.path("chapAmount").asText();
                    String description = fictionNode.path("description").asText();

                    System.out.println("Title: " + title);
                    System.out.println("Author: " + author);
                    System.out.println("chapAmount: " + chapAmount);
                    System.out.println("description: " + description);
                    System.out.println();
                    i++;
                }
            } else {
                System.out.println("Array not found.");
            }
    }


    public Fiction getFic(int ficId) {
        JsonNode rootNode = readJsonFile();
           
        JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("ficID").asInt() == ficId){
                        return new Fiction(
                            fictionNode.path("ficLink").asText(),
                            fictionNode.path("ficID").asInt(),
                            fictionNode.path("title").asText(),
                            fictionNode.path("author").asText(),
                            fictionNode.path("chapAmount").asText(),
                            fictionNode.path("description").asText()
                        );
                    }
                }
            } 
            return null;
    }

    public List<Fiction> getAllFics() {
        List<Fiction> fictionList = new ArrayList<>();
        JsonNode rootNode = readJsonFile();
          JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                       fictionList.add(new Fiction(
                        fictionNode.path("ficLink").asText(),
                        fictionNode.path("ficID").asInt(),
                        fictionNode.path("title").asText(),
                        fictionNode.path("author").asText(),
                        fictionNode.path("chapAmount").asText(),
                        fictionNode.path("description").asText()
                    )); 
                }
            } 
        return fictionList;
    }


    public String setFicChapter(int ficId, int chapter) {
        boolean found = false;
        String returnStatement;
        JsonNode rootNode = readJsonFile();
        JsonNode fictionsArray = rootNode.path("fictions");
        
        if (fictionsArray.isArray()) {
            for (JsonNode fictionNode : fictionsArray) {
                if (fictionNode.path("ficID").asInt() == ficId) {

                        ((ObjectNode) fictionNode).put("chapAmount", chapter);
                        found = true;
                    }
                }
                if (found) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(new File(FILEPATH))) {
                    
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, rootNode);
                        
                    } catch (IOException e){
                        e.printStackTrace();
                        returnStatement = "An error occured while updating file";
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
    
    public String getChapAmountInJSON(int ficNumber) {
        String returnStatement = "";

        JsonNode rootNode = readJsonFile();
       
        JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("ficID").asInt() == ficNumber) {
                        String chapAmount = fictionNode.path("chapAmount").asText().split(" ")[0];
                        returnStatement = String.format(chapAmount); 
                    }
                }
            } else {
                returnStatement = String.format("ChapAmount not found.");
                return returnStatement;
            }
        
        return returnStatement; 
    }

    public String getFicLink(int ficId) {
        String returnStatement = "";
       
        JsonNode rootNode = readJsonFile();

        JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("ficID").asInt() == ficId) {
                        returnStatement = fictionNode.path("ficLink").asText(); 
                    }
                }
            } else {
                returnStatement = String.format("ficLink not found.");
                return returnStatement;
            }
        
        return returnStatement; 
    }

    public String getFicTitle(int ficId) {
        String returnStatement = "";

        JsonNode rootNode = readJsonFile();
       
        JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("ficID").asInt() == ficId) {
                        returnStatement = fictionNode.path("title").asText(); 
                    }
                }
            } else {
                returnStatement = String.format("title not found.");
            }
       
        return returnStatement; 
    }

    public boolean matchFicTitle(String ficTitle) {
        boolean titleFound = false;
        JsonNode rootNode = readJsonFile();
       
        JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("title").asText().equals(ficTitle)) {
                        titleFound = true; 
                        System.out.println("Matching fic title found.");
                    }
                }
            } else {
                System.out.println("No matching title found.");
            }
        return titleFound; 
    }

}
