package com.example.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import com.example.Config;
import com.example.FicScraper;
import com.example.model.ChapterInfo;
import com.example.model.Fiction;
import com.example.model.FictionList;
import com.example.scrape.ScraperFactory;
import com.example.scrape.SiteScraper;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FicJsonHandler {
    private static final Logger log = LoggerFactory.getLogger(FicJsonHandler.class);
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
    }

    public void updateChapAmount(Fiction fiction, int chapterAmount) {
        if (fiction != null) {
            jsonSerializer.updateChapAmount(fiction, chapterAmount);
            log.info("Chapter amount updated to: {}", chapterAmount);
        } else {
            log.warn("Fic with specified ID not found");
        }
    }

    public void migrateChapAmountToReaderProgress() {
        Path marker = Path.of("../data/.reader-progress-migration-complete");
        Path readingPath = Config.ficsJsonPath();
        Path finishedPath = Config.finishedFicsJsonPath();

        if (Files.exists(marker)) {
            log.info("Reader progress migration already completed.");
            return;
        }

        try {
            ObjectMapper mapper = Config.objectMapper();

            FictionList readingList = readFictionList(readingPath, mapper);
            FictionList finishedList = readFictionList(finishedPath, mapper);

            migrateFictionList(readingList);
            migrateFictionList(finishedList);

            Path readingTemp = Path.of(readingPath + ".migration-temp");
            Path finishedTemp = Path.of(finishedPath + ".migration-temp");

            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(readingTemp.toFile(), readingList);

            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(finishedTemp.toFile(), finishedList);

            backupFile(readingPath);
            backupFile(finishedPath);

            Files.move(
                readingTemp,
                readingPath,
                StandardCopyOption.REPLACE_EXISTING
            );

            Files.move(
                finishedTemp,
                finishedPath,
                StandardCopyOption.REPLACE_EXISTING
            );

            Files.createFile(marker);

            log.info("Reader progress migration completed successfully.");
        } catch (Exception e) {
            log.error("Reader progress migration failed.", e);
            throw new RuntimeException("Reader progress migration failed", e);
        }
    }

    private FictionList readFictionList(Path path, ObjectMapper mapper) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            return new FictionList();
        }

        return mapper.readValue(path.toFile(), FictionList.class);
    }

    private void migrateFictionList(FictionList list) {
        for (Fiction fiction : list.getFictions()) {
            if (fiction.getId() == null) {
                throw new IllegalStateException(
                    "Fiction has no UUID: " + fiction.getTitle()
                );
            }

            int oldReaderProgress = fiction.getChapAmount();
            int availableChapters = fiction.getChapAmount();

            if (fiction.getSite() != com.example.scrape.Site.AO3) {
                try {
                    SiteScraper scraper = ScraperFactory.forFic(fiction.getSite());
                    List<ChapterInfo> chapters = scraper.getChapters(fiction);

                    if (chapters.isEmpty()) {
                        throw new IllegalStateException(
                            "Could not determine chapter count"
                        );
                    }

                    availableChapters = chapters.size();
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Failed to migrate fiction: " + fiction.getTitle(),
                        e
                    );
                }
            } else {
                log.warn(
                    "Skipping chapter count update for AO3 fiction: {}",
                    fiction.getTitle()
                );
            }

            fiction.setReaderProgress(oldReaderProgress);
            fiction.setChapAmount(availableChapters);

            log.info(
                "Migrated {}: readerProgress={}, chapAmount={}",
                fiction.getTitle(),
                oldReaderProgress,
                availableChapters
            );
        }
    }

    private void backupFile(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            return;
        }

        Path backupPath = Path.of(path + ".reader-progress-backup");

        if (!Files.exists(backupPath)) {
            Files.copy(
                path,
                backupPath,
                StandardCopyOption.COPY_ATTRIBUTES
            );

            log.info("Created migration backup: {}", backupPath);
        }
    }

    private void migrateFile(Path path) {
        try {
            ObjectMapper mapper = Config.objectMapper();

            FictionList list = mapper.readValue(
                path.toFile(),
                FictionList.class
            );

            for (Fiction fiction : list.getFictions()) {
                if (fiction.getId() == null) {
                    fiction.setId(UUID.randomUUID());
                }
            }

            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), list);

            log.info("Migrated fiction IDs in {}", path);
        } catch (IOException e) {
            log.error("Failed to migrate {}", path, e);
        }
    }

    public Fiction getFic(UUID ficId) {
        return jsonDeserializer.getFic(ficId);
    }

    public void removeFic(Fiction fiction) {
        jsonSerializer.removeFic(fiction);
    }

    public List<Fiction> getAllFics() {
        return jsonDeserializer.getAllFics();
    }

    public List<Fiction> getFinishedFics() {
        return jsonDeserializer.getFinishedFics();
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
            log.error("Failed to calculate total read words", e);
            return 0;
        }
    }

    public void backfillWordCounts() {
        try {
            Path finishedPath = Config.finishedFicsJsonPath();
            ObjectMapper mapper = Config.objectMapper();

            FictionList finishedList = mapper.readValue(
                finishedPath.toFile(),
                FictionList.class
            );

            for (Fiction fic : finishedList.getFictions()) {
                int words = ficScraper.getWordCount(fic);
                fic.setWordCount(words);

                log.info(
                    "Updated word count for {}: {}",
                    fic.getTitle(),
                    words
                );
            }

            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(finishedPath.toFile(), finishedList);
        } catch (IOException e) {
            log.error("Failed to backfill word counts", e);
        }
    }

    public void setFicChapter(Fiction fiction, int chapter) {
        if (fiction != null) {
            jsonSerializer.updateReaderProgress(fiction, chapter);
            log.info("Reader progress updated to: {}", chapter);
        } else {
            log.warn("Fic with specified name not found");
        }
    }

    public String getFicTitle(UUID ficId) {
        Fiction fiction = getFic(ficId);

        if (fiction != null) {
            return fiction.getTitle();
        }

        return "Fiction title not found";
    }
}

