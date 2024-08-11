package com.example;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FicScraper {
    private String urlOfFic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FicScraper(){}

    public FicScraper(String urlOfFic) {
        this.urlOfFic = urlOfFic;
    }

    public String getUrlOfFic() {
        return this.urlOfFic;
    }
    //#endregion


    public Elements connectToUrl() {
        Elements wrapper = new Elements();
    
        try {
            Document document = Jsoup.connect(this.urlOfFic).get();
            wrapper = document.select(".mt-list-container.no-border.list-news.ext-1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wrapper;
    }

    public Fiction ficInformation() {
        Fiction fic = null;
        String filePath = "scraper/src/main/resources/fics.json";  

        
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            
            JsonNode rootNode = objectMapper.readTree(fileInputStream);
               
            JsonNode fictionsArray = rootNode.path("fictions");
            int ficID = fictionsArray.size() + 1;

            Document document = Jsoup.connect(this.urlOfFic).get();
            Elements titleAndAuthorContainer = document.select(".col");
            
            String title = titleAndAuthorContainer.select("h1").text();
            String author = titleAndAuthorContainer.select("h4 > span > a").text();
            String chapterAmount = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();

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

            fic = new Fiction(getUrlOfFic(),ficID, title, author, chapterAmount, description);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return fic;
    }


    public String searchForChap(){
        String output = "";
        try {
            Document document = Jsoup.connect(this.urlOfFic).get();
            output = document.select("div.portlet.light > div.portlet-title > div.actions > span").text();
            String parsedOutput = output.split(" ")[0];
            return parsedOutput;
        } catch (Exception e) {
            // TODO: handle exception
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
        boolean chapFound = false;
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        if (Integer.parseInt(jsonDeserializer.getChapAmountInJSON(ficID)) < Integer.parseInt(searchForChap())) {
            chapFound = true; 
        }
        return chapFound;
    }
    
    
    public List<String> getAllChapterLinks(String ficName) {
        List<String> allChapterLinks = null;
        JsonDeserializer jsonDeserializer = new JsonDeserializer();

        try {
            Document document = Jsoup.connect(jsonDeserializer.getFicLink(ficName)).get();
            Elements allChapters = document.select("table#chapters tbody tr");
            
            allChapterLinks = allChapters
            .stream()
            .map(chapter -> chapter.attr("data-url"))
            .collect(Collectors.toList());
        } catch (Exception e) {
            // TODO: handle exception
        }
        return allChapterLinks;
    }
    
    public String nextChapFicLink(String ficName) {
        //Go through the list of links until it reaches chapAmount + 1 and return that link
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        List<String> allChapterLinks = null;
        String chapterLink = null;
        try {
            int ficId = jsonDeserializer.getFicId(ficName);
            int chapAmount = Integer.parseInt(jsonDeserializer.getChapAmountInJSON(ficId));
            allChapterLinks = getAllChapterLinks(ficName);

            
            if (chapAmount < allChapterLinks.size()) {
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(chapAmount);
                System.out.println("Chapter link found: " + chapterLink);
            } else {
                System.out.println("Chapter link not found: requested index is out of bounds.");
            }
            
        } catch (Exception e) {
            // TODO: handle exception
        }
        return chapterLink;
    }

    public static List<String> getAllChapterNames(String ficName) {
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        List<String> allChapterNames = null;
        try {
            Document document = Jsoup.connect(jsonDeserializer.getFicLink(ficName)).get();
            Elements allChapters = document.select("table#chapters tbody tr");
            
            allChapterNames = allChapters
                .stream()
                .map(chapter -> chapter.select("a").first().text())
                .collect(Collectors.toList());
        } catch (Exception e) {
            // TODO: handle exception
        }
        return allChapterNames;
    }


    /*public String searchForLatestChapLink() {
        String linkToLatestChap = "";
        List<String> allChapterLinks;
        try {
            //4
            allChapterLinks = getAllChapterLinks();

            linkToLatestChap = "https://www.royalroad.com" + allChapterLinks.get(allChapterLinks.size() - 1);
            System.out.println("Found latest chapter link.");
        } catch (Exception e) {
            // TODO: handle exception
        }
        return linkToLatestChap;

    }*/


}



