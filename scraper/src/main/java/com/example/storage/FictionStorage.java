package com.example.storage;

import com.example.model.Fiction;

import java.util.List;
import java.util.UUID;

public interface FictionStorage {

    void save(Fiction fiction);

    Fiction get(UUID id);

    List<Fiction> getAll();

    List<Fiction> getFinished();

    void delete(UUID id);

    void setFinished(UUID id, boolean finished);

    void setReaderProgress(UUID id, int chapter);

    void setChapterAmount(UUID id, int chapterAmount);
}