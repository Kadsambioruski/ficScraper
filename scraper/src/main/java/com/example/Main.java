package com.example;

import java.util.Scanner;

public class Main {
    private static Scanner userInputScanner = new Scanner(System.in);
    final static String MENU_ITEM_Q = "-1";


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
                        System.out.println(ficScraper2.checkUpdatedChap(parsedFicNumber));
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
                q. Quit 
                """);
        
        return input();
    }
    
}
