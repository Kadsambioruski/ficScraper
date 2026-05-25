package com.example;

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
            System.out.println("MAIN STARTED");
            ficBot.start();
        
            ficBot.receiveDiscCommand(DISCORD_CLIENT);
            FicBot.scrapeFicLoop(DISCORD_CLIENT);

        }

    }


    
}
