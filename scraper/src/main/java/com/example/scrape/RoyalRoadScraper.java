package com.example.scrape;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.example.Config;
import com.example.model.Fiction;

public class RoyalRoadScraper implements SiteScraper{
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
            
            System.out.println("===============================================");
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("ChapAmount: " + chapterAmount);
            System.out.println("WordCount: " + wordCount);
            System.out.println("Description: " + description);
            System.out.println("===============================================");
    
            fic = new Fiction(url, Site.ROYAL_ROAD, 0, title, author, chapterAmount, wordCount, description);
            return fic;
            
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getChapterLinks(Fiction fiction) {
        List<String> allChapterLinks = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            Elements allChapters = document.select("table#chapters tbody tr");
            
            allChapterLinks = allChapters
            .stream()
            .map(chapter -> chapter.attr("data-url"))
            .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allChapterLinks;
    }

    public String nextChapterLink(Fiction fiction) {
        String chapterLink = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());

            int chapAmount = fiction.getChapAmount();
            List<String> allChapterLinks = document.select("table#chapters tbody tr")
                .stream()
                .map(chapter -> chapter.attr("data-url"))
                .collect(Collectors.toList());

            int scrapedChapAmount = allChapterLinks.size(); 

            if (scrapedChapAmount > chapAmount) {
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(chapAmount);
                System.out.println("Chapter link found: " + chapterLink);
            } else if (scrapedChapAmount < chapAmount) { 
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(scrapedChapAmount - 1);
                System.out.println("Chapter link found: " + chapterLink);
            } else {
                System.out.println("Chapter link not found: requested index is out of bounds.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chapterLink;
    }

    public List<String> getChapterNames(Fiction fiction) {
        List<String> allChapterNames = null;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            System.out.println("Here is the ficLink: " + fiction.getFicLink());
            Elements allChapters = document.select("table#chapters tbody tr");
            allChapterNames = allChapters
                .stream()
                .map(chapter -> chapter.select("a").first().text())
                .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allChapterNames;
    }

    public int getWordCount(Fiction fiction) {
        int wordCount = 0;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            wordCount = extractWordCount(document);

            System.out.printf("Should be the word amount of fic '%s': %s\n",fiction.getTitle(), wordCount);
            return wordCount;
        } catch (Exception e) {
            e.printStackTrace();
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
