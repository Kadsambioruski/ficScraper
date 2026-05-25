package com.example;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import discord4j.core.GatewayDiscordClient;

public class FicScraper {
    private final FicJsonHandler ficJsonHandler; 
    private final ObjectMapper objectMapper = Config.objectMapper();
    private static final Set<Integer> updatedFics = new HashSet<>();
    private final Path jsonPath = Config.ficsJsonPath();


    public FicScraper(){
        this.ficJsonHandler = new FicJsonHandler();
    }

    //#endregion


    public Elements connectToUrl(String ficUrl) {
        Elements wrapper = new Elements();
    
        try {
            Document document = Config.fetch(ficUrl);
            wrapper = document.select(".mt-list-container.no-border.list-news.ext-1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wrapper;
    }

    public Fiction ficInformation(String ficUrl) {
        Fiction fic = null;
        
        try (FileInputStream fileInputStream = new FileInputStream(jsonPath.toFile())){
            
            JsonNode rootNode = objectMapper.readTree(fileInputStream);
               
            JsonNode fictionsArray = rootNode.path("fictions");
            int ficID = fictionsArray.size() + 1;

            Document document = Config.fetch(ficUrl);
            Elements titleAndAuthorContainer = document.select(".col");
            
            String title = titleAndAuthorContainer.select("h1").text();
            String author = titleAndAuthorContainer.select("h4 > span > a").text();
            String chapterText = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();
            int chapterAmount = Integer.parseInt(chapterText.split(" ")[0]);

            System.out.println("===============================================");
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("chapAmount: " + chapterAmount);
           
            StringBuilder descriptionBuilder = new StringBuilder();
            Elements descContainer = document.select("div.description");
            for (Element text : descContainer.select("div.hidden-content")) {
                String paragraph = text.select("p").text();
                descriptionBuilder.append(paragraph);

            }
            String description = descriptionBuilder.toString().trim();
            System.out.println("Description: " + description);
            System.out.println("===============================================");

            fic = new Fiction(ficUrl, ficID, title, author, chapterAmount, description);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return fic;
    }


    public String searchForChap(String ficUrl){
        String output = "";
        try {
            Document document = Config.fetch(ficUrl);
            output = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();
            String parsedOutput = output.split(" ")[0];
            return parsedOutput;
        } catch (IOException e) {
            e.printStackTrace();

        }
        return output;
    }

    public void searchForTitle(Elements wrapper) {
        for (Element potato : wrapper) {
            Elements book = potato.select(".mt-list-item.no-border.inline-block");
            String title = book.select("h2 > a").text();
            System.out.println(title + ".");
            System.out.println("=========================================");
        }
    }

    public boolean checkUpdatedChap(int ficID){
        Fiction fic = ficJsonHandler.getFic(ficID);

        int savedChapCount = fic.getChapAmount(); 
        int currentChapCount = Integer.parseInt(searchForChap(fic.getFicLink()));
        if (savedChapCount < currentChapCount) {
            updatedFics.add(ficID);
            return true;
        }
        return false;
    }

    public boolean checkIfStubbed(GatewayDiscordClient gateWay, int ficId) {
        Fiction fic = ficJsonHandler.getFic(ficId);
        System.out.println("Checking if " + fic.getTitle() + " is stubbed");
        
        int savedChapCount = fic.getChapAmount(); 
        int currentChapCount = Integer.parseInt(searchForChap(fic.getFicLink()));
        return savedChapCount > currentChapCount;
    }
    
    public static List<Fiction> getUpdatedFics() {
        return updatedFics.stream()
            .map(id -> new FicJsonHandler().getFic(id))
            .filter(fiction -> fiction != null)
            .collect(Collectors.toList());
    }

    public static void clearFicUpdate(int ficId) {
        updatedFics.remove(ficId);
    }
    
    public List<String> getAllChapterLinks(int ficId) {
        List<String> allChapterLinks = null;
        Fiction fiction = ficJsonHandler.getFic(ficId);
        
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
    
    public String nextChapFicLink(int ficId) {
        //Go through the list of links until it reaches chapAmount + 1 and return that link
        List<String> allChapterLinks;
        String chapterLink = null;
        try {
            Fiction fiction = ficJsonHandler.getFic(ficId);
            int chapAmount = fiction.getChapAmount();
            allChapterLinks = getAllChapterLinks(ficId);

            int scrapedChapAmount = allChapterLinks.size(); 

            if (scrapedChapAmount > chapAmount) {
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(chapAmount);
                System.out.println("Chapter link found: " + chapterLink);
                getFicWordAmount(ficId);
            } else if (scrapedChapAmount < chapAmount) { 
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(scrapedChapAmount - 1);
                System.out.println("Chapter link found: " + chapterLink);
                getFicWordAmount(ficId);
            } else {
                System.out.println("Chapter link not found: requested index is out of bounds.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chapterLink;
    }

    public int getFicWordAmount(int ficId) {
        Fiction fiction = ficJsonHandler.getFic(ficId);
        int wordCount = 0;
        try {
            Document document = Config.fetch(fiction.getFicLink());
            Element pagesLi = document.selectFirst("ul.list-unstyled li:contains(Pages)");

            if (pagesLi == null) return 0;

            Element iTag = pagesLi.selectFirst("i.popovers");
            
            if (iTag == null) return 0; 
            
            String dataContent = iTag.attr("data-content");
            Pattern pattern = Pattern.compile("calculated from ([0-9,]+) words");
            Matcher matcher = pattern.matcher(dataContent);

            if (matcher.find()) {
                String wordCountStr = matcher.group(1).replaceAll(",", "");
                wordCount = Integer.parseInt(wordCountStr);
            }
        
            System.out.printf("Should be the word amount of fic '%s': %s\n",fiction.getTitle(), wordCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordCount;
    }

    public List<String> getAllChapterNames(int ficId) {
        Fiction fiction = ficJsonHandler.getFic(ficId);
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

}



