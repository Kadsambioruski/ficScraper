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

    public void updateFiction(Fiction fiction) {
        SiteScraper scraper = ScraperFactory.forFic(fiction.getSite());

        int currentChapCount = scraper.getChapterCount(fiction);

        if (currentChapCount < 0) {
            log.warn(
                "Could not get chapter count for {}",
                fiction.getTitle()
            );
            return;
        }

        int storedChapCount =
            Config.chapterStorage().getForFiction(fiction.getId()).size();

        log.info(
            "{}: site={}, stored={}, recorded={}",
            fiction.getTitle(),
            currentChapCount,
            storedChapCount,
            fiction.getChapAmount()
        );

        if (currentChapCount == fiction.getChapAmount()
                && storedChapCount >= currentChapCount) {
            log.debug(
                "No new or missing chapters for {}",
                fiction.getTitle()
            );
            return;
        }

        if (currentChapCount < fiction.getChapAmount()) {
            log.warn(
                "Chapter count for {} decreased from {} to {}",
                fiction.getTitle(),
                fiction.getChapAmount(),
                currentChapCount
            );
        }

        List<ChapterInfo> chapters = scraper.getChapters(fiction);

        if (chapters == null || chapters.isEmpty()) {
            log.warn(
                "Could not retrieve chapters for {}",
                fiction.getTitle()
            );
            return;
        }

        log.info(
            "{}: retrieved {} chapter entries, stored {}",
            fiction.getTitle(),
            chapters.size(),
            storedChapCount
        );

        scrapeMissingChapters(fiction, chapters);
    }


    public void scrapeMissingChapters(Fiction fiction, List<ChapterInfo> chapters) {
        List<Chapter> storedChapters = Config.chapterStorage().getForFiction(fiction.getId());

        int currentChapCount = chapters.size();

        int updatedChapAmount = Math.max(fiction.getChapAmount(), currentChapCount);
        fiction.setChapAmount(updatedChapAmount);
        Config.ficJsonHandler().updateChapAmount(fiction, currentChapCount);

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
