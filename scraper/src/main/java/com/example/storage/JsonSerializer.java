package com.example.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.example.Config;
import com.example.model.Fiction;
import com.example.model.FictionList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonSerializer {
    private final ObjectMapper objectMapper = Config.objectMapper();
    private final Path readingFicsPath;
    private final Path finishedFicsPath;

    public JsonSerializer(){
        this.readingFicsPath = Config.ficsJsonPath();
        this.finishedFicsPath = Config.finishedFicsJsonPath();
    }
    
    // Make boolean for this method to determine if its going to be saved to the "finished" json file or not
    public void saveFicToJson(Fiction fiction) {         
        try {
            // Read existing data from the file
            File readingFile = readingFicsPath.toFile();

            FictionList readingList = readingFile.exists() && readingFile.length() != 0 
                ? objectMapper.readValue(readingFile, FictionList.class)
                : new FictionList(); 
            
            
            // Append new Fiction to the list
            readingList.addFiction(fiction);
            
            // Write the updated list back to the file
            ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
            objectWriter.writeValue(readingFile, readingList);

            System.out.println("New data appended and saved to file: " + readingFicsPath);
        } catch (Exception e) {
            //TODO could add runtimeexception to break process if something happens during serialization
            e.printStackTrace();
        }

    }

    public void moveFicToFinished(Fiction fiction) {
        // Read through fictionlist to check that it is actually there, if it is remove it from there and move it to finished with all the information
        try {
            // Read existing data from the file
            File readingFile = readingFicsPath.toFile();
            File finishedFile = finishedFicsPath.toFile();

            FictionList readingList = readingFile.exists() && readingFile.length() != 0 
                ? objectMapper.readValue(readingFile, FictionList.class)
                : new FictionList(); 

            FictionList finishedList = finishedFile.exists() && finishedFile.length() != 0
                ? objectMapper.readValue(finishedFile, FictionList.class)
                : new FictionList(); 

                Fiction fictionToMove = readingList.getFiction(fiction.getFicID());
            if (fictionToMove != null) {
                readingList.removeFiction(fictionToMove);

                finishedList.addFiction(fictionToMove);

                ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
                objectWriter.writeValue(readingFile, readingList);
                objectWriter.writeValue(finishedFile, finishedList);

                System.out.println("Moved fiction to finished: " + fiction.getTitle() + " ID: " + fiction.getFicID());
            } else {
                System.out.println("Fiction not found in reading list: " + fiction.getTitle() + " ID: " + fiction.getFicID());
            }
        } catch (IOException e) {
            //TODO could add runtimeexception to break process if something happens during serialization
            e.printStackTrace();
        }
    }


}
