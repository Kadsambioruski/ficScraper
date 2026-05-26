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

public class FicScraper { 
    private static final FicJsonHandler ficJsonHandler = Config.ficJsonHandler();
    private final ObjectMapper objectMapper = Config.objectMapper();
    private static final Set<Integer> updatedFics = new HashSet<>();
    private final Path jsonPath = Config.ficsJsonPath();

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

    public boolean checkUpdatedChap(Fiction fiction, int currentChapCount){
        if (fiction.getChapAmount() < currentChapCount) {
            updatedFics.add(fiction.getFicID());
            return true;
        }
        return false;
    }

    public boolean checkIfStubbed(Fiction fiction, int currentChapCount) {
        System.out.println("Checking if " + fiction.getTitle() + " is stubbed");
        return fiction.getChapAmount() > currentChapCount;
    }
    
    public static List<Fiction> getUpdatedFics() {
        return updatedFics.stream()
            .map(id -> ficJsonHandler.getFic(id))
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
    
    public String nextChapFicLink(Fiction fiction, Document document) {
        //Go through the list of links until it reaches chapAmount + 1 and return that link
        String chapterLink = null;
        try {
            int chapAmount = fiction.getChapAmount();
            List<String> allChapterLinks = document.select("table#chapters tbody tr")
                .stream()
                .map(chapter -> chapter.attr("data-url"))
                .collect(Collectors.toList());

            int scrapedChapAmount = allChapterLinks.size(); 

            if (scrapedChapAmount > chapAmount) {
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(chapAmount);
                System.out.println("Chapter link found: " + chapterLink);
                getFicWordAmount(fiction, document);
            } else if (scrapedChapAmount < chapAmount) { 
                chapterLink = "https://www.royalroad.com" + allChapterLinks.get(scrapedChapAmount - 1);
                System.out.println("Chapter link found: " + chapterLink);
                getFicWordAmount(fiction, document);
            } else {
                System.out.println("Chapter link not found: requested index is out of bounds.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chapterLink;
    }

    public void getFicWordAmount(Fiction fiction, Document document) {
        int wordCount = 0;
        try {
            Element pagesLi = document.selectFirst("ul.list-unstyled li:contains(Pages)");

            if (pagesLi == null) return;

            Element iTag = pagesLi.selectFirst("i.popovers");
            
            if (iTag == null) return; 
            
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
    }

    public List<String> getAllChapterNames(Fiction fiction, Document document) {
        List<String> allChapterNames = null;
        try {
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



