package com.example.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.example.Config;
import com.example.model.Fiction;
import com.example.model.FictionList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSerializer {
    private static final Logger log = LoggerFactory.getLogger(JsonSerializer.class);
    private final ObjectMapper objectMapper = Config.objectMapper();
    private final Path readingFicsPath;
    private final Path finishedFicsPath;

    public JsonSerializer(){
        this.readingFicsPath = Config.ficsJsonPath();
        this.finishedFicsPath = Config.finishedFicsJsonPath();
    }

    public void saveFicToJson(Fiction fiction) {
        try {
            File readingFile = readingFicsPath.toFile();

            FictionList readingList = readingFile.exists() && readingFile.length() != 0
                ? objectMapper.readValue(readingFile, FictionList.class)
                : new FictionList();

            readingList.addFiction(fiction);

            ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
            objectWriter.writeValue(readingFile, readingList);

            log.info("New data appended and saved to file: {}", readingFicsPath);
        } catch (Exception e) {
            log.error("Failed to save fic to JSON", e);
        }
    }

    public void saveFicList(List<Fiction> fictions) {
        try {
            FictionList list = new FictionList();
            list.setFictions(new ArrayList<>(fictions));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(readingFicsPath.toFile(), list);
        } catch (Exception e) {
            log.error("Failed to save fic list", e);
        }
    }

    public void moveFicToFinished(Fiction fiction) {
        try {
            File readingFile = readingFicsPath.toFile();
            File finishedFile = finishedFicsPath.toFile();

            FictionList readingList = readingFile.exists() && readingFile.length() != 0
                ? objectMapper.readValue(readingFile, FictionList.class)
                : new FictionList();

            FictionList finishedList = finishedFile.exists() && finishedFile.length() != 0
                ? objectMapper.readValue(finishedFile, FictionList.class)
                : new FictionList();

            Fiction fictionToMove = readingList.getFiction(fiction.getId());
            if (fictionToMove != null) {
                readingList.removeFiction(fictionToMove);

                finishedList.addFiction(fictionToMove);

                ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
                objectWriter.writeValue(readingFile, readingList);
                objectWriter.writeValue(finishedFile, finishedList);

                log.info("Moved fiction to finished: {} ID: {}", fiction.getTitle(), fiction.getId());
            } else {
                log.warn("Fiction not found in reading list: {} ID: {}", fiction.getTitle(), fiction.getId());
            }
        } catch (IOException e) {
            log.error("Failed to move fic to finished", e);
        }
    }

    public void updateChapAmount(Fiction updatedFic, int newChapAmount) {
        try {
            File readingFile = readingFicsPath.toFile();
            FictionList readingList = objectMapper.readValue(readingFile, FictionList.class);

            for (Fiction fiction : readingList.getFictions()) {
                if (fiction.getId().equals(updatedFic.getId())) {
                    fiction.setChapAmount(newChapAmount);
                    break;
                }
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(readingFile, readingList);
        } catch (Exception e) {
            log.error("Failed to update chapter amount for {}", updatedFic.getTitle(), e);
        }
    }

}
