package com.example.scrape;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;

import com.example.model.ChapterInfo;
import com.example.model.Fiction;
import com.example.ChapterHtmlCleaner;
import com.example.Config;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FanfictionScraper implements SiteScraper {
    private static final Logger log = LoggerFactory.getLogger(FanfictionScraper.class);

    public Site supportedSite() { return Site.FANFICTION; }

    public Fiction scrapeInfo(String url) {
        Fiction fic = null;
        try {
            Document document = Config.fetch(url);

            String title = document.select("b.xcontrast_txt").text();
            String author = document.select("#profile_top a.xcontrast_txt").first().text();
            String description = document.select("#profile_top div.xcontrast_txt").first().text();
            String metadata = document.select(".xgray.xcontrast_txt").text();
            int chapterAmount = extractNumber(metadata, "Chapters:");
            int wordCount = extractNumber(metadata, "Words:");

            log.debug("Scraped FanFiction fic - Title: {}, Author: {}, Chapters: {}, Words: {}", title, author, chapterAmount, wordCount);

            fic = new Fiction(url, Site.FANFICTION, title, author, chapterAmount, 0, wordCount, description);
            return fic;
        } catch (Exception e) {
            log.error("Failed to scrape info from {}", url, e);
        }
        return null;
    }

    @Override
    public String getChapterContent(String chapterUrl) {
        try {
            Document document = Config.fetch(chapterUrl);
            Element chapterBody = document.selectFirst("div#storytext");

            if (chapterBody == null) {
                throw new RuntimeException("Could not find chapter content: " + chapterUrl);
            }

            return ChapterHtmlCleaner.clean(chapterBody.html());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch chapter: " + chapterUrl, e);
        }
    }

    @Override
    public int getChapterCount(Fiction fiction) {
        try {
            Document document = Config.fetch(fiction.getFicLink());

            String metadata = document
                .select(".xgray.xcontrast_txt")
                .text();

            int chapterCount = extractNumber(metadata, "Chapters:");

            if (chapterCount <= 0) {
                log.warn(
                    "Could not determine FanFiction chapter count for {}",
                    fiction.getFicLink()
                );
                return -1;
            }

            return chapterCount;
        } catch (Exception e) {
            log.error(
                "Failed to get chapter count for {}",
                fiction.getFicLink(),
                e
            );
            return -1;
        }
    }

    @Override
    public List<ChapterInfo> getChapters(Fiction fiction) {
        try {
            Document document = Config.fetch(fiction.getFicLink());
            Elements allChapters = document.select("select#chap_select").first().select("option");

            String ficLink = fiction.getFicLink();
            String storyId = ficLink.split("/s/")[1].split("/")[0];
            String storyTitle = ficLink.split("/s/")[1].split("/", 3)[2];

            List<ChapterInfo> chapters = allChapters.stream()
                .map(option -> {
                    String value = option.attr("value");
                    String title = option.text();
                    String url = "https://www.fanfiction.net/s/" + storyId + "/" + value + "/" + storyTitle;
                    return new ChapterInfo(title, url);
                })
                .collect(Collectors.toList());

            return chapters;
        } catch (Exception e) {
            log.error("Failed to get chapters for {}", fiction.getFicLink(), e);
            return List.of();
        }
    }

    public String nextChapterLink(Fiction fiction) {
        try {
            List<ChapterInfo> chapters = getChapters(fiction);
            int storedChapAmount = fiction.getChapAmount();
            int scrapedChapAmount = chapters.size();

            if (scrapedChapAmount > storedChapAmount) {
                String chapterLink = chapters.get(storedChapAmount).getUrl();
                log.debug("Next chapter found: {}", chapterLink);
                return chapterLink;
            }

            if (scrapedChapAmount < storedChapAmount) {
                String chapterLink = chapters.get(scrapedChapAmount - 1).getUrl();
                log.warn("Fiction may be stubbed. Using latest available: {}", chapterLink);
                return chapterLink;
            }

            log.debug("No new chapter found.");
        } catch (Exception e) {
            log.error("Failed to find next chapter link for {}", fiction.getFicLink(), e);
        }

        return null;
    }

    public int getWordCount(Fiction fiction) {
        try {
            Document document = Config.fetch(fiction.getFicLink());
            String metadata = document.select(".xgray.xcontrast_txt").text();

            int wordCount = extractNumber(metadata, "Words:");

            log.debug("Word count for '{}': {}", fiction.getTitle(), wordCount);
            return wordCount;
        } catch (Exception e) {
            log.error("Failed to get word count for {}", fiction.getFicLink(), e);
        }
        return 0;
    }

    private int extractNumber(String text, String key) {
        Pattern pattern = Pattern.compile(key + "\\s*([\\d,]+)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        }

        return 0;
    }

}
