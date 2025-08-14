package com.example;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonSerializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path jsonPath;

    public JsonSerializer(){
        this.jsonPath = Paths.get(System.getProperty("user.dir"), "data", "fics.json");
        System.out.println("JSON file path: " + jsonPath.toAbsolutePath());
    }
    
    
    public void saveFicToJson(Fiction fiction) {         
        try {
            // Read existing data from the file
            FictionList listOfFictions;
            File file = jsonPath.toFile();

            if (file.exists() && file.length() != 0) {
                try (FileReader fileReader = new FileReader(file)) {
                    listOfFictions = objectMapper.readValue(fileReader, FictionList.class);
                }
            } else {
                listOfFictions = new FictionList();
            }
            
            // Append new Fiction to the list
            listOfFictions.addFiction(fiction);
            
            // Write the updated list back to the file
            try (FileWriter fileWriter = new FileWriter(file);) {
                ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
                objectWriter.writeValue(fileWriter, listOfFictions);
            }
            
            

            System.out.println("New data appended and saved to file: " + jsonPath);
        } catch (Exception e) {
            //TODO could add runtimeexception to break process if something happens during serialization
            e.printStackTrace();
        }

    }


}
