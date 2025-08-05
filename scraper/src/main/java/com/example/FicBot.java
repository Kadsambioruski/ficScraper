package com.example;
import java.util.ArrayList;
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
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class FicBot {
    private static Message existingMessage = null;
    private static final FicJsonHandler ficJsonHandler = new FicJsonHandler();
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

    public static Mono<ApplicationCommandData> registerReadCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        System.out.println("Creating read command!");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("read")
            .description("Tells the bot that the chapter for the specific fic has been read")
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, commandRequest)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    
    public static SelectMenu createFicSelectMenu(List<Fiction> allFics) {
    
        List<SelectMenu.Option> options = allFics.stream()
            .limit(25)
            .map(fiction -> SelectMenu.Option.of(fiction.getTitle(), ""+ fiction.getFicID()))
            .toList();

        return SelectMenu.of("ficList", options).withPlaceholder("Select a fiction");
    }

    public static Mono<ApplicationCommandData> registerAddFicCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        System.out.println("Creating add command!");

        ApplicationCommandOptionData optionData = ApplicationCommandOptionData.builder()
            .name("ficlink")
            .description("Name of the fic you want to add")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build();
            
        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("add")
            .description("Tells the bot to add the fic given to the list of all fics being read")
            .addOption(optionData)
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, commandRequest)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    public static Mono<ApplicationCommandData> registerEndLoopCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);

        System.out.println("Creating endloop command!");
        ApplicationCommandRequest commandRequest1 = ApplicationCommandRequest.builder()
            .name("endloop")
            .description("Tells the bot that the scraping loop should end")
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, commandRequest1)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    public static Mono<ApplicationCommandData> registerStartLoopCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        System.out.println("Creating startLoop command!");
        ApplicationCommandRequest commandRequest1 = ApplicationCommandRequest.builder()
            .name("startloop")
            .description("Tells the bot that the scraping loop should start")
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, commandRequest1)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
    }

    public static Mono<Void> handleReadCommand(ChatInputInteractionEvent event) {
        List<Fiction> allUpdatedFics = FicScraper.getUpdatedFics();

        if (allUpdatedFics.isEmpty()) {
            return event.reply("No fictions with a new chapter have been found. (This command only shows updated fics)").then();
        }

        SelectMenu ficSelectMenu = createFicSelectMenu(allUpdatedFics);
        ActionRow actionRow = ActionRow.of(ficSelectMenu);

        return event.reply()
            .withContent("Select the fiction with a new chapter that you have read:")
            .withComponents(actionRow)
            .withEphemeral(true);
            
    }

    public static Mono<Void> handleAddFicCommand(ChatInputInteractionEvent event) {
        System.out.println("Interaction Event Details:");
        System.out.println("Command Name: " + event.getCommandName());
        System.out.println("Options: " + event.getOptions().toString());

        String ficlink = event.getOption("ficlink")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("Unknown Fic");



        System.out.println("Recieved link of fic: " + ficlink);
        
        Main.putFicInJson(ficlink);
        return event.reply(String.format("The fic with the link:" + ficlink + " has been added to the list")).then();
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
    
    public static SelectMenu createChapSelectMenu(List<String> currentPageChapters, int ficId, int pageOffset) {
        List<SelectMenu.Option> options = new ArrayList<>();

        for (int i = 0; i < currentPageChapters.size(); i++) {
            String chapterName = currentPageChapters.get(i);
            int globalIndex = pageOffset + i; // convert to absolute index
            options.add(SelectMenu.Option.of(chapterName, ficId + ":" + globalIndex)); 
        }

        return SelectMenu.of("chapList", options);
    }

    

    public static MessageCreateSpec createPaginatedMenuCreateSpec(int ficId, List<String> chapterNames, int page) {
        int pageSize = 25;
        int totalPages = (int) Math.ceil((double) chapterNames.size() / pageSize);

        List<String> currentPageChapters = chapterNames.stream()
            .skip(page * pageSize)
            .limit(pageSize)
            .collect(Collectors.toList());

        Fiction fiction = ficJsonHandler.getFic(ficId);
        String ficTitle = fiction.getTitle();

        SelectMenu selectMenu = createChapSelectMenu(currentPageChapters, ficId, page * pageSize);
        ActionRow selectMenuRow = ActionRow.of(selectMenu);
        ActionRow navigationRow = createNavigationButtons(ficId, page, totalPages);

        return MessageCreateSpec.builder()
            .content(String.format("Choose the chapter that you have read for '%s'(ID: %d):\nPage %d of %d",
                    ficTitle, ficId, page + 1, totalPages))
            .addComponent(selectMenuRow)
            .addComponent(navigationRow)
            .build();
    }


    public static MessageEditSpec createPaginatedMenuEditSpec(int ficId, List<String> chapterNames, int page) {
        int pageSize = 25;
        int totalPages = (int) Math.ceil((double) chapterNames.size() / pageSize);

        List<String> currentPageChapters = chapterNames.stream()
            .skip(page * pageSize)
            .limit(pageSize)
            .collect(Collectors.toList());

        Fiction fiction = ficJsonHandler.getFic(ficId);
        String ficTitle = fiction.getTitle();

            SelectMenu selectMenu = createChapSelectMenu(currentPageChapters, ficId, page * pageSize);
        ActionRow selectMenuRow = ActionRow.of(selectMenu);
        ActionRow navigationRow = createNavigationButtons(ficId, page, totalPages);

        return MessageEditSpec.builder()
            .content(String.format("Choose the chapter that you have read for '%s'(ID: %d):\nPage %d of %d",
                    ficTitle, ficId, page + 1, totalPages))
            .addComponent(selectMenuRow)
            .addComponent(navigationRow)
            .build();
    }

    public static Mono<Void> sendPaginatedMenu(GatewayDiscordClient gateway, String channelId, int ficId, List<String> chapterNames, int page) {
        Snowflake channelSnowflake = Snowflake.of(channelId);

        return gateway.getChannelById(channelSnowflake)
            .ofType(MessageChannel.class)
            .flatMap(channel -> {
                if (existingMessage == null) {
                    MessageCreateSpec createSpec = createPaginatedMenuCreateSpec(ficId, chapterNames, page);
                    return channel.createMessage(createSpec)
                        .doOnNext(message -> existingMessage = message)
                        .then();
                } else {
                    // Optionally check if the existing message is for a different fic, then recreate it:
                    String content = existingMessage.getContent();
                    if (content == null || !content.contains("(ID: " + ficId + ")")) {
                        MessageCreateSpec createSpec = createPaginatedMenuCreateSpec(ficId, chapterNames, page);
                        return channel.createMessage(createSpec)
                            .doOnNext(message -> existingMessage = message)
                            .then();
                    }
                    // Otherwise edit the existing message
                    MessageEditSpec editSpec = createPaginatedMenuEditSpec(ficId, chapterNames, page);
                    return existingMessage.edit(editSpec).then();
                }
            });
    }    

    public static ActionRow createNavigationButtons(int ficId, int currentPage, int totalPages) {
        boolean isFirstPage = currentPage == 0;
        boolean isLastPage = currentPage == totalPages - 1;

        Button firstButton = Button.primary("first_page:" + ficId + ":" + currentPage, "First").disabled(isFirstPage);
        Button prevButton = Button.primary("prev_page:" + ficId + ":" + currentPage, "Previous").disabled(isFirstPage);
        Button nextButton = Button.primary("next_page:" + ficId + ":" + currentPage, "Next").disabled(isLastPage);
        Button lastButton = Button.primary("last_page:" + ficId + ":" + currentPage, "Last").disabled(isLastPage);

        return ActionRow.of(firstButton, prevButton, nextButton, lastButton);
    }

    public static void handleButtonInteractions(GatewayDiscordClient gateway) {
        gateway.on(ButtonInteractionEvent.class).subscribe(event -> {
            String customId = event.getCustomId();
            String channelId = event.getInteraction().getChannelId().asString();
            FicScraper ficScraper = new FicScraper();
    
            // Extract the fic name and current page from the message content
            String[] parts = customId.split(":");
            String action = parts[0];  // first_page, prev_page, etc.
            int ficId = Integer.parseInt(parts[1]);
            int currentPage = Integer.parseInt(parts[2]);
            List<String> chapters = ficScraper.getAllChapterNames(ficId);
    
            int newPage;
            switch (action) {
                case "first_page": newPage = 0; break;
                case "prev_page": newPage = currentPage - 1; break;
                case "next_page": newPage = currentPage + 1; break;
                case "last_page": newPage = (int) Math.ceil((double) chapters.size() / 25) - 1; break;
                default: return;                                   
            }    
    
            // Send the updated paginated menu
            sendPaginatedMenu(gateway, channelId, ficId, chapters, newPage).subscribe();
    
            // Acknowledge the interaction
            event.acknowledge().subscribe();
        });
    }



    public static void handleSelectMenuInteractions(GatewayDiscordClient discordClient) {
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        FicScraper ficScraper = new FicScraper();

        discordClient.on(SelectMenuInteractionEvent.class).subscribe(event -> {
            String customId = event.getCustomId();
            if (customId.equals("ficList")) {
                String selectedValue = event.getValues().get(0); // Get the selected value
                int ficId = Integer.parseInt(selectedValue);
                
                Fiction fiction = ficJsonHandler.getFic(ficId);
                List<String> allChapters = ficScraper.getAllChapterNames(ficId);
                
                if (allChapters == null || allChapters.isEmpty()) {
                    event.reply()
                    .withContent("⚠ Could not retrieve chapters for `" + fiction.getTitle() + "`. Please try again later.")
                    .subscribe();
                    return; // Stop execution to prevent NPE
                }

                FicScraper.clearFicUpdate(ficId);

                sendPaginatedMenu(discordClient, 
                    event.getInteraction().getChannelId().asString(), 
                    ficId, 
                    allChapters, 0
                ).subscribe();


                event.acknowledge().subscribe();

            } else if (customId.equals("chapList")) {
                String selectedValue = event.getValues().get(0); // now "ficId:chapterIndex"
                String[] parts = selectedValue.split(":");
                int selectedFicId = Integer.parseInt(parts[0]);
                int chapterIndex = Integer.parseInt(parts[1]);
                
                String ficName = ficJsonHandler.getFic(selectedFicId).getTitle();

                List<String> allChapters = ficScraper.getAllChapterNames(selectedFicId);  
                if (allChapters == null || allChapters.isEmpty()) {
                    event.reply()
                        .withContent("⚠ Could not retrieve chapters for `" + ficName + "`. Please try again later.")
                        .subscribe();
                    return; // Prevents NPE
                }
                
                
                if (chapterIndex >= 0 && chapterIndex < allChapters.size() + 1) {
                    String chapterName = allChapters.get(chapterIndex);
                    jsonDeserializer.setFicChapter(selectedFicId, chapterIndex + 1);
                    event.reply()
                        .withContent("You selected: " + chapterName + " (Chapter " + (chapterIndex + 1) + ")")
                        .subscribe();
                } else {
                    event.reply().withContent("Selected chapter not found in the list").subscribe();
                }
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
