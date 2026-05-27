package com.example.scrape;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;

import com.example.model.Fiction;
import com.example.Config;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FanfictionScraper implements SiteScraper {
    public Site supportedSite() { return Site.FANFICTION; }

    public Fiction scrapeInfo(String url) {
        Fiction fic = null;
        try {
            Document document = Config.fetch(url);

            String title = document.select("b.xcontrast_txt").text();
            String author = document.select("a.xcontrast_txt:nth-child(4)").text();
            String description = document.select("div.xcontrast_txt:nth-child(7)").text();
            String metadata = document.select(".xgray.xcontrast_txt").text();
            System.out.println("Metadata: " + metadata);
            int chapterAmount = extractNumber(metadata, "Chapters:");

            System.out.println("===============================================");
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("chapAmount: " + chapterAmount);
            System.out.println("Description: " + description);
            System.out.println("===============================================");

            fic = new Fiction(url, Site.FANFICTION, 0, title, author, chapterAmount, description);
            return fic;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getChapterNames(Fiction fiction) {
        List<String> allChapterNames = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            System.out.println("Here is the ficLink: " + fiction.getFicLink());
            Elements allChapters = document.select("select#chap_select > option");
            
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
            Elements allChapters = document.select("select#chap_select > option");
            
            String ficLink = fiction.getFicLink();
            String storyId = ficLink.split("/s/")[1].split("/")[0];
            String storyTitle = ficLink.split("/s/")[1].split("/", 3)[2];

            allChapterLinks = allChapters.stream()  
                .map(option -> option.attr("value"))
                .filter(value -> value != null && !value.isEmpty())
                .map(value -> "https://www.fanfiction.net/s/" + storyId + "/" + value + "/" + storyTitle)
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
            String metadata = document.select(".xgray.xcontrast_txt").text();

            int wordCount = extractNumber(metadata, "Words:");
            
            System.out.printf("Should be the word amount of fic '%s': %s\n",fiction.getTitle(), wordCount);
            return wordCount;
        } catch (Exception e) {
            e.printStackTrace();
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
