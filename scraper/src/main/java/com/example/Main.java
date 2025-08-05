package com.example;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandData;
import io.github.cdimascio.dotenv.Dotenv;
import reactor.core.publisher.Mono;

public class Main {
    final private static Scanner userInputScanner = new Scanner(System.in);
    final private static String MENU_ITEM_Q = "-1";
    final private static Dotenv dotEnv = Dotenv.configure().directory("./scraper").load();
    final private static String TOKEN = dotEnv.get("DISCORD_BOT_TOKEN");
    final private static String DISCORD_SERVER_ID = dotEnv.get("DISCORD_SERVER_ID");
    final private static String DISCORD_CHANNEL_ID = dotEnv.get("DISCORD_CHANNEL_ID"); 
    final private static JsonDeserializer jsonDeserializer = new JsonDeserializer();
    final private static FicJsonHandler ficJsonHandler = new FicJsonHandler();
    final private static GatewayDiscordClient DISCORD_CLIENT = FicBot.login(TOKEN);
    private static boolean runLoop = true;
    private static Thread loopThread = null;
    /*Should add json file for storing the fics that i have read through to the end. For now i will put them in this comment:
    Industrial Strength Magic
    DIE.RESPAWN.REPEAT
    
    */     
    public static void main(String[] args) {
        boolean notQ = true;


        if (DISCORD_CLIENT != null) {
            registerCommands(DISCORD_CLIENT).subscribe();
            FicBot.handleSelectMenuInteractions(DISCORD_CLIENT);
            FicBot.handleButtonInteractions(DISCORD_CLIENT);
        }
        
        
        Runnable handleDiscCommands = () -> {
            FicBot.sendMessage(DISCORD_CLIENT, DISCORD_CHANNEL_ID, String.format("Listening for commands!"));
            recieveDiscCommand(DISCORD_CLIENT);
        };
        
        
        Thread discordListenThread = new Thread(handleDiscCommands);
        loopThread = new Thread(() ->  {
            scrapeFicLoop(DISCORD_CLIENT);
        });
        
        discordListenThread.start();
        
        // Handle command interactions
        // TODO use cookie saving for cloudflare
        // Currently only works with Royal Road
        while (notQ) {
            try {
                int menuNav = Integer.parseInt(menu());
                switch (menuNav) {
                    case 1:
                        //Ends of magic and Syl do not work, they get registered as null.
                        putFicInJson();
                    break;
                    case 2:
                        jsonDeserializer.printJsonContent();
                    break;
                    case 3:
                        checkFicUpdates();
                    break;
                    case 4: 
                        scrapeFicLoop(DISCORD_CLIENT);
                        break;
                    case -1:
                        notQ = false;
                        break;
                }

            } catch (NumberFormatException e) {
                System.out.println("Input was not an integer or q, try again.");
                e.printStackTrace();
            }

        }

    }

    public static String input() {
        String inputValue;
        while (true) {
            String input = userInputScanner.next();
            userInputScanner.nextLine();
            if (input.equals("q")) {
                inputValue = MENU_ITEM_Q;
                break;
            }
            inputValue = input;
            break;
        }
        return inputValue;
    }

    public static String menu() {
        System.out.println("""
                1. New link to fic
                2. See all fictions
                3. New updates from fics
                4. Turn on loop
                q. Quit
                """);

        return input();
    }


    private static Mono<Void> registerCommands(GatewayDiscordClient gateway) {
        long applicationId = gateway.getRestClient().getApplicationId().block();

        Mono<ApplicationCommandData> readCommand = FicBot.registerReadCommand(gateway, DISCORD_SERVER_ID,  applicationId);
        Mono<ApplicationCommandData> endLoopCommand = FicBot.registerEndLoopCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> startLoopCommand = FicBot.registerStartLoopCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> addFicCommand = FicBot.registerAddFicCommand(gateway, DISCORD_SERVER_ID, applicationId);

        return Mono.when(readCommand, endLoopCommand, startLoopCommand, addFicCommand)
                .doOnSuccess(v -> System.out.println("Commands registered successfully"))
                .then();
    }

    public static void recieveDiscCommand(GatewayDiscordClient gateway){
        gateway.on(ChatInputInteractionEvent.class)
            .flatMap(event -> {
                switch (event.getCommandName()) {
                    case "read":
                        return FicBot.handleReadCommand(event);
                    case "endloop":
                        runLoop = false;
                        try {
                            loopThread.interrupt();
                            loopThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                        return FicBot.handleEndLoopCommand(event);
                    case "startloop":
                        runLoop = true;
                        if (loopThread == null || !loopThread.isAlive()) {
                            loopThread = new Thread(() ->  {
                                scrapeFicLoop(DISCORD_CLIENT);
                            });
                        }
                        loopThread.start();
                        //TODO main menu perhaps not needed anymore with interaction through disc.
                        return FicBot.handleStartLoopCommand(event);
                    case "add": 
                        return FicBot.handleAddFicCommand(event);
                    default:
                    return Mono.empty();
                }
            })
            .onErrorResume(err -> {
                err.printStackTrace();
                return Mono.empty();
            })
            .then(gateway.onDisconnect()) // Chaining with the disconnect operation
            .block(); // Block until completion
    }

    public static void putFicInJson(){
        System.out.println("Put in link to new fic: ");
        String newLinkToFic = input();
        // TODO parse through input to check link format
        FicScraper ficScraper = new FicScraper();
        Fiction newFic = ficScraper.ficInformation(newLinkToFic);
        
        ficJsonHandler.addFic(newFic);
        
        System.out.println("Fic added to JSON file: " + newFic);
    }

    public static void putFicInJson(String newLinkToFic){
        System.out.println("Put in link to new fic: ");
        // TODO parse through input to check link format
        FicScraper ficScraper = new FicScraper();
        Fiction newFic = ficScraper.ficInformation(newLinkToFic);
        
        ficJsonHandler.addFic(newFic);
        
        System.out.println("Fic added to JSON file: " + newFic);
    }

    public static void checkFicUpdates(){
        System.out.println("Choose fic from list through fic Number");
        String ficNumberString = input();
        int ficId = Integer.parseInt(ficNumberString);
        
        Fiction fic = ficJsonHandler.getFic(ficId);
        FicScraper ficScraper2 = new FicScraper();
        
        //ficScraper2.searchForLatestChapLink();
        
        System.out.println("potato smotato");
        System.out.println(jsonDeserializer.getChapAmountInJSON(ficId));
        
        if (ficScraper2.checkUpdatedChap(ficId)) {
            GatewayDiscordClient gateWay = FicBot.login(TOKEN);
            FicBot.sendMessage(gateWay, DISCORD_CHANNEL_ID,
            String.format("New chapter found! Latest chapter in "
            + jsonDeserializer.getFicTitle(ficId) + " is: "
            + Integer.valueOf(ficScraper2.searchForChap(fic.getFicLink()))));
        }
    }

    //TODO could use a discord command to end the webscrape loop instead of deactivating it through the terminal (could also try to start it with a command although i have no clue how rn)
    public static void scrapeFicLoop(GatewayDiscordClient gateWay){
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("Loop has started!");
        Map<Integer, Integer> lastSeenChapters = new HashMap<>();
        
        LocalDateTime nextTime = LocalDateTime.now().withMinute(0).withSecond(0);
        
        while (runLoop) {
            if(LocalDateTime.now().withMinute(0).withSecond(0).isBefore(nextTime)) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // TODO: handle exception
                }
                continue;
            } else {
                nextTime = LocalDateTime.now().plusHours(3);
            }
            
            JsonNode rootNode = new JsonDeserializer().readJsonFile();
            
            JsonNode fictionsArray = rootNode.path("fictions");

            if (fictionsArray.isArray()) {
                for (JsonNode fictionNode : fictionsArray) {
                    int ficId = Integer.parseInt(fictionNode.path("ficID").asText());

                    FicScraper ficScraper3 = new FicScraper();
                    int latestChapter = Integer.parseInt(ficScraper3.searchForChap(fictionNode.path("ficLink").asText()));
                    
                    if (ficScraper3.checkUpdatedChap(ficId)) {
                        if (!lastSeenChapters.containsKey(ficId)|| lastSeenChapters.get(ficId) < latestChapter) {
                            
                            lastSeenChapters.put(ficId, latestChapter);
                            
                            
                            FicBot.sendMessage(gateWay, DISCORD_CHANNEL_ID,
                            String.format("New chapter found! New chapter found in %s is: %d\nLink: %s", jsonDeserializer.getFicTitle(ficId),
                            Integer.parseInt(jsonDeserializer.getChapAmountInJSON(ficId)) + 1, ficScraper3.nextChapFicLink(ficId)));
                        }
                    }
                }
            }
           
        }
        System.out.println("nfgegeognegoeg");
        LocalDateTime endTime = LocalDateTime.now();
        long totalTime = startTime.until(endTime, ChronoUnit.HOURS);
        System.out.println("Uptime of loop: " + totalTime + "hours.");
    }
    
}
