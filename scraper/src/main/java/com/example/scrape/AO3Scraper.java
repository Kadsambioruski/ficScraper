package com.example.scrape;

import com.example.model.ChapterInfo;
import com.example.model.Fiction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import com.example.ChapterHtmlCleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AO3Scraper implements SiteScraper {
    private static final Logger log = LoggerFactory.getLogger(AO3Scraper.class);

    public Site supportedSite() { return Site.AO3; }

    public Fiction scrapeInfo(String url) {
        Fiction fic = null;
        try {
            String workUrl = normalizeWorkUrl(url);
            Document document = AO3BrowserFetch.fetch(workUrl);

            String title = document.select("h2.title heading").text();
            String author = document.select("a[rel=\"author\"]").text();
            String description = document.select("div.summary blockquote.userstuff").text();

            String chaptersText = document.select("dd.chapters").text();
            int chapterAmount = Integer.parseInt(chaptersText.split("/")[0]);

            int wordCount = extractNumber(document.select("dd.words").text());

            log.debug("Scraped AO3 fic - Title: {}, Author: {}, Chapters: {}, Words: {}", title, author, chapterAmount, wordCount);

            fic = new Fiction(url, Site.AO3, title, author, chapterAmount, 0, wordCount, description);
            return fic;
        } catch (Exception e) {
            log.error("Failed to scrape info from {}", url, e);
        }
        return null;
    }
    

    public int getChapterCount(Fiction fiction){
        int chapterCount;
        try {
            Document document = AO3BrowserFetch.fetch(fiction.getFicLink());
            String chaptersText = document.select("dd.chapters").text();
            chapterCount = Integer.parseInt(chaptersText.split("/")[0]);
            
        } catch (Exception e) {
            log.error("Failed to get chapter count for {}", fiction.getFicLink(), e);
            chapterCount = -1;        
        }
    
        return chapterCount;
    }


    @Override
    public String getChapterContent(String chapterUrl) {
        try {
            Document document = AO3BrowserFetch.fetch(chapterUrl);

            Element chapterBody =
                document.selectFirst("#chapters .chapter .userstuff");

            if (chapterBody == null || chapterBody.text().isBlank()) {
                throw new IOException(
                    "Chapter content missing or page did not load: "
                        + chapterUrl
                );
            }

            return ChapterHtmlCleaner.clean(chapterBody.html());

        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to fetch AO3 chapter: " + chapterUrl, e
            );
        }
    }

    public List<String> getChapterNames(Fiction fiction) {
        List<String> allChapterNames = null;
        try {
            Document document = AO3BrowserFetch.fetch(fiction.getFicLink());
            log.debug("Fetching chapter names for: {}", fiction.getFicLink());
            Elements allChapters = document.select("select#selected_id option");

            allChapterNames = allChapters
                .stream()
                .map(Element::text)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get chapter names for {}", fiction.getFicLink(), e);
            return Collections.emptyList();
        }
        return allChapterNames;
    }

    public List<String> getChapterLinks(Fiction fiction) {
        List<String> allChapterLinks = null;
        try {
            Document document = AO3BrowserFetch.fetch(fiction.getFicLink());
            Elements allChapters = document.select("select#selected_id option");

            String ficLink = fiction.getFicLink();
            String workId = ficLink.split("/works/")[1].split("/")[0];
            String baseUrl = ficLink.split("/works/")[0] + "/works/" + workId;

            allChapterLinks = allChapters
            .stream()
            .map(chapter -> chapter.attr("value"))
            .filter(value -> value != null && !value.isEmpty())
            .map(value -> baseUrl + "/chapters/" + value)
            .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to get chapter links for {}", fiction.getFicLink(), e);
            return Collections.emptyList();
        }
        return allChapterLinks;
    }

    public String nextChapterLink(Fiction fiction) {
        String chapterLink = null;
        try {
            List<String> allChapterLinks = getChapterLinks(fiction);

            int storedChapAmount = fiction.getChapAmount();
            int scrapedChapAmount = allChapterLinks.size();

            if (scrapedChapAmount > storedChapAmount) {
                chapterLink = allChapterLinks.get(storedChapAmount);
                log.debug("Next chapter found: {}", chapterLink);
            } else if (scrapedChapAmount < storedChapAmount) {
                chapterLink = allChapterLinks.get(scrapedChapAmount - 1);
                log.warn("Fiction may be stubbed. Using latest available: {}", chapterLink);
            } else {
                log.debug("No new chapter found.");
            }

        } catch (Exception e) {
            log.error("Failed to find next chapter link for {}", fiction.getFicLink(), e);
        }
        return chapterLink;
    }

    public int getWordCount(Fiction fiction) {
        try {
            Document document = AO3BrowserFetch.fetch(fiction.getFicLink());
            int wordCount = extractNumber(document.select("dd.words").text());

            log.debug("Word count for '{}': {}", fiction.getTitle(), wordCount);
            return wordCount;
        } catch (Exception e) {
            log.error("Failed to get word count for {}", fiction.getFicLink(), e);
        }
        return 0;
    }

    private static String normalizeWorkUrl(String url) {
        if (url.contains("/chapters/")) {
            url = url.substring(0, url.indexOf("/chapters/"));
        }

        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        return url;
    }

    private int extractNumber(String text) {
        return Integer.parseInt(text.replace(",", "").trim());
    }

    @Override
    public List<ChapterInfo> getChapters(Fiction fiction) {
        try {
            Document document = AO3BrowserFetch.fetch(fiction.getFicLink());
            Elements allChapters = document.select("select#selected_id option");

            String ficLink = normalizeWorkUrl(fiction.getFicLink());
            String workId = ficLink.split("/works/")[1].split("/")[0];
            String baseUrl = ficLink.split("/works/")[0] + "/works/" + workId;

            return allChapters.stream()
                .map(option -> {
                    String value = option.attr("value");
                    String title = option.text();
                    String url = baseUrl + "/chapters/" + value;

                    return new ChapterInfo(title, url);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get chapters for {}", fiction.getFicLink(), e);
            return List.of();
        }
    }

}
