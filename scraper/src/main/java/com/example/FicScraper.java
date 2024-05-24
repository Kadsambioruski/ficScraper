package com.example;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FicScraper {
    private String webSiteUrl;
    private String nameOfFic;


    public FicScraper(String webSiteUrl, String nameOfFic) {
        this.webSiteUrl = webSiteUrl;
        this.nameOfFic = nameOfFic;
    }

    //#region Getters
    public String getWebSiteUrl() {
        return this.webSiteUrl;
    }

    public String getNameOfFic() {
        return this.nameOfFic;
    }
    //#endregion


    public Elements connectToSite() {
        Elements wrapper = new Elements();
    
        try {
            Document document = Jsoup.connect(this.webSiteUrl).get();
            wrapper = document.select(".mt-list-container.no-border.list-news.ext-1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wrapper;
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



