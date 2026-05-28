package com.example.scrape;

import com.example.model.Fiction;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import com.example.Config;

public class AO3Scraper implements SiteScraper {
    public Site supportedSite() { return Site.AO3; }

    public Fiction scrapeInfo(String url) {
        Fiction fic = null;
        try {
            String workUrl = normalizeWorkUrl(url);
            Document document = Config.fetch(workUrl);

            String title = document.select("h2.title.heading").text();
            String author = document.select("a[rel=\"author\"]").text();
            String description = document.select("div.summary blockquote.userstuff").text();
            
            String chaptersText = document.select("dd.chapters").text();
            int chapterAmount = Integer.parseInt(chaptersText.split("/")[0]);
            
            int wordCount = extractNumber(document.select("dd.words").text()); 

            System.out.println("===============================================");
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("chapAmount: " + chapterAmount);
            System.out.println("wordCount: " + wordCount);
            System.out.println("Description: " + description);
            System.out.println("===============================================");

            fic = new Fiction(url, Site.AO3, 0, title, author, 0, wordCount, description);
            return fic;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getChapterNames(Fiction fiction) {
        List<String> allChapterNames = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            System.out.println("Here is the ficLink: " + fiction.getFicLink());
            Elements allChapters = document.select("select#selected_id option");
            
            allChapterNames = allChapters
                .stream()
                .map(Element::text)
                .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allChapterNames;
    }

    public List<String> getChapterLinks(Fiction fiction) {
        List<String> allChapterLinks = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
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
            e.printStackTrace();
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
                System.out.println("Next chapter found: " + chapterLink);
            } else if (scrapedChapAmount < storedChapAmount) {
                chapterLink = allChapterLinks.get(scrapedChapAmount - 1);
                System.out.println("Fiction may be stubbed. Using latest available: " + chapterLink);
            } else {
                System.out.println("No new chapter found.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chapterLink;
    }
    
    public int getWordCount(Fiction fiction) {
        try {
            Document document = Config.fetch(fiction.getFicLink());
            int wordCount = extractNumber(document.select("dd.words").text()); 
            
            System.out.printf("Should be the word amount of fic '%s': %s\n",fiction.getTitle(), wordCount);
            return wordCount;
        } catch (Exception e) {
            e.printStackTrace();
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

}
