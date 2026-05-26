package com.example;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.github.cdimascio.dotenv.Dotenv;
import reactor.core.publisher.Mono;


public class FicBot {
    final private static Dotenv dotEnv = Dotenv.configure().directory("scraper").load();
    private final String token;
    final private static String DISCORD_SERVER_ID = dotEnv.get("DISCORD_SERVER_ID");
    final private static String DISCORD_CHANNEL_ID = dotEnv.get("DISCORD_CHANNEL_ID"); 
    private static final FicJsonHandler ficJsonHandler = Config.ficJsonHandler();
    private final InteractionManager interactionManager;

    private GatewayDiscordClient client;
    private static volatile boolean runLoop;
    private static volatile Thread loopThread;
    
    public FicBot(String token) {
        this.token = token;
        this.interactionManager = new InteractionManager();

    }
    
    public static GatewayDiscordClient login(String token) {
        return DiscordClient.create(token).login().block();
    }
 
    public void start() {
        client = login(token);
        registerCommands(client).subscribe();
        interactionManager.registerListeners(client);
        receiveDiscCommand(client);
    }
    
    public void receiveDiscCommand(GatewayDiscordClient gateway){
        gateway.on(ChatInputInteractionEvent.class)
            .flatMap(event -> {
                switch (event.getCommandName()) {
                    case "read": return handleReadCommand(event);
                    case "finish": return handleFinishFicCommand(event);
                    case "endloop": return handleEndLoopCommand(event);
                    case "startloop": return handleStartLoopCommand(event);
                    case "add": return handleAddFicCommand(event);
                    default: return Mono.empty();
                }
            })
            .onErrorResume(err -> {
                err.printStackTrace();
                return Mono.empty();
            })
            .then(gateway.onDisconnect()) // Chaining with the disconnect operation
            .block(); // Block until completion
    }

    public static Mono<Void> handleReadCommand(ChatInputInteractionEvent event) {
        List<Fiction> allUpdatedFics = FicScraper.getUpdatedFics();

        if (allUpdatedFics.isEmpty()) {
            return event.reply("No fictions with a new chapter have been found. (This command only shows updated fics)").then();
        }

        return event.deferReply().then(InteractionManager.sendPaginatedMenu(
            event.getClient(),
            event.getInteraction().getChannelId().asString(),
            allUpdatedFics,
            0, // start at page 0
            "Select the fiction with a new chapter that you have read:",
            "ficList", // customId for finishing fics
            fic -> fic.getTitle(), // display name
            fic -> String.valueOf(fic.getFicID()), // value sent on selection
            0
        ));
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
        
        putFicInJson(ficlink);
        return event.reply(String.format("The fic with the link:" + ficlink + " has been added to the list")).then();
    }

    public static Mono<Void> handleFinishFicCommand(ChatInputInteractionEvent event) {
        List<Fiction> allFics = ficJsonHandler.getAllFics();

        return event.deferReply().then(InteractionManager.sendPaginatedMenu(
            event.getClient(),
            event.getInteraction().getChannelId().asString(),
            allFics,
            0, // start at page 0
            "Select the fiction that you have finished:",
            "finishFic", // customId for finishing fics
            fic -> fic.getTitle(), // display name
            fic -> String.valueOf(fic.getFicID()), // value sent on selection
            0
        ));
    }

    public static Mono<Void> handleEndLoopCommand(ChatInputInteractionEvent event) {
        System.out.println("Interaction Event Details:");
        System.out.println("Command Name: " + event.getCommandName());
        return event.deferReply()
            .then(Mono.fromRunnable(() -> {
                runLoop = false;
                if (loopThread != null && loopThread.isAlive()) {
                    loopThread.interrupt();
                    try {
                        loopThread.join();
                        System.out.println("Loop thread stopped");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Interrupted while stopping loop thread");
                    }
                } else {
                    System.out.println("Loop thread was not running");
                }
            }))
            .then(event.createFollowup("The loop has now ended!").then());
    }

    public static Mono<Void> handleStartLoopCommand(ChatInputInteractionEvent event) {
        runLoop = true;

        System.out.println("Interaction Event Details:");
        System.out.println("Command Name: " + event.getCommandName());

        return event.deferReply()
            .then(Mono.fromRunnable(() -> {
                if (loopThread == null || !loopThread.isAlive()) {
                    runLoop = true;
                    GatewayDiscordClient client = event.getClient();
                    loopThread = new Thread(() -> scrapeFicLoop(client));
                    loopThread.start();
                    System.out.println("Loop thread started");
                } else {
                    System.out.println("Loop thread already running");
                }
            }))
            .then(Mono.defer(() -> {
                if (loopThread != null && loopThread.isAlive()) {
                    return event.createFollowup("The loop has now started!").then();
                } else {
                    return event.createFollowup("The loop was already running!").then();
                }
            }));
    }

    public static void putFicInJson(String newLinkToFic){
        System.out.println("Put in link to new fic: ");
        // TODO parse through input to check link format
        FicScraper ficScraper = new FicScraper();
        Fiction newFic = ficScraper.ficInformation(newLinkToFic);
        
        ficJsonHandler.addFic(newFic);
        
        System.out.println("Fic added to JSON file: " + newFic);
    }
    
    public static void scrapeFicLoop(GatewayDiscordClient gateWay){
        FicScraper ficScraper = new FicScraper();
        final Duration interval = Duration.ofHours(3);
        LocalDateTime nextTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("Loop has started!");
        Map<Integer, Integer> lastSeenChapters = new HashMap<>();
        String message;
        
        while (runLoop) {
            LocalDateTime now = LocalDateTime.now();
            if(now.isBefore(nextTime)) {
                long sleepMillis = Duration.between(now, nextTime).toMillis();
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            nextTime = nextTime.plus(interval);

            List<Fiction> allFictions = ficJsonHandler.getAllFics();
            for (Fiction fiction : allFictions) {
                try {
                    Document doc;
                    try {
                        doc = Config.fetch(fiction.getFicLink());
                    } catch (IOException e) {
                        System.err.println("Failed to fetch " + fiction.getTitle() + ": " + e.getMessage());
                        continue;
                    }
                    
                    String chapText = doc.select("div.portlet.light > div.portlet-title > div.actions > span").text();
                    int currentChapCount = Integer.parseInt(chapText.split(" ")[0]);
                    
                    if (ficScraper.checkIfStubbed(fiction, currentChapCount)) {
                        message = String.format("Seems like %s has been STUBBED! New latest chapter amount is: %d. Updating fiction to the new latest chapter! Here is the link: %s", fiction.getTitle(), currentChapCount, ficScraper.nextChapFicLink(fiction, doc));
                        ficJsonHandler.setFicChapter(fiction, currentChapCount);
                        sendMessage(gateWay, message).subscribe();
                    }
                    if (ficScraper.checkUpdatedChap(fiction, currentChapCount)) {
                        Integer lastSeen = lastSeenChapters.get(fiction.getFicID());
                        if (lastSeen == null || currentChapCount > lastSeen) {
                            lastSeenChapters.put(fiction.getFicID(), currentChapCount);
                            
                            message = String.format("New chapter found! New chapter found in %s is: %d\nLink: %s", 
                                    fiction.getTitle(),
                                    fiction.getChapAmount() + 1, 
                                    ficScraper.nextChapFicLink(fiction, doc));

                            sendMessage(gateWay, message).subscribe();
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error during scraping loop: " + e.getMessage());
                }
            }
            
           
        }
        LocalDateTime endTime = LocalDateTime.now();
        long totalTime = startTime.until(endTime, ChronoUnit.HOURS);
        System.out.println("Uptime of loop: " + totalTime + "hours.");
    }


    public Mono<Void> registerCommands(GatewayDiscordClient gateway) {
        long applicationId = gateway.getRestClient().getApplicationId().block();

        Mono<ApplicationCommandData> readCommand = FicBot.registerReadCommand(gateway, DISCORD_SERVER_ID,  applicationId);
        Mono<ApplicationCommandData> finishCommand = FicBot.registerFinishCommand(gateway, DISCORD_SERVER_ID,  applicationId);
        Mono<ApplicationCommandData> endLoopCommand = FicBot.registerEndLoopCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> startLoopCommand = FicBot.registerStartLoopCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> addFicCommand = FicBot.registerAddFicCommand(gateway, DISCORD_SERVER_ID, applicationId);

        return Mono.when(readCommand, finishCommand, endLoopCommand, startLoopCommand, addFicCommand)
                .doOnSuccess(_ -> System.out.println("Commands registered successfully"))
                .then(sendMessage(gateway, "Bot is now ready and online!"));
    }

    public static Mono<Void> sendMessage(GatewayDiscordClient gateWay, String message) {
        Snowflake channelId = Snowflake.of(DISCORD_CHANNEL_ID);

        return gateWay.getChannelById(channelId)
            .ofType(MessageChannel.class)
            .flatMap(channel -> channel.createMessage(message))
            .then();
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

    public static Mono<ApplicationCommandData> registerFinishCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        System.out.println("Creating finish command!");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("finish")
            .description("Tells the bot that you have finished reading the fiction")
            .build();

            return gateway.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, commandRequest)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.empty();
                });
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




}
