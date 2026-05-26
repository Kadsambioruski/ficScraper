package com.example.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.example.Config;
import com.example.model.Fiction;
import com.example.model.FictionList;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonDeserializer {
    private final ObjectMapper objectMapper = Config.objectMapper();
    private final Path jsonPath;
    
    public JsonDeserializer(){
        this.jsonPath = Config.ficsJsonPath();
    }

    public Fiction getFic(int ficID) {
        List<Fiction> allFics = getAllFics();
        for (Fiction fiction : allFics) {
            if (fiction.getFicID() == ficID) {
                return fiction;
            }
        }
        return null;
    }

    public List<Fiction> getAllFics() {
        try {
            FictionList fictionList = objectMapper.readValue(jsonPath.toFile(), FictionList.class);
            return fictionList.getFictions();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
