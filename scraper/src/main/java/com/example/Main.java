package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;

import javax.sound.midi.Soundbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import discord4j.core.GatewayDiscordClient;

public class Main {
    private static Scanner userInputScanner = new Scanner(System.in);
    private final static String MENU_ITEM_Q = "-1";
    private static String token = "";
    private static String generalChat = "";

    public static void main(String[] args) {
        boolean notQ = true;
        JsonDeserializer jsonDeserializer = new JsonDeserializer();

        //TODO use cookie saving for cloudflare
        // Currently only works with Royal Road
        while (notQ) {
            try {
                int menuNav = Integer.parseInt(menu());
                switch (menuNav) {
                    case 1:
                        System.out.println("Put in link to new fic: ");
                        String newLinkToFic = input();
                        //TODO parse through input to check link format
                        FicScraper ficScraper = new FicScraper(newLinkToFic);
                        Fiction newFic = ficScraper.ficInformation();
                        
                        JsonSerializer serializer = new JsonSerializer();
                        serializer.saveFicToJson(newFic);
                        
                        System.out.println("Fic added to JSON file: " + newFic);
                        break;
                    case 2:
                        jsonDeserializer.readJsonFile();
                        break;
                    case 3:
                        System.out.println("Choose fic from list through fic Number");
                        String ficNumberString = input();
                        int parsedFicNumber = Integer.parseInt(ficNumberString);

                        FicScraper ficScraper2 = new FicScraper(jsonDeserializer.getFicLink(parsedFicNumber));

                        System.out.println(jsonDeserializer.getChapAmountInJSON(parsedFicNumber));
                        
                        if (ficScraper2.checkUpdatedChap(parsedFicNumber) != null) {
                            GatewayDiscordClient gateWay = FicBot.login(token);
                            FicBot.sendMessage(gateWay, generalChat, ficScraper2.checkUpdatedChap(parsedFicNumber));
                        }
                        break;
                    case 4:
                        System.out.println("Put in the amount of hours the loop should run: ");
                        int workHours = Integer.parseInt(input());
                        String filePath = "scraper/src/main/java/com/example/fics.json";

                        LocalDateTime startTime = LocalDateTime.now();
                        LocalDateTime endTime = startTime.plusHours(workHours);
                        while (LocalDateTime.now().isBefore(endTime)) {
                            try (FileInputStream fileInputStream = new FileInputStream(new File(filePath));
                            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)){ // Translates the text to UTF_8
                            
                                ObjectMapper objectMapper = new ObjectMapper();
                            
                                JsonNode rootNode = objectMapper.readTree(inputStreamReader);
                    
                                JsonNode fictionsArray = rootNode.path("fictions");
                                
                                if (fictionsArray.isArray()) {
                                    for (JsonNode fictionNode : fictionsArray) {
                                        int ficId = Integer.parseInt(fictionNode.path("ficID").asText());
                                        FicScraper ficScraper3 = new FicScraper(jsonDeserializer.getFicLink(ficId));
                                       System.out.println(ficScraper3.checkUpdatedChap(ficId));
                                        
                                    }
                                }
                                
                            } catch (IOException e) {
                                e.printStackTrace();        
                            }
                        }
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
    
}
