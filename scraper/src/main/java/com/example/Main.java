package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.example.bot.FicBot;
import com.fasterxml.jackson.databind.ObjectMapper;

import discord4j.core.GatewayDiscordClient;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    final private static Dotenv dotEnv = Dotenv.configure().directory("scraper").load();
    final private static String TOKEN = dotEnv.get("DISCORD_BOT_TOKEN");
    final private static GatewayDiscordClient DISCORD_CLIENT = FicBot.login(TOKEN);
    final private static FicBot ficBot = new FicBot(TOKEN);
 
    /*Should add json file for storing the fics that i have read through to the end. For now i will put them in this comment:
    Industrial Strength Magic
    DIE.RESPAWN.REPEAT
    
    */     
    public static void main(String[] args) {
        if (DISCORD_CLIENT != null) {
            checkDataFiles();
            System.out.println("MAIN STARTED");
            ficBot.start();
        }

    }

    private static void checkDataFiles() {
        Path dataDir = Paths.get("data");
        Path ficsPath = dataDir.resolve("fics.json");
        Path finishedFicsPath = dataDir.resolve("finishedFics.json");

        try {
            if (!Files.exists(dataDir)) {
                System.out.println("data/ directory not found. Creating...");
                Files.createDirectories(dataDir);
            }

            for (Path path : new Path[] {ficsPath, finishedFicsPath}) {
                if (!Files.exists(path)) {
                    System.out.println(path.getFileName() + " not found. Creating with empty structure...");
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(path.toFile(), new com.example.model.FictionList());
                } else {
                    System.out.println(path.getFileName() + " found, skipping");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to initalize data files: " + e.getMessage());
            System.exit(1);
        }
    }


    
}
