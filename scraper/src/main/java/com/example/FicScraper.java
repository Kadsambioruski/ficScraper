package com.example;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.model.Fiction;
import com.example.scrape.ScraperFactory;
import com.example.scrape.Site;
import com.example.scrape.SiteScraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FicScraper {
    private static final Logger log = LoggerFactory.getLogger(FicScraper.class);
    private static final Set<Integer> updatedFics = ConcurrentHashMap.newKeySet();

    public Fiction ficInformation(String ficUrl) {
        Site site = Site.fromUrl(ficUrl);
        SiteScraper scraper = ScraperFactory.forFic(site);
        Fiction fic = scraper.scrapeInfo(ficUrl);
        if (fic == null) {
            throw new RuntimeException("Failed to scrape story: " + ficUrl);
        }
        fic.setFicId(Config.ficJsonHandler().getAllFics().size() + 1);
        return fic;
    }

    public boolean checkUpdatedChap(Fiction fiction, int currentChapCount){
        if (fiction.getChapAmount() < currentChapCount) {
            updatedFics.add(fiction.getFicID());
            return true;
        }
        return false;
    }

    public boolean checkIfStubbed(Fiction fiction, int currentChapCount) {
        log.debug("Checking if {} is stubbed", fiction.getTitle());
        return fiction.getChapAmount() > currentChapCount;
    }

    public static List<Fiction> getUpdatedFics() {
        return updatedFics.stream()
            .map(id -> Config.ficJsonHandler().getFic(id))
            .filter(fiction -> fiction != null)
            .collect(Collectors.toList());
    }

    public static void clearFicUpdate(int ficId) {
        updatedFics.remove(ficId);
    }

    public List<String> getAllChapterLinks(Fiction fiction) {
        return ScraperFactory.forFic(fiction.getSite()).getChapterLinks(fiction);
    }

    public String nextChapFicLink(Fiction fiction) {
       return ScraperFactory.forFic(fiction.getSite()).nextChapterLink(fiction);
    }

    public List<String> getAllChapterNames(Fiction fiction) {
        return ScraperFactory.forFic(fiction.getSite()).getChapterNames(fiction);
    }

    public int getWordCount(Fiction fiction) {
        return ScraperFactory.forFic(fiction.getSite()).getWordCount(fiction);
    }
}
