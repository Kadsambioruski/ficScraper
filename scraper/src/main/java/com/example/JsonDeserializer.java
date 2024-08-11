package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class JsonDeserializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String filePath = "fics.json";
    public JsonDeserializer(){}

    public void readJsonFile() {
        int i = 1;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) { // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStream);
           
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


    public int getFicId(String ficName) {
        int ficId = 0;
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) { // Translates the text to UTF_8
            
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
            JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("title").asText().equals(ficName)) {
                            ficId = fictionNode.path("ficID").asInt();
                            
                        }
                    }
                }                
        } catch (Exception e) {
            // TODO: handle exception
        }
        return ficId;
    }

    public String getFicLink(String ficName) {
        String ficLink = "";
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)){ // Translates the text to UTF_8
            
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
            JsonNode fictionsArray = rootNode.path("fictions");
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("title").asText().equals(ficName)) {
                            ficLink = fictionNode.path("ficLink").asText();
                            
                        }
                    }
                }                
        } catch (Exception e) {
            // TODO: handle exception
        }
        return ficLink;
    }


    public String setFicChapter(String ficName, int chapter) {
        String filePath = "scraper/src/main/resources/fics.json";
        boolean found = false;
        String returnStatement;

        try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)){ // Translates the text to UTF_8
            
            JsonNode rootNode = objectMapper.readTree(inputStreamReader);
            JsonNode fictionsArray = rootNode.path("fictions");
            
            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    if (fictionNode.path("title").asText().equals(ficName)) {
                            //int ficId = fictionNode.path("ficID").asInt();
                            //FicScraper ficScraper2 = new FicScraper(getFicLink(ficId));
                            ((ObjectNode) fictionNode).put("chapAmount", chapter);
                            found = true;
                        }
                    }

                    if (found) {
                        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath))) {
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileOutputStream, rootNode);
                            
                        } 
                        returnStatement = "Chapter amount succesfully updated to: " + chapter;
                    } else {
                        returnStatement = "Fic with specified name not found";
                    }

                } else {
                    returnStatement = String.format("ChapAmount not found.");
                }

        } catch (IOException e) {
            e.printStackTrace();        
            returnStatement = "An error occurred while updating the file.";

        }
        return returnStatement; 
    }
    
    public String getChapAmountInJSON(int ficNumber) {
        String returnStatement = "";

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStream);
           
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
       
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)){ // Translates the text to UTF_8
            
            JsonNode rootNode = objectMapper.readTree(inputStream);

            JsonNode fictionsArray = rootNode.path("fictions");
                if (fictionsArray.isArray()) {
                    for (JsonNode fictionNode : fictionsArray) {
                        if (fictionNode.path("ficID").asInt() == ficNumber) {
                            returnStatement = fictionNode.path("ficLink").asText(); 
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

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStream);
           
            JsonNode fictionsArray = rootNode.path("fictions");
                if (fictionsArray.isArray()) {
                    for (JsonNode fictionNode : fictionsArray) {
                        if (fictionNode.path("ficID").asInt() == ficNumber) {
                            returnStatement = fictionNode.path("title").asText(); 
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

    public boolean matchFicTitle(String fitTitle) {
        boolean titleFound = false;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)){ // Translates the text to UTF_8
        
            JsonNode rootNode = objectMapper.readTree(inputStream);
           
            JsonNode fictionsArray = rootNode.path("fictions");
                if (fictionsArray.isArray()) {
                    for (JsonNode fictionNode : fictionsArray) {
                        if (fictionNode.path("title").asText().equals(fitTitle)) {
                            titleFound = true; 
                            System.out.println("Matching fic title found.");
                        }
                    }
                } else {
                    System.out.println("No matching title found.");
                }

        } catch (IOException e) {
            e.printStackTrace();        
        }
        return titleFound; 
    }

}
