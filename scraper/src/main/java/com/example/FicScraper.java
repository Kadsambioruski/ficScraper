package com.example;
import java.io.IOException;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FicScraper {
    private String urlOfFic;


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
        try {
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

            fic = new Fiction(title, author, chapterAmount, description);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return fic;
    }


    public void searchForTitle(Elements wrapper) {
        for (Element potato : wrapper) {
            Elements book = potato.select(".mt-list-item.no-border.inline-block");
            String title = book.select("h2 > a").text();
            System.out.println(title + ".");
            System.out.println("=========================================");
        }
    }
}



