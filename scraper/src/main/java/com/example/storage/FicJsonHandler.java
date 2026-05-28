package com.example.storage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.example.Config;
import com.example.FicScraper;
import com.example.model.Fiction;
import com.example.model.FictionList;
import com.fasterxml.jackson.databind.ObjectMapper;


public class FicJsonHandler {
    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;
    private final FicScraper ficScraper = new FicScraper();

    public FicJsonHandler() {
        this.jsonSerializer = new JsonSerializer();
        this.jsonDeserializer = new JsonDeserializer();
    }

    public void addFic(Fiction newFic) {
        jsonSerializer.saveFicToJson(newFic);
    }

    public void moveFicToFinished(Fiction fiction) {
        int words = ficScraper.getWordCount(fiction);
        fiction.setWordCount(words);
        jsonSerializer.moveFicToFinished(fiction);
        resetFicIds();
    }

    public Fiction getFic(int ficId) {
        return jsonDeserializer.getFic(ficId);
    }
    
    public List<Fiction> getAllFics() {
       return jsonDeserializer.getAllFics();
    }

    public int getTotalReadWords() {
        try {
            Path finishedPath = Config.finishedFicsJsonPath();
            FictionList finishedList = Config.objectMapper()
                .readValue(finishedPath.toFile(), FictionList.class);
            return finishedList.getFictions().stream()
                .mapToInt(Fiction::getWordCount)
                .sum();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void backfillWordCounts() {
        try {
            Path finishedPath = Config.finishedFicsJsonPath();
            ObjectMapper mapper = Config.objectMapper();
            FictionList finishedList = mapper.readValue(finishedPath.toFile(), FictionList.class);

            for (Fiction fic : finishedList.getFictions()) {
                int words = ficScraper.getWordCount(fic);
                fic.setWordCount(words);
                System.out.println("Updated word count for " + fic.getTitle() + ": " + words);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(finishedPath.toFile(), finishedList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetFicIds() {
        List<Fiction> fictions = getAllFics();
        int i = 0;
        for (Fiction fiction : fictions) {
            fiction.setFicId(i);
            i++;
        }
        jsonSerializer.saveFicList(fictions);
    }

    public void setFicChapter(Fiction fiction, int chapter) {
        if (fiction != null) {
            jsonSerializer.updateChapAmount(fiction, chapter);
            System.out.println("Chapter amount succesfully updated to: " + chapter);
        } else {
            System.out.println("Fic with specified name not found");
        }
    }

    public String getFicTitle(int ficId) {
        Fiction fiction = getFic(ficId);
        if (fiction != null) {
            return fiction.getTitle();
        }
        return "Fiction title not found";
    }

}
