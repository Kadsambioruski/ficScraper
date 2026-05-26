package com.example.scrape;
import java.util.List;

import com.example.model.Fiction;

public interface SiteScraper {
    Site supportedSite();
    Fiction scrapeInfo(String url);
    List<String> getChapterNames(Fiction fiction);
    List<String> getChapterLinks(Fiction fiction);
    String nextChapterLink(Fiction fiction);
    int getWordCount(Fiction fiction);
}
