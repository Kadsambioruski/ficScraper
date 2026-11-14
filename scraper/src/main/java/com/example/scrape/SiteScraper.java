package com.example.scrape;
import java.util.List;

import com.example.model.ChapterInfo;
import com.example.model.Fiction;

public interface SiteScraper {
    Site supportedSite();
    Fiction scrapeInfo(String url);
    String nextChapterLink(Fiction fiction);
    int getWordCount(Fiction fiction);
    String getChapterContent(String chapterUrl);
    int getChapterCount(Fiction fiction);
    List<ChapterInfo> getChapters(Fiction fiction);
}
