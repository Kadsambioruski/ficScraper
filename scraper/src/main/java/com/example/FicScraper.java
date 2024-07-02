package com.example;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FicScraper {
    private String urlOfFic;
    private final ObjectMapper objectMapper = new ObjectMapper();


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
        String filePath = "scraper/src/main/java/com/example/fics.json";  

        
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
            // TODO: handle exception
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

    public String checkUpdatedChap(int ficID){
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        if (Integer.parseInt(jsonDeserializer.getChapAmountInJSON(ficID)) < Integer.parseInt(searchForChap())) {
            return String.format("New chapter found!");
        } else {
            return String.format("No new chapter for this fic.");
        }
    }


}



