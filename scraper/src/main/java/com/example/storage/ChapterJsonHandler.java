package com.example.storage;

import com.example.Config;
import com.example.model.Chapter;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ChapterJsonHandler implements ChapterStorage {
    private static final Path CHAPTERS_DIR = Path.of("../data", "chapters");

    public ChapterJsonHandler() {
        try {
            Files.createDirectories(CHAPTERS_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create chapters directory", e);
        }
    }

    @Override
    public void save(Chapter chapter) {
        List<Chapter> chapters = getForFiction(chapter.getFictionId());

        Chapter existing = chapters.stream()
            .filter(c -> c.getTitle() != null && c.getTitle().equals(chapter.getTitle()))
            .findFirst()
            .orElse(null);

        if (existing != null) {
            chapter.setId(existing.getId());
            chapters.remove(existing);
        }

        chapters.add(chapter);
        chapters.sort(Comparator.comparingInt(Chapter::getChapterNumber));

        write(chapter.getFictionId(), chapters);
    }

    @Override
    public Chapter get(UUID id) {
        try {
            for (Path path : Files.newDirectoryStream(CHAPTERS_DIR, "*.json")) {
                List<Chapter> chapters = read(path);

                for (Chapter chapter : chapters) {
                    if (chapter.getId().equals(id)) {
                        return chapter;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to find chapter: " + id, e);
        }

        return null;
    }

    @Override
    public List<Chapter> getForFiction(UUID fictionId) {
        Path path = pathFor(fictionId);

        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        return read(path);
    }

    @Override
    public void delete(UUID id) {
        Chapter chapter = get(id);

        if (chapter == null) {
            return;
        }

        List<Chapter> chapters = getForFiction(chapter.getFictionId());
        chapters.removeIf(existing -> existing.getId().equals(id));
        write(chapter.getFictionId(), chapters);
    }

    private void write(UUID fictionId, List<Chapter> chapters) {
        try {
            Config.objectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValue(pathFor(fictionId).toFile(), chapters);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save chapters for fiction: " + fictionId, e);
        }
    }

    private List<Chapter> read(Path path) {
        try {
            return Config.objectMapper().readValue(
                path.toFile(),
                new TypeReference<List<Chapter>>() {}
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chapters: " + path, e);
        }
    }

    private Path pathFor(UUID fictionId) {
        return CHAPTERS_DIR.resolve(fictionId + ".json");
    }
}