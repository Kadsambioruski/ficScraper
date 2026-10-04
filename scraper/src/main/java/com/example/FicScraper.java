package com.example;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        Config.fictionStorage().save(fiction);
        scrapeAllChapters(fiction);
        
        return fiction;
    }

    public void scrapeAllChapters(Fiction fiction) {
        SiteScraper scraper = ScraperFactory.forFic(fiction.getSite());
        List<ChapterInfo> chapters = scraper.getChapters(fiction);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < chapters.size(); i++) {
            ChapterInfo chapter = chapters.get(i);
            int chapterNumber = i + 1;

            executor.submit(() -> {
                scrapeChapter(
                    fiction,
                    chapterNumber,
                    chapter.getUrl(),
                    chapter.getTitle()
                );
            });
        }

        executor.shutdown();

        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Chapter scraping interrupted", e);
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
        List<Chapter> storedChapters =
            Config.chapterStorage().getForFiction(fiction.getId());

        int nextChapterNumber = storedChapters.size() + 1;

        for (ChapterInfo chapter : chapters) {
            boolean exists = storedChapters.stream()
                .anyMatch(stored ->
                    sameRoyalRoadChapter(stored.getUrl(), chapter.getUrl())
                    || Objects.equals(stored.getTitle(), chapter.getTitle())
                );

            if (exists) {
                continue;
            }

            scrapeChapter(
                fiction,
                nextChapterNumber,
                chapter.getUrl(),
                chapter.getTitle()
            );

            nextChapterNumber++;
        }

        int newStoredChapCount =
            Config.chapterStorage().getForFiction(fiction.getId()).size();

        int updatedChapAmount = Math.max(
            fiction.getChapAmount(),
            newStoredChapCount
        );

        fiction.setChapAmount(updatedChapAmount);

        Config.fictionStorage().setChapterAmount(
            fiction.getId(),
            updatedChapAmount
        );
    }

    private boolean sameRoyalRoadChapter(String storedUrl, String newUrl) {
        String storedId = extractRoyalRoadChapterId(storedUrl);
        String newId = extractRoyalRoadChapterId(newUrl);

        return storedId != null
            && newId != null
            && storedId.equals(newId);
    }

    private String extractRoyalRoadChapterId(String url) {
        if (url == null) {
            return null;
        }

        String marker = "/chapter/";
        int start = url.indexOf(marker);

        if (start == -1) {
            return null;
        }

        start += marker.length();

        int end = url.indexOf("/", start);

        if (end == -1) {
            end = url.length();
        }

        String chapterId = url.substring(start, end);

        return chapterId.isBlank() ? null : chapterId;
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
        log.info(
            "Saving chapter: number={}, title={}, url={}",
            chapterNumber,
            chapterTitle,
            chapterUrl
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
            .map(id -> Config.fictionStorage().get(id))
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
