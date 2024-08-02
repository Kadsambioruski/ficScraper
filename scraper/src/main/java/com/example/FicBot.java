package com.example;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class FicBot {
    private static Message existingMessage = null;
    
    public static GatewayDiscordClient login(String token) {
        return DiscordClient.create(token).login().block();
    }


    public static void sendMessage(GatewayDiscordClient gateWay, String channelID, String message) {
        Snowflake channelId = Snowflake.of(channelID);

        gateWay.getChannelById(channelId)
            .ofType(MessageChannel.class)
            .flatMap(channel -> channel.createMessage(message))
            .then().block();
    }

    public static Mono<ApplicationCommandData> registerReadCommand(GatewayDiscordClient gateway, long applicationId) {

        System.out.println("Creating read command!");

        ApplicationCommandOptionData optionData = ApplicationCommandOptionData.builder()
            .name("name")
            .description("Name of the fic you want to have latest chap updated")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build();
            
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("read")
            .description("Tells the bot that the chapter for the specific fic has been read")
            .addOption(optionData)
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    public static Mono<ApplicationCommandData> registerEndLoopCommand(GatewayDiscordClient gateway, long applicationId) {
        
        System.out.println("Creating endloop command!");
        ApplicationCommandRequest commandRequest1 = ApplicationCommandRequest.builder()
            .name("endloop")
            .description("Tells the bot that the scraping loop should end")
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest1)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    public static Mono<ApplicationCommandData> registerStartLoopCommand(GatewayDiscordClient gateway, long applicationId) {
        
        System.out.println("Creating startLoop command!");
        ApplicationCommandRequest commandRequest1 = ApplicationCommandRequest.builder()
            .name("startloop")
            .description("Tells the bot that the scraping loop should start")
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, commandRequest1)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    public static Mono<Void> handleReadCommand(ChatInputInteractionEvent event) {
        JsonDeserializer jsonDeserializer = new JsonDeserializer();

        System.out.println("Interaction Event Details:");
        System.out.println("Command Name: " + event.getCommandName());
        System.out.println("Options: " + event.getOptions().toString());

        String nameOfFic = event.getOption("name")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("Unknown Fic");



        System.out.println("Recieved name of fic: " + nameOfFic);
        if (jsonDeserializer.matchFicTitle(nameOfFic)) {
            // Fetch all chapter names for the given fic
            List<String> chapters = FicScraper.getAllChapterNames(nameOfFic); // Implement this method

            // Send the select menu with chapters
            return sendPaginatedMenu(event.getClient(), event.getInteraction().getChannelId().asString(), nameOfFic, chapters, 0)
                .then(event.reply("Select a chapter from the menu below."))
                .then();
        } else {
            // Respond if the fic was not found
            return event.reply("Could not find a fic in file that matches the name provided.").then();
        }

    }


    public static Mono<Void> handleEndLoopCommand(ChatInputInteractionEvent event) {
        System.out.println("Interaction Event Details:");
        System.out.println("Command Name: " + event.getCommandName());
        return event.reply(String.format("The loop has now ended!")).then();

    }

    public static Mono<Void> handleStartLoopCommand(ChatInputInteractionEvent event) {
        System.out.println("Interaction Event Details:");
        System.out.println("Command Name: " + event.getCommandName());
        return event.reply(String.format("The loop has now started!")).then();

    }
    
    public static SelectMenu createChapSelectMenu(List<String> allChapterNames) {
        
        List<SelectMenu.Option> options = allChapterNames
            .stream()
            .limit(25)
            .map(chapterName -> SelectMenu.Option.of(chapterName, chapterName))
            .toList();

        return SelectMenu.of("chapList", options);
    }

    // Method to send the paginated menu
    public static Mono<Void> sendPaginatedMenu(GatewayDiscordClient gateway, String channelId, String ficName, List<String> chapterNames, int page) {
        int pageSize = 25; // Number of chapters per page
        int totalPages = (int) Math.ceil((double) chapterNames.size() / pageSize);
    
        
        Snowflake channelSnowflake = Snowflake.of(channelId);
        
        List<String> currentPageChapters = chapterNames.stream()
            .skip(page * pageSize)
            .limit(pageSize)
            .collect(Collectors.toList());
        
        SelectMenu selectMenu = createChapSelectMenu(currentPageChapters);
        ActionRow selectMenuRow = ActionRow.of(selectMenu);
        boolean isFirstPage = page == 0;
        boolean isLastPage = page == totalPages - 1;
        ActionRow navigationRow = createNavigationButtons(page > 0, page < totalPages - 1, isFirstPage, isLastPage);
        
        MessageCreateSpec spec = MessageCreateSpec.builder()
            .content(String.format("Choose the chapter that you have read for '%s':\nPage %d of %d", ficName, page + 1, totalPages))
            .addComponent(selectMenuRow)
            .addComponent(navigationRow)
            .build();

         return gateway.getChannelById(channelSnowflake)
        .ofType(MessageChannel.class)
        .flatMap(channel -> {
            // Check if the message already exists
            if (existingMessage == null) {
                // Create and store the message
                return channel.createMessage(spec)
                    .doOnNext(message -> existingMessage = message) // Store the reference to the created message
                    .then();
            } else {
                // Edit the existing message
                return existingMessage.edit()
                    .withContentOrNull(spec.content().get()) // Unwrap Possible<String> value
                    .withComponents(spec.components().get()) // Unwrap Possible<List<LayoutComponent>> value
                    .then();
            }
        });
    }
    

    private static String extractFicNameFromMessage(String messageContent) {
        // Assuming the message content contains "Choose the chapter that you have read for 'FIC_NAME':"
        int ficNameStart = messageContent.indexOf("for '") + 5;
        int ficNameEnd = messageContent.indexOf("':\nPage");
        if (ficNameStart != -1 && ficNameEnd != -1) {
            return messageContent.substring(ficNameStart, ficNameEnd);
        }
        return "Unknown Fic";
    }

    public static int extractPageFromMessage(String messageContent) {
        // Assuming the message content contains "Page X of Y"
        int pageIndex = messageContent.indexOf("Page ");
        if (pageIndex != -1) {
            int start = pageIndex + 5; // "Page " is 5 characters long
            int end = messageContent.indexOf(" of", start);
            if (end != -1) {
                try {
                    return Integer.parseInt(messageContent.substring(start, end)) - 1; // Subtract 1 to get zero-based page index
                } catch (NumberFormatException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        return 0; 
    }
    

    public static ActionRow createNavigationButtons(boolean hasPreviousPage, boolean hasNextPage, boolean isFirstPage, boolean isLastPage) {
        // Create the Previous button (disabled if on the first page)
        Button firstButton = Button.primary("first_page", "First")
            .disabled(isFirstPage);
        
        Button prevButton = Button.primary("prev_page", "Previous")
            .disabled(!hasPreviousPage);
    
        // Create the Next button (disabled if on the last page)
        Button nextButton = Button.primary("next_page", "Next")
            .disabled(!hasNextPage);
    
        Button lastButton = Button.primary("last_page", "Last")
            .disabled(isLastPage);
        // Add both buttons to an ActionRow
        return ActionRow.of(firstButton, prevButton, nextButton, lastButton);
    }


    public static void handleButtonInteractions(GatewayDiscordClient gateway) {
        gateway.on(ButtonInteractionEvent.class).subscribe(event -> {
            String customId = event.getCustomId();
            String channelId = event.getInteraction().getChannelId().asString();
    
            // Fetch the current message content
            String messageContent = event.getMessage().toString();
    
            // Extract the fic name and current page from the message content
            String ficName = extractFicNameFromMessage(messageContent);
            int currentPage = extractPageFromMessage(messageContent);
            List<String> chapters = FicScraper.getAllChapterNames(ficName);
    
            int newPage;
            switch (customId) {
                case "first_page":
                    newPage = 0;
                    break;
                case "prev_page":
                    newPage = currentPage - 1;
                    break;
                case "next_page":
                    newPage = currentPage + 1;
                    break;
                case "last_page":
                    newPage = (int) Math.ceil((double) chapters.size() / 25) - 1;     
                    break;
                default:
                    return;                                   
            }    
    
            // Send the updated paginated menu
            sendPaginatedMenu(gateway, channelId, ficName, chapters, newPage).subscribe();
    
            // Acknowledge the interaction
            event.acknowledge().subscribe();
        });
    }




    public static void handleSelectMenuInteractions(GatewayDiscordClient gateway) {
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        gateway.on(SelectMenuInteractionEvent.class).subscribe(event -> {
            String ficName = extractFicNameFromMessage(event.getMessage().toString());
            List<String> allChapters = FicScraper.getAllChapterNames(ficName);
            String selectedValue = event.getValues().get(0); // Get the selected value
    
            // Find the index of the selected chapter in the list of all chapters
            int chapterIndex = allChapters.indexOf(selectedValue);
    
            // Set the chapter using the index instead of the content
            if (chapterIndex != -1) {
                jsonDeserializer.setFicChapter(ficName, chapterIndex + 1); // Assuming 1-based index for chapter numbers
                event.reply()
                    .withContent("You selected: " + selectedValue + " (Chapter " + (chapterIndex + 1) + ")")
                    .subscribe();
            } else {
                event.reply()
                    .withContent("Selected chapter not found in the list.")
                    .subscribe();
            }
        });
    }
    

    public static int processChapter(String chapterTitle) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(chapterTitle);
        int chapterNumber = 0;

        if (matcher.find()) {
            String chapterString = matcher.group();
            try {
                int chapter = Integer.parseInt(chapterString);
                chapterNumber = chapter;
                System.out.println("This is the chapter number: " + chapter);
            } catch (NumberFormatException e) {
                System.err.println("Error: " + e.getMessage());
            }
        } else {
            System.err.println("No chapter number found in the string: " + chapterTitle);
        }
        return chapterNumber;
    }
}
