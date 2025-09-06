package com.example;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import discord4j.core.GatewayDiscordClient;

public class FicScraper {
    private final FicJsonHandler ficJsonHandler; 
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<Integer> updatedFics = new HashSet<>();
    private final Path jsonPath = Paths.get(System.getProperty("user.dir"), "data", "fics.json");


    public FicScraper(){
        this.ficJsonHandler = new FicJsonHandler();
    }

    //#endregion


    public Elements connectToUrl(String ficUrl) {
        Elements wrapper = new Elements();
    
        try {
            Document document = Jsoup.connect(ficUrl).get();
            wrapper = document.select(".mt-list-container.no-border.list-news.ext-1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wrapper;
    }

    public Fiction ficInformation(String ficUrl) {
        Fiction fic = null;
        
        try {
            FileInputStream fileInputStream = new FileInputStream(jsonPath.toFile());
            
            JsonNode rootNode = objectMapper.readTree(fileInputStream);
               
            JsonNode fictionsArray = rootNode.path("fictions");
            int ficID = fictionsArray.size() + 1;

            Document document = Jsoup.connect(ficUrl).get();
            Elements titleAndAuthorContainer = document.select(".col");
            
            String title = titleAndAuthorContainer.select("h1").text();
            String author = titleAndAuthorContainer.select("h4 > span > a").text();
            String chapterText = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();
            int chapterAmount = Integer.parseInt(chapterText.split(" ")[0]);

            System.out.println("===============================================");
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("chapAmount: " + chapterAmount);

            String description = "";
           
            Elements descContainer = document.select("div.description");
            for (Element text : descContainer.select("div.hidden-content")) {
                String paragraph = text.select("p").text();
                description += paragraph;

            }
            description = description.trim();
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
            Document document = Jsoup.connect(ficUrl).get();
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
        System.out.println("Checking if fiction is stubbed");
        Fiction fic = ficJsonHandler.getFic(ficId);
        
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
            Document document = Jsoup.connect(fiction.getFicLink()).get();
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

            
            if (chapAmount < allChapterLinks.size()) {
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(chapAmount);
                System.out.println("Chapter link found: " + chapterLink);
            } else {
                System.out.println("Chapter link not found: requested index is out of bounds.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chapterLink;
    }

    public List<String> getAllChapterNames(int ficId) {
        Fiction fiction = ficJsonHandler.getFic(ficId);
        List<String> allChapterNames = null;
        try {
            Document document = Jsoup.connect(fiction.getFicLink()).get();
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



