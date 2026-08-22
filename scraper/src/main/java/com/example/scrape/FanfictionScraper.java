package com.example.scrape;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;

import com.example.model.Fiction;
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

            fic = new Fiction(url, Site.FANFICTION, 0, title, author, 0, wordCount, description);
            return fic;
        } catch (Exception e) {
            log.error("Failed to scrape info from {}", url, e);
        }
        return null;
    }

    public List<String> getChapterNames(Fiction fiction) {
        List<String> allChapterNames = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            Elements allChapters = document.select("select#chap_select").first().select("option");
            allChapterNames = allChapters
                .stream()
                .map(Element::text)
                .collect(Collectors.toList());

            String metadata = document.select(".xgray.xcontrast_txt").text();
            int chapterAmount = extractNumber(metadata, "Chapters:");

            while (allChapterNames.size() < chapterAmount) {
                allChapterNames.add("Chapter: " + (allChapterNames.size() + 1));
            }

        } catch (Exception e) {
            log.error("Failed to get chapter names for {}", fiction.getFicLink(), e);
        }
        return allChapterNames;
    }

    public List<String> getChapterLinks(Fiction fiction) {
        List<String> allChapterLinks = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            Elements allChapters = document.select("select#chap_select").first().select("option");

            String ficLink = fiction.getFicLink();
            String storyId = ficLink.split("/s/")[1].split("/")[0];
            String storyTitle = ficLink.split("/s/")[1].split("/", 3)[2];

            allChapterLinks = allChapters.stream()
                .map(option -> option.attr("value"))
                .filter(value -> value != null && !value.isEmpty())
                .map(value -> "https://www.fanfiction.net/s/" + storyId + "/" + value + "/" + storyTitle)
                .collect(Collectors.toList());

            String metadata = document.select(".xgray.xcontrast_txt").text();
            int chapterAmount = extractNumber(metadata, "Chapters:");

            for (int i = allChapterLinks.size() + 1; i <= chapterAmount; i++) {
                allChapterLinks.add("https://www.fanfiction.net/s/" + storyId + "/" + i + "/" + storyTitle);
            }

        } catch (Exception e) {
            log.error("Failed to get chapter links for {}", fiction.getFicLink(), e);
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
