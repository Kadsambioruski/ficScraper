package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDeserializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public JsonDeserializer(){}

    public void readJsonFile() {
        int i = 1;
        String filePath = "scraper/src/main/java/com/example/fics.json";
        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStreamReader);
           
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

        } catch (IOException e) {
            e.printStackTrace();        
        }
    }

    public String getChapAmountInJSON(int ficNumber) {
        String returnStatement = "";
        String filePath = "scraper/src/main/java/com/example/fics.json";

        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStreamReader);
           
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

        } catch (IOException e) {
            e.printStackTrace();        
        }
        return returnStatement; 
    }

    public String getFicLink(int ficNumber) {
        String returnStatement = "";
        String filePath = "scraper/src/main/java/com/example/fics.json";

        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStreamReader);
           
            JsonNode fictionsArray = rootNode.path("fictions");
                if (fictionsArray.isArray()) {
                    for (JsonNode fictionNode : fictionsArray) {
                        if (fictionNode.path("ficID").asInt() == ficNumber) {
                            returnStatement = fictionNode.path("ficLink").asText(); 
                            System.out.println("This is the link: " + returnStatement);
                        }
                    }
                } else {
                    returnStatement = String.format("ficLink not found.");
                    return returnStatement;
                }

        } catch (IOException e) {
            e.printStackTrace();        
        }
        return returnStatement; 
    }

    public String getFicTitle(int ficNumber) {
        String returnStatement = "";
        String filePath = "scraper/src/main/java/com/example/fics.json";

        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStreamReader);
           
            JsonNode fictionsArray = rootNode.path("fictions");
                if (fictionsArray.isArray()) {
                    for (JsonNode fictionNode : fictionsArray) {
                        if (fictionNode.path("ficID").asInt() == ficNumber) {
                            returnStatement = fictionNode.path("title").asText(); 
                            System.out.println("This is the title: " + returnStatement);
                        }
                    }
                } else {
                    returnStatement = String.format("title not found.");
                    return returnStatement;
                }

        } catch (IOException e) {
            e.printStackTrace();        
        }
        return returnStatement; 
    }

}
