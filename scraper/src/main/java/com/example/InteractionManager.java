package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import reactor.core.publisher.Mono;

public class InteractionManager {
    private static Message existingMessage = null;
    private static final FicJsonHandler ficJsonHandler = new FicJsonHandler();
    private final FicScraper ficScraper;
    
    public InteractionManager() {
        this.ficScraper = new FicScraper();
    }

    public void registerListeners(GatewayDiscordClient client) {
        client.on(ButtonInteractionEvent.class)
            .flatMap(this::handleButtonInteractions)
            .subscribe();

        client.on(SelectMenuInteractionEvent.class)
            .flatMap(this::handleSelectMenuInteractions)
            .subscribe();
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

    public Mono<Void> handleButtonInteractions(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        String channelId = event.getInteraction().getChannelId().asString();

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
            default: return Mono.empty();                                   
        }    

        // Send the updated paginated menu
        return sendPaginatedMenu(event.getClient(), channelId, ficId, chapters, newPage).then(event.acknowledge());

    }
    



    public Mono<Void> handleSelectMenuInteractions(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();

        if (customId.equals("ficList")) {
            String selectedValue = event.getValues().get(0); // Get the selected value
            int ficId = Integer.parseInt(selectedValue);
            
            Fiction fiction = ficJsonHandler.getFic(ficId);
            List<String> allChapters = ficScraper.getAllChapterNames(ficId);
            
            if (allChapters == null || allChapters.isEmpty()) {
                return event.reply()
                .withContent("⚠ Could not retrieve chapters for `" + fiction.getTitle() + "`. Please try again later.")
                .then();
            }

            FicScraper.clearFicUpdate(ficId);

            Mono<Void> sendMenuMono = sendPaginatedMenu(event.getClient(), 
                event.getInteraction().getChannelId().asString(), 
                ficId, 
                allChapters, 0
            );

            return sendMenuMono.then(event.acknowledge());

        } else if (customId.equals("chapList")) {
            String selectedValue = event.getValues().get(0); // now "ficId:chapterIndex"
            String[] parts = selectedValue.split(":");
            int selectedFicId = Integer.parseInt(parts[0]);
            int chapterIndex = Integer.parseInt(parts[1]);
            
            String ficName = ficJsonHandler.getFic(selectedFicId).getTitle();
            List<String> allChapters = ficScraper.getAllChapterNames(selectedFicId);

            if (allChapters == null || allChapters.isEmpty()) {
                return event.reply()
                    .withContent("⚠ Could not retrieve chapters for `" + ficName + "`. Please try again later.")
                    .then();
            }
            
            
            if (chapterIndex >= 0 && chapterIndex < allChapters.size() + 1) {
                String chapterName = allChapters.get(chapterIndex);
                ficJsonHandler.setFicChapter(selectedFicId, chapterIndex + 1);
                return event.reply()
                    .withContent("You selected: " + chapterName + " (Chapter " + (chapterIndex + 1) + ")")
                    .then();
            } else {
                return event.reply()
                    .withContent("Selected chapter not found in the list")
                    .then();
            }
        }

        return Mono.empty();
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

    public static SelectMenu createChapSelectMenu(List<String> currentPageChapters, int ficId, int pageOffset) {
        List<SelectMenu.Option> options = new ArrayList<>();

        for (int i = 0; i < currentPageChapters.size(); i++) {
            String chapterName = currentPageChapters.get(i);
            int globalIndex = pageOffset + i; // convert to absolute index
            options.add(SelectMenu.Option.of(chapterName, ficId + ":" + globalIndex)); 
        }

        return SelectMenu.of("chapList", options);
    }
}
