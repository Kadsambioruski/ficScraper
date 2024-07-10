package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandData;
import reactor.core.publisher.Mono;

public class Main {
    private static Scanner userInputScanner = new Scanner(System.in);
    private final static String MENU_ITEM_Q = "-1";
    private static String token = "";
    private static String generalChat = "";
    private static JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private static boolean runLoop = true;

    public static void main(String[] args) {
        boolean notQ = true;
        GatewayDiscordClient gateWay1 = FicBot.login(token);

        long applicationId = gateWay1.getRestClient().getApplicationId().block();

        if (gateWay1 != null) {
            Mono<ApplicationCommandData> brotato = FicBot.registerReadCommand(gateWay1, applicationId);
            Mono<ApplicationCommandData> smotato = FicBot.registerEndLoopCommand(gateWay1, applicationId);
            
            brotato.doOnSuccess(commandData -> {
                if (commandData != null) {
                    System.out.println("Read command registered successfully: " + commandData.name());
                } else {
                    System.err.println("Failed to register read command.");
                }

            }).subscribe();

            smotato.doOnSuccess(commandData -> {
                if (commandData != null) {
                    System.out.println("endloop command registered successfully: " + commandData.name());
                } else {
                    System.err.println("Failed to register endloop command.");
                }

            }).subscribe();

        }


        
        Runnable potato = () -> {
            FicBot.sendMessage(gateWay1, generalChat, String.format("Listening for commands!"));
            recieveDiscCommand(gateWay1);
        };
        Thread discordListenThread = new Thread(potato);

        discordListenThread.start();

                
        
        // Handle command interactions
        // TODO use cookie saving for cloudflare
        // Currently only works with Royal Road
        while (notQ) {
            try {
                int menuNav = Integer.parseInt(menu());
                switch (menuNav) {
                    case 1:
                        putFicInJson();
                    break;
                    case 2:
                        jsonDeserializer.readJsonFile();
                    break;
                    case 3:
                        checkFicUpdates();
                    break;
                    case 4: 
                        scrapeFicLoop(gateWay1);
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


    public static void recieveDiscCommand(GatewayDiscordClient gateway){
        gateway.on(ChatInputInteractionEvent.class)
            .flatMap(event -> {
                switch (event.getCommandName()) {
                    case "read":
                        return FicBot.handleReadCommand(event);
                    case "endloop":
                        runLoop = false;
                        return FicBot.handleEndLoopCommand(event);
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
        FicScraper ficScraper = new FicScraper(newLinkToFic);
        Fiction newFic = ficScraper.ficInformation();
        
        JsonSerializer serializer = new JsonSerializer();
        serializer.saveFicToJson(newFic);
        
        System.out.println("Fic added to JSON file: " + newFic);
    }

    public static void checkFicUpdates(){
        System.out.println("Choose fic from list through fic Number");
        String ficNumberString = input();
        int parsedFicNumber = Integer.parseInt(ficNumberString);
        
        FicScraper ficScraper2 = new FicScraper(jsonDeserializer.getFicLink(parsedFicNumber));
        
        System.out.println(jsonDeserializer.getChapAmountInJSON(parsedFicNumber));
        
        if (ficScraper2.checkUpdatedChap(parsedFicNumber)) {
            GatewayDiscordClient gateWay = FicBot.login(token);
            FicBot.sendMessage(gateWay, generalChat,
            String.format("New chapter found! Latest chapter in "
            + jsonDeserializer.getFicTitle(parsedFicNumber) + " is: "
            + Integer.parseInt(ficScraper2.searchForChap())));
        }
    }

    //TODO could use a discord command to end the webscrape loop instead of deactivating it through the terminal (could also try to start it with a command although i have no clue how rn)
    public static void scrapeFicLoop(GatewayDiscordClient gateWay){
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("Loop has started!");
        String filePath = "scraper/src/main/java/com/example/fics.json";
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
            try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream,
            StandardCharsets.UTF_8)) { // Translates the text to UTF_8
                
                
                ObjectMapper objectMapper = new ObjectMapper();
                
                JsonNode rootNode = objectMapper.readTree(inputStreamReader);
                
                JsonNode fictionsArray = rootNode.path("fictions");

                if (fictionsArray.isArray()) {
                    for (JsonNode fictionNode : fictionsArray) {
                        int ficId = Integer.parseInt(fictionNode.path("ficID").asText());
                        FicScraper ficScraper3 = new FicScraper(jsonDeserializer.getFicLink(ficId));
                        int latestChapter = Integer.parseInt(ficScraper3.searchForChap());
                        
                        if (ficScraper3.checkUpdatedChap(ficId)) {
                            if (!lastSeenChapters.containsKey(ficId)|| lastSeenChapters.get(ficId) < latestChapter) {
                                
                                lastSeenChapters.put(ficId, latestChapter);
                                
                                
                                FicBot.sendMessage(gateWay, generalChat,
                                String.format("New chapter found! Latest chapter in "
                                + jsonDeserializer.getFicTitle(ficId) + " is: "
                                + Integer.parseInt(ficScraper3.searchForChap())));
                            }
                        }
                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("nfgegeognegoeg");
        LocalDateTime endTime = LocalDateTime.now();
        long totalTime = startTime.until(endTime, ChronoUnit.HOURS);
        System.out.println("Uptime of loop: " + totalTime + "hours.");
    }
    
}
