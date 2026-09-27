package com.example;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.model.Chapter;
import com.example.model.ChapterInfo;
import com.example.model.Fiction;
import com.example.scrape.ScraperFactory;
import com.example.scrape.Site;
import com.example.scrape.SiteScraper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FicScraper {
    private static final Logger log = LoggerFactory.getLogger(FicScraper.class);
    private static final Set<UUID> updatedFics = ConcurrentHashMap.newKeySet();

    public Fiction ficInformation(String ficUrl) {
        Site site = Site.fromUrl(ficUrl);
        SiteScraper scraper = ScraperFactory.forFic(site);
        Fiction fic = scraper.scrapeInfo(ficUrl);
        if (fic == null) {
            throw new RuntimeException("Failed to scrape story: " + ficUrl);
        }
        return fic;
    }

    public boolean checkUpdatedChap(Fiction fiction, int currentChapCount){
        if (fiction.getChapAmount() < currentChapCount) {
            updatedFics.add(fiction.getId());
            return true;
        }
        return false;
    }

    public Fiction addFiction(String ficUrl) {
        Fiction fiction = ficInformation(ficUrl);
        Config.ficJsonHandler().addFic(fiction);
        scrapeAllChapters(fiction);
        
        return fiction;
    }

    public void scrapeAllChapters(Fiction fiction) {
        SiteScraper scraper = ScraperFactory.forFic(fiction.getSite());
        List<ChapterInfo> chapters = scraper.getChapters(fiction);

        for (int i = 0; i < chapters.size(); i++) {
            ChapterInfo chapter = chapters.get(i);
            scrapeChapter(fiction, i + 1, chapter.getUrl(), chapter.getTitle());
        }
    }

    public void scrapeMissingChapters(Fiction fiction) {
        SiteScraper scraper = ScraperFactory.forFic(fiction.getSite());
        List<ChapterInfo> chapters = scraper.getChapters(fiction);
        List<Chapter> storedChapters = Config.chapterStorage().getForFiction(fiction.getId());

        Set<String> storedNames = storedChapters.stream()
            .map(Chapter::getTitle)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (int i = 0; i < chapters.size(); i++) {
            ChapterInfo chapter = chapters.get(i);

            if (storedNames.contains(chapter.getTitle())) {
                continue;
            }

            scrapeChapter(
                fiction,
                i + 1,
                chapter.getUrl(),
                chapter.getTitle()
            );
        }
    }

    public Chapter scrapeChapter(Fiction fiction, int chapterNumber, String chapterUrl, String chapterTitle) {
        SiteScraper scraper = ScraperFactory.forFic(fiction.getSite());
        String content = scraper.getChapterContent(chapterUrl);

        Chapter chapter = new Chapter(
            fiction.getId(),
            chapterNumber,
            chapterTitle,
            chapterUrl,
            content
        );

        Config.chapterStorage().save(chapter);
        return chapter;
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

    public static void clearFicUpdate(UUID ficId) {
        updatedFics.remove(ficId);
    }

    public String nextChapFicLink(Fiction fiction) {
       return ScraperFactory.forFic(fiction.getSite()).nextChapterLink(fiction);
    }

    public int getWordCount(Fiction fiction) {
        return ScraperFactory.forFic(fiction.getSite()).getWordCount(fiction);
    }
}
