package com.example;

import com.example.archive.PdfGenerator;
import com.example.model.Fiction;
import com.example.scrape.ScraperFactory;

import java.util.List;

public class PdfGeneratorTest {

    public static void main(String[] args) throws Exception {

        List<Fiction> fictions =
                Config.ficJsonHandler().getAllFics();

        if (fictions.isEmpty()) {
            System.out.println("No fictions found.");
            return;
        }

        Fiction fiction = fictions.get(1);

        FicScraper scraper = new FicScraper();

        List<String> chapterNames =
                scraper.getAllChapterNames(fiction);

        List<String> chapterLinks =
                scraper.getAllChapterLinks(fiction);
        
        List<String> chapterContents = new java.util.ArrayList<>();

        for (String chapterLink : chapterLinks) {
            String content = ScraperFactory
                    .forFic(fiction.getSite())
                    .getChapterContent(chapterLink);

            chapterContents.add(content);
}

        PdfGenerator pdfGenerator = new PdfGenerator();

        pdfGenerator.generate(
                fiction,
                chapterNames,
                chapterContents
        );

        System.out.println(
                "PDF generated for: " + fiction.getTitle()
        );
    }
}