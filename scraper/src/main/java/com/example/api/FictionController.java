package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Config;
import com.example.model.Chapter;
import com.example.model.Fiction;

@RestController
@RequestMapping("/api/fictions")
public class FictionController {

    @GetMapping
    public List<Fiction> getFictions() {
        return Config.ficJsonHandler().getAllFics();
    }

    @GetMapping("/{fictionId}/chapters")
    public List<Chapter> getChapters(@PathVariable("fictionId") UUID fictionId) {
        return Config.chapterStorage().getForFiction(fictionId);
    }
}