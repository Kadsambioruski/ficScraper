package com.example;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.model.Fiction;
import com.example.scrape.ScraperFactory;
import com.example.scrape.Site;
import com.example.scrape.SiteScraper;
import com.example.storage.FicJsonHandler;


public class FicScraper { 
    private static final FicJsonHandler ficJsonHandler = Config.ficJsonHandler();
    private static final Set<Integer> updatedFics = new HashSet<>();

    public Fiction ficInformation(String ficUrl) {
        Site site = Site.fromUrl(ficUrl);
        SiteScraper scraper = ScraperFactory.forFic(site);
        Fiction fic = scraper.scrapeInfo(ficUrl);
        if (fic == null) {
            throw new RuntimeException("Failed to scrape story: " + ficUrl);
        }
        fic.setFicId(ficJsonHandler.getAllFics().size() + 1);
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
        System.out.println("Checking if " + fiction.getTitle() + " is stubbed");
        return fiction.getChapAmount() > currentChapCount;
    }
    
    public static List<Fiction> getUpdatedFics() {
        return updatedFics.stream()
            .map(id -> ficJsonHandler.getFic(id))
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
}



