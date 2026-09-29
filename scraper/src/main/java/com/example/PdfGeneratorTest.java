package com.example;

import com.example.archive.PdfGenerator;
import com.example.model.ChapterInfo;
import com.example.model.Fiction;
import com.example.scrape.ScraperFactory;

import java.util.List;

public class PdfGeneratorTest {

        public static void main(String[] args) throws Exception {
        List<Fiction> fictions = Config.ficJsonHandler().getAllFics();

        if (fictions.isEmpty()) {
                System.out.println("No fictions found.");
                return;
        }

        Fiction fiction = fictions.get(3);

        System.out.println("Testing: " + fiction.getTitle());
        System.out.println("Site: " + fiction.getSite());

        FicScraper scraper = new FicScraper();
        scraper.scrapeMissingChapters(fiction);

        System.out.println("Finished scraping missing chapters.");
        }
}