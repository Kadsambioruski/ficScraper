package com.example.storage;

import com.example.model.Chapter;

import java.util.List;
import java.util.UUID;

public interface ChapterStorage {
    void save(Chapter chapter);
    Chapter get(UUID id);
    List<Chapter> getForFiction(UUID fictionId);
    void delete(UUID id);
}