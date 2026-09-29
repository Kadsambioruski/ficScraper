package com.example.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 
    public void removeFic(Fiction fiction) {
        try {
            File readingFile = readingFicsPath.toFile();
            File finishedFile = finishedFicsPath.toFile();

            FictionList readingList = objectMapper.readValue(readingFile, FictionList.class);
            FictionList finishedList = objectMapper.readValue(finishedFile, FictionList.class);

            Fiction fictionToRemove = readingList.getFiction(fiction.getId());
            File fileToUpdate = readingFile;
            FictionList listToUpdate = readingList;

            if (fictionToRemove == null) {
                fictionToRemove = finishedList.getFiction(fiction.getId());
                fileToUpdate = finishedFile;
                listToUpdate = finishedList;
            }

            if (fictionToRemove != null) {
                listToUpdate.removeFiction(fictionToRemove);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileToUpdate, listToUpdate);
                log.info("Removed fiction: {} ID: {}", fiction.getTitle(), fiction.getId());
            } else {
                log.warn("Fiction not found: {} ID: {}", fiction.getTitle(), fiction.getId());
            }
        } catch (IOException e) {
            log.error("Failed to remove fiction: {}", fiction.getTitle(), e);
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
            FictionList fictionList = objectMapper.readValue(
                readingFicsPath.toFile(),
                FictionList.class
            );

            Fiction fiction = fictionList.getFiction(updatedFic.getId());

            if (fiction != null) {
                int currentChapAmount = fiction.getChapAmount();
                int updatedChapAmount = Math.max(currentChapAmount, newChapAmount);

                fiction.setChapAmount(updatedChapAmount);

                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(readingFicsPath.toFile(), fictionList);

                log.info(
                    "Updated chapter amount for {} from {} to {}",
                    fiction.getTitle(),
                    currentChapAmount,
                    updatedChapAmount
                );
            }
        } catch (IOException e) {
            log.error(
                "Failed to update chapter amount for {}",
                updatedFic.getTitle(),
                e
            );
        }
    }

    public synchronized void updateReaderProgress(Fiction updatedFic, int readerProgress) {
        Path tempPath = readingFicsPath.resolveSibling(
            readingFicsPath.getFileName() + ".tmp"
        );

        try {
            FictionList fictionList = objectMapper.readValue(
                readingFicsPath.toFile(),
                FictionList.class
            );

            Fiction fiction = fictionList.getFiction(updatedFic.getId());

            if (fiction == null) {
                return;
            }

            int updatedProgress = Math.max(
                fiction.getReaderProgress(),
                readerProgress
            );

            fiction.setReaderProgress(updatedProgress);

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(tempPath.toFile(), fictionList);

            try {
                Files.move(
                    tempPath,
                    readingFicsPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                    tempPath,
                    readingFicsPath,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }

            log.info(
                "Updated reader progress for {} to {}",
                fiction.getTitle(),
                updatedProgress
            );
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }

            log.error(
                "Failed to update reader progress for {}",
                updatedFic.getTitle(),
                e
            );
        }
    }

}
