package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonDeserializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path jsonPath;
    public JsonDeserializer(){
        this.jsonPath = Paths.get(System.getProperty("user.dir"), "data", "fics.json");
    }

    public JsonNode readJsonFile() {
        try (FileInputStream fileInputStream = new FileInputStream(jsonPath.toFile())) { // Translates the text to UTF_8
            return objectMapper.readTree(fileInputStream);
            
        } catch (IOException e) {
            e.printStackTrace(); 
            return null;       
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
                            fictionNode.path("chapAmount").asInt(),
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
                        fictionNode.path("chapAmount").asInt(),
                        fictionNode.path("description").asText()
                    )); 
                }
            } 
        return fictionList;
    }

}
