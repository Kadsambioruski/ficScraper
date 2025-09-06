package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final Map<String, Message> existingMessages = new HashMap<>();
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

    public static ActionRow createNavigationButtons(String menuType, int currentPage, int totalPages, int ficId) {
        boolean isFirstPage = currentPage == 0;
        boolean isLastPage = currentPage == totalPages - 1;
        
        Button firstButton = Button.primary(menuType + ":first_page:" + currentPage + ":" + ficId, "First").disabled(isFirstPage); 
        Button prevButton = Button.primary(menuType + ":prev_page:" + currentPage + ":" + ficId, "Previous").disabled(isFirstPage);
        Button nextButton = Button.primary(menuType + ":next_page:" + currentPage + ":" + ficId, "Next").disabled(isLastPage);
        Button lastButton = Button.primary(menuType + ":last_page:" + currentPage + ":" + ficId, "Last").disabled(isLastPage);

          

        return ActionRow.of(firstButton, prevButton, nextButton, lastButton);
    }

    public Mono<Void> handleButtonInteractions(ButtonInteractionEvent event) {
        String customId = event.getCustomId();

        // Extract the fic name and current page from the message content
        String[] parts = customId.split(":");
        String menuType = parts[0];
        String action = parts[1];  // first_page, prev_page, etc.
        int currentPage = Integer.parseInt(parts[2]);
        int ficId = Integer.parseInt(parts[3]);
        
        List<?> items = Collections.emptyList();
        
        switch (menuType) {
            case "chapList":
                items = ficScraper.getAllChapterNames(ficId); 
                break;
            case "fictionList":
            case "finishList":
                items = ficJsonHandler.getAllFics();
                break;
            default:
                break;
        }

        final List<?> finalItems = items;

        int pageSize = 25;
        int totalPages = (int) Math.ceil((double) finalItems.size() / pageSize);
        
        int newPage;
        switch (action) {
            case "first_page": newPage = 0; break;
            case "prev_page": newPage = Math.max(currentPage - 1, 0) ; break;
            case "next_page": newPage = Math.min(currentPage + 1, totalPages - 1) ; break;
            case "last_page": newPage = totalPages - 1; break;
            default: return Mono.empty();                                   
        }    

        if (menuType.equals("chapList")) {
            // For chapters
            return event.acknowledge().then(sendPaginatedMenu(
                event.getClient(),
                event.getInteraction().getChannelId().asString(),
                finalItems,
                newPage,
                "Choose a chapter",
                menuType,
                name -> (String) name,          // labelMapper
                name -> ficId + ":" + finalItems.indexOf(name), // valueMapper
                ficId
            ));
        } else {
            // For fictions
            return event.acknowledge().then(sendPaginatedMenu(
                event.getClient(),
                event.getInteraction().getChannelId().asString(),
                finalItems,
                newPage,
                "Select a fiction",
                menuType,
                fiction -> ((Fiction) fiction).getTitle(),
                fiction -> Integer.toString(((Fiction) fiction).getFicID()),
                ficId
            ));
        }

    }
    
    public static <T> List<T> getPage(List<T> items, int page, int pageSize) {
        int start = page * pageSize;
        if (start >= items.size()) return new ArrayList<>();
        return items.stream()
            .skip(start)
            .limit(pageSize)
            .collect(Collectors.toList());
    }

    public static int totalPages(int totalItems, int pageSize) {
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    public static <T> SelectMenu createPaginatedSelectMenu(String menuId, List<T> items, int page, int pageSize, java.util.function.Function<T, String> labelMapper, java.util.function.Function<T, String> valueMapper) {
        List<T> pageItems = getPage(items, page, pageSize);
        List<SelectMenu.Option> options = new ArrayList<>();

        for (int i = 0; i < pageItems.size(); i++) {
            T item = pageItems.get(i);
            options.add(SelectMenu.Option.of(labelMapper.apply(item), valueMapper.apply(item)));
        }

        return SelectMenu.of(menuId, options);
    }

    public static <T> MessageCreateSpec createPaginatedMenu(
        String header, String menuId, List<T> items, int page, int pageSize, java.util.function.Function<T, String> labelMapper, java.util.function.Function<T, String> valueMapper, int ficId) {

        int totalPages = totalPages(items.size(), pageSize);
        SelectMenu selectMenu = createPaginatedSelectMenu(
                menuId,
                items,
                page,
                pageSize,
                labelMapper,       // simple default label
                valueMapper // default value
        );

        ActionRow selectRow = ActionRow.of(selectMenu);
        ActionRow navRow = createNavigationButtons(menuId, page, totalPages, ficId);

        return MessageCreateSpec.builder()
                .content(String.format("%s\nPage %d of %d", header, page + 1, totalPages))
                .addComponent(selectRow)
                .addComponent(navRow)
                .build();
    }

    public static <T> MessageEditSpec editPaginatedMenu(
        String header, String menuId, List<T> items, int page, int pageSize, java.util.function.Function<T, String> labelMapper, java.util.function.Function<T, String> valueMapper, int ficId) {

        int totalPages = totalPages(items.size(), pageSize);
        SelectMenu selectMenu = createPaginatedSelectMenu(
                menuId,
                items,
                page,
                pageSize,
                labelMapper,       // simple default label
                valueMapper // default value
        );

        ActionRow selectRow = ActionRow.of(selectMenu);
        ActionRow navRow = createNavigationButtons(menuId, page, totalPages, ficId); // you can generalize button IDs too

        return MessageEditSpec.builder()
                .content(String.format("%s\nPage %d of %d", header, page + 1, totalPages))
                .addComponent(selectRow)
                .addComponent(navRow)
                .build();
    }


    public Mono<Void> handleSelectMenuInteractions(SelectMenuInteractionEvent event) {
        String customId = event.getCustomId();

        switch (customId) {
            case "ficList":
            {
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
                
                return event.deferReply().then(sendPaginatedMenu(
                        event.getClient(),
                        event.getInteraction().getChannelId().asString(),
                        allChapters,
                        0,
                        String.format("Choose the chapter that you have read for '%s' (ID: %d)", fiction.getTitle(), ficId),
                        "chapList",
                        name -> name,
                        name -> ficId + ":" + allChapters.indexOf(name),
                        ficId
                        
                ));
                
            }
            case "chapList":
            {
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
            case "finishFic":
                System.out.println("This shows up if are in finishFic case");
                String selectedValue = event.getValues().get(0); // Get the selected value
                int ficId = Integer.parseInt(selectedValue);
                
                Fiction finishedFiction = ficJsonHandler.getFic(ficId);
                
                ficJsonHandler.moveFicToFinished(finishedFiction);

                return event.reply()
                    .withContent("You have marked '" + finishedFiction.getTitle() + "' as finished!")
                    .then();
            default:
                return event.deferEdit().then();
        }
    }
    


    public static <T> Mono<Void> sendPaginatedMenu(
        GatewayDiscordClient gateway,
        String channelId,
        List<T> items,
        int page,
        String header,
        String menuId,
        java.util.function.Function<T, String> labelMapper,
        java.util.function.Function<T, String> valueMapper,
        int ficId) {

        Snowflake channelSnowflake = Snowflake.of(channelId);
        int maxPageSize = 25;
        
        return gateway.getChannelById(channelSnowflake)
            .ofType(MessageChannel.class)
            .flatMap(channel -> {
                Message existingMessage = existingMessages.get(menuId);
                MessageCreateSpec createSpec = createPaginatedMenu(
                    header, 
                    menuId, 
                    items, 
                    page, 
                    maxPageSize, 
                    labelMapper, 
                    valueMapper,
                    ficId
                );
                if (existingMessage == null) {
                    return channel.createMessage(createSpec)
                            .doOnNext(message -> existingMessages.put(menuId, message))
                            .then();
                } else {
                    // Optionally check if existing message is for a different menu
                    MessageEditSpec editSpec = editPaginatedMenu(
                        header, 
                        menuId, 
                        items, 
                        page, 
                        maxPageSize, 
                        labelMapper, 
                        valueMapper,
                        ficId
                    );
                    return existingMessage.edit(editSpec).then();
                }
                });
    }


}
