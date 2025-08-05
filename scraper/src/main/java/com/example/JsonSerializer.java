package com.example;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonSerializer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonSerializer(){}
    
    
    public void saveFicToJson(Fiction fiction) { 
        String filePath = "scraper\\src\\main\\java\\com\\example\\data\\fics.json";
        
        try {
            // Read existing data from the file
            FictionList listOfFictions;
            File file = new File(filePath);
            if (file.exists() && file.length() != 0) {
                FileReader fileReader = new FileReader(file);
                listOfFictions = objectMapper.readValue(fileReader, FictionList.class);
                fileReader.close();
            } else {
                listOfFictions = new FictionList();
            }
            
            // Append new Fiction to the list
            listOfFictions.addFiction(fiction);
            
            // Write the updated list back to the file
            FileWriter fileWriter = new FileWriter(filePath);
            ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();

            objectWriter.writeValue(fileWriter, listOfFictions);
            fileWriter.close();

            System.out.println("New data appended and saved to file: " + filePath);
        } catch (Exception e) {
            //TODO could add runtimeexception to break process if something happens during serialization
            e.printStackTrace();
        }

    }


}
