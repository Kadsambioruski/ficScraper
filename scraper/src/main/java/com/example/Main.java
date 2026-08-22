package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.example.bot.FicBot;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Dotenv dotEnv = Dotenv.configure().directory("scraper").load();
    private static final String TOKEN = dotEnv.get("DISCORD_BOT_TOKEN");
    private static final FicBot ficBot = new FicBot(TOKEN);

    public static void main(String[] args) {
        checkDataFiles();
        log.info("Main started");
        ficBot.start();
    }

    private static void checkDataFiles() {
        Path dataDir = Paths.get("data");
        Path ficsPath = dataDir.resolve("fics.json");
        Path finishedFicsPath = dataDir.resolve("finishedFics.json");

        try {
            if (!Files.exists(dataDir)) {
                log.info("data/ directory not found. Creating...");
                Files.createDirectories(dataDir);
            }

            for (Path path : new Path[] {ficsPath, finishedFicsPath}) {
                if (!Files.exists(path)) {
                    log.info("{} not found. Creating with empty structure...", path.getFileName());
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(path.toFile(), new com.example.model.FictionList());
                } else {
                    log.info("{} found, skipping", path.getFileName());
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize data files", e);
            System.exit(1);
        }
    }


    
}
