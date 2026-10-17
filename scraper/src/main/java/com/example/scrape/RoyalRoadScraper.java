package com.example.scrape;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.example.ChapterHtmlCleaner;
import com.example.Config;
import com.example.model.ChapterInfo;
import com.example.model.Fiction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoyalRoadScraper implements SiteScraper {
    private static final Logger log = LoggerFactory.getLogger(RoyalRoadScraper.class);

    public Site supportedSite() { return Site.ROYAL_ROAD; }

    public Fiction scrapeInfo(String url) {
        Fiction fic = null;
        try {
            Document document = Config.fetch(url);

            Elements titleAndAuthorContainer = document.select(".col");

            String title = titleAndAuthorContainer.select("h1").text();
            String author = titleAndAuthorContainer.select("h4 > span > a").text();
            String chapterText = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();
            int chapterAmount = Integer.parseInt(chapterText.split(" ")[0]);
            int wordCount = extractWordCount(document);

            StringBuilder descriptionBuilder = new StringBuilder();
            Elements descContainer = document.select("div.description");
            for (Element text : descContainer.select("div.hidden-content")) {
                String paragraph = text.select("p").text();
                descriptionBuilder.append(paragraph);
            }
            String description = descriptionBuilder.toString().trim();

            log.debug("Scraped RoyalRoad fic - Title: {}, Author: {}, Chapters: {}, Words: {}", title, author, chapterAmount, wordCount);

            fic = new Fiction(url, Site.ROYAL_ROAD, title, author, chapterAmount, 0, wordCount, description);
            return fic;

        } catch (Exception e) {
            log.error("Failed to scrape info from {}", url, e);
        }
        return null;
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

    public int getChapterCount(Fiction fiction){
        try {
            Document document = Config.fetch(fiction.getFicLink());
            String chapterText = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();
            
            if (chapterText.isBlank()) {
                log.warn("Could not find Royal Road chapter count for {}",
                    fiction.getFicLink()
                );
                return -1;
            }
            String firstPart = chapterText.split("\\s+")[0];
            return Integer.parseInt(firstPart);

        } catch (Exception e) {
            log.error("Failed to get chapter count for {}", fiction.getFicLink(), e);
            return -1;        }
    }


    @Override
    public List<ChapterInfo> getChapters(Fiction fiction) {
        try {
            Document document = Config.fetch(fiction.getFicLink());
            Elements allChapters = document.select("table#chapters tbody tr");

            return allChapters.stream()
                .map(chapter -> {
                    String title = chapter.select("a").first().text();
                    String url = chapter.attr("data-url");

                    if (url.startsWith("/")) {
                        url = "https://www.royalroad.com" + url;
                    }

                    return new ChapterInfo(title, url);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get chapters for {}", fiction.getFicLink(), e);
            return List.of();
        }
    }

    @Override
    public String getChapterContent(String chapterUrl) {
        try {
            Document document = Config.fetch(chapterUrl);

            Element chapterBody = document.selectFirst("div.chapter-inner");

            if (chapterBody == null) {
                throw new RuntimeException(
                    "Could not find chapter content: " + chapterUrl
                );
            }

            return ChapterHtmlCleaner.clean(chapterBody.html());

        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to fetch chapter: " + chapterUrl,
                e
            );
        }
    }

    public int getWordCount(Fiction fiction) {
        int wordCount = 0;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            wordCount = extractWordCount(document);

            log.debug("Word count for '{}': {}", fiction.getTitle(), wordCount);
            return wordCount;
        } catch (Exception e) {
            log.error("Failed to get word count for {}", fiction.getFicLink(), e);
        }
        return wordCount;
    }

    private int extractWordCount(Document document) {
        Element pagesLi = document.selectFirst("ul.list-unstyled li:contains(Pages)");

        if (pagesLi == null) return 0;

        Element iTag = pagesLi.selectFirst("i.popovers");

        if (iTag == null) return 0;

        String dataContent = iTag.attr("data-content");
        Pattern pattern = Pattern.compile("calculated from ([0-9,]+) words");
        Matcher matcher = pattern.matcher(dataContent);

        if (matcher.find()) {
            String wordCountStr = matcher.group(1).replaceAll(",", "");
            return Integer.parseInt(wordCountStr);
        }
        return 0;
    }
}
