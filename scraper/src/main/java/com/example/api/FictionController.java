package com.example.api;

import java.util.List;
import java.util.UUID;

import javax.management.RuntimeErrorException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Config;
import com.example.FicScraper;
import com.example.model.Chapter;
import com.example.model.Fiction;

@RestController
@RequestMapping("/api/fictions")
public class FictionController {

    @GetMapping
    public List<Fiction> getFictions() {
        return Config.ficJsonHandler().getAllFics();
    }

    @GetMapping("/{fictionId}")
    public Fiction getFiction(@PathVariable("fictionId") UUID fictionId) {
        Fiction fiction = Config.ficJsonHandler().getFic(fictionId);

        if (fiction == null) {
            throw new RuntimeException("Fiction not found: " + fictionId);
        }
        return fiction;
    }

    @GetMapping("/finished")
    public List<Fiction> getFinishedFictions() {
        return Config.ficJsonHandler().getFinishedFics();
    }

    @GetMapping("/{fictionId}/chapters")
    public List<Chapter> getChapters(@PathVariable("fictionId") UUID fictionId) {
        return Config.chapterStorage().getForFiction(fictionId);
    }
    
   @PostMapping("/{fictionId}/update-chapters")
    public void updateChapters(@PathVariable("fictionId") UUID fictionId) {
        Fiction fiction = Config.ficJsonHandler().getFic(fictionId);

        if (fiction == null) {
            throw new RuntimeException("Fiction not found: " + fictionId);
        }

        FicScraper scraper = new FicScraper();
        scraper.updateFiction(fiction);
    }

    @PostMapping 
    public Fiction addFiction(@RequestBody String ficUrl) {
        FicScraper scraper = new FicScraper();
        return scraper.addFiction(ficUrl);
    }

    @PostMapping("/{fictionId}/finish")
    public Fiction finishFiction(@PathVariable("fictionId") UUID fictionId) {
        Fiction fiction = Config.ficJsonHandler().getFic(fictionId);

        if (fiction == null) {
            throw new RuntimeException("Fiction not found: " + fictionId);
        }

        Config.ficJsonHandler().moveFicToFinished(fiction);
        return fiction;
    }

    @PostMapping("/{fictionId}/remove")
    public void removeFiction(@PathVariable("fictionId") UUID fictionId) {
        Fiction fiction = Config.ficJsonHandler().getFic(fictionId);

        if (fiction == null) {
            throw new RuntimeException("Fiction not found: " + fictionId);
        }

        Config.ficJsonHandler().removeFic(fiction);
    }

    @PostMapping("/refresh")
    public void refreshFictions() {
        List<Fiction> fictions = Config.ficJsonHandler().getAllFics();
        FicScraper scraper = new FicScraper();

        for (Fiction fiction : fictions) {
            scraper.updateFiction(fiction);
        }
    }

    @PostMapping("/{fictionId}/progress")
    public Fiction updateReaderProgress(
            @PathVariable("fictionId") UUID fictionId,
            @RequestBody int chapter) {

        Fiction fiction = Config.ficJsonHandler().getFic(fictionId);

        if (fiction == null) {
            throw new RuntimeException("Fiction not found: " + fictionId);
        }

        Config.ficJsonHandler().setFicChapter(fiction, chapter);

        fiction.setReaderProgress(chapter);
        return fiction;
    }

}