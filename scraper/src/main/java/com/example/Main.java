package com.example;

public class Main {
    
    public static void main(String[] args) {
        /*FicScraper work = new FicScraper("https://www.royalroad.com/home", null);
        Elements wrapper = work.connectToSite();
        work.searchForTitle(wrapper);
        */
        Fiction fic = new Fiction("Potato", "It is about a potato");
        Fiction bruh = new Fiction("bob", "The builder");
        JsonSerializer serializer = new JsonSerializer();

        serializer.saveFicToJson(fic);
        serializer.saveFicToJson(bruh);
    }
    
}
