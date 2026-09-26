package com.example.bot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.Config;
import com.example.FicScraper;
import com.example.model.Fiction;
import com.example.storage.FicJsonHandler;

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
    private static final Logger log = LoggerFactory.getLogger(FicBot.class);
    private static final Dotenv dotEnv = Dotenv.configure().directory("scraper").load();
    private final String token;
    private static final String DISCORD_SERVER_ID = dotEnv.get("DISCORD_SERVER_ID");
    private static final String DISCORD_CHANNEL_ID = dotEnv.get("DISCORD_CHANNEL_ID");
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

    public void receiveDiscCommand(GatewayDiscordClient gateway) {
        gateway.on(ChatInputInteractionEvent.class)
            .flatMap(event -> {
                switch (event.getCommandName()) {
                    case "read": return handleReadCommand(event);
                    case "finish": return handleFinishFicCommand(event);
                    case "endloop": return handleEndLoopCommand(event);
                    case "startloop": return handleStartLoopCommand(event);
                    case "add": return handleAddFicCommand(event);
                    case "wordcount": return handleWordCountCommand(event);
                    default: return Mono.empty();
                }
            })
            .onErrorResume(err -> {
                log.error("Error handling command", err);
                return Mono.empty();
            })
            .then(gateway.onDisconnect())
            .block();
    }

    public static Mono<Void> handleReadCommand(ChatInputInteractionEvent event) {
        int page = 0;
        
        return event.deferReply()
            .then(Mono.fromCallable(FicScraper::getUpdatedFics))
            .flatMap(allUpdatedFics -> {
                if (allUpdatedFics.isEmpty()) {
                    return event.editReply("No fictions with a new chapter have been found. (This command only shows updated fics)").then();
                }

            return InteractionManager.sendPaginatedMenu(
                event,
                allUpdatedFics,
                page,
                "Select the fiction with a new chapter that you have read:",
                "fictionList",
                fic -> fic.getTitle(),
                fic -> String.valueOf(fic.getFicID()),
                0
            );
        });
    }

    public static Mono<Void> handleAddFicCommand(ChatInputInteractionEvent event) {
        log.debug("Command: {}, Options: {}", event.getCommandName(), event.getOptions());

        String ficlink = event.getOption("ficlink")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("Unknown Fic");

        log.info("Received link of fic: {}", ficlink);

        try {
            putFicInJson(ficlink);
            return event.reply("The fic with the link:" + ficlink + " has been added to the list").then();
        } catch (Exception e) {
            return event.reply("Failed to add fic: " + e.getMessage()).then();
        }
    }

    public static Mono<Void> handleFinishFicCommand(ChatInputInteractionEvent event) {
        List<Fiction> allFics = ficJsonHandler.getAllFics();

        return event.deferReply()
        .then(InteractionManager.sendPaginatedMenu(
            event,
            allFics,
            0,
            "Select the fiction that you have finished:",
            "finishList",
            fic -> fic.getTitle(),
            fic -> String.valueOf(fic.getFicID()),
            0
            )
        );
    }

    public static Mono<Void> handleEndLoopCommand(ChatInputInteractionEvent event) {
        log.debug("Received command: {}", event.getCommandName());
        return event.deferReply()
            .then(Mono.fromRunnable(() -> {
                runLoop = false;
                if (loopThread != null && loopThread.isAlive()) {
                    loopThread.interrupt();
                    try {
                        loopThread.join();
                        log.info("Loop thread stopped");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while stopping loop thread", e);
                    }
                } else {
                    log.info("Loop thread was not running");
                }
            }))
            .then(event.createFollowup("The loop has now ended!").then());
    }

    public static Mono<Void> handleStartLoopCommand(ChatInputInteractionEvent event) {
        runLoop = true;
        log.debug("Received command: {}", event.getCommandName());

        return event.deferReply()
            .then(Mono.fromRunnable(() -> {
                if (loopThread == null || !loopThread.isAlive()) {
                    runLoop = true;
                    GatewayDiscordClient client = event.getClient();
                    loopThread = new Thread(() -> scrapeFicLoop(client));
                    loopThread.start();
                    log.info("Loop thread started");
                } else {
                    log.info("Loop thread already running");
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

    public static Mono<Void> handleWordCountCommand(ChatInputInteractionEvent event) {
        return event.reply(String.format("Total words read from finished fics: %,d", Config.ficJsonHandler().getTotalReadWords()));
    }

    public static void putFicInJson(String newLinkToFic) {
        log.info("Adding new fic: {}", newLinkToFic);
        FicScraper ficScraper = new FicScraper();
        Fiction newFic = ficScraper.ficInformation(newLinkToFic);

        ficJsonHandler.addFic(newFic);

        log.info("Fic added to JSON file: {}", newFic);
    }

    public static void scrapeFicLoop(GatewayDiscordClient gateWay) {
        FicScraper ficScraper = new FicScraper();
        final Duration interval = Duration.ofHours(3);
        LocalDateTime nextTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Loop has started!");
        Map<Integer, Integer> lastSeenChapters = new HashMap<>();
        String message;

        while (runLoop) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(nextTime)) {
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
                    List<String> chapterNames = ficScraper.getAllChapterNames(fiction);

                    if (chapterNames == null || chapterNames.isEmpty()) {
                        log.warn("Skipping {}: could not retrieve chapters.", fiction.getTitle());
                        continue;
                    }
                    int currentChapCount = chapterNames.size();

                    if (ficScraper.checkIfStubbed(fiction, currentChapCount)) {
                        message = String.format("Seems like %s has been STUBBED! New latest chapter amount is: %d. Updating fiction to the new latest chapter! Here is the link: %s", fiction.getTitle(), currentChapCount, ficScraper.nextChapFicLink(fiction));
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
                                    ficScraper.nextChapFicLink(fiction));

                            sendMessage(gateWay, message).subscribe();
                        }
                    }
                } catch (Exception e) {
                    log.error("Error during scraping loop for {}", fiction.getTitle(), e);
                }
            }
        }
        LocalDateTime endTime = LocalDateTime.now();
        long totalTime = startTime.until(endTime, ChronoUnit.HOURS);
        log.info("Uptime of loop: {}hours.", totalTime);
    }

    public Mono<Void> registerCommands(GatewayDiscordClient gateway) {
        long applicationId = gateway.getRestClient().getApplicationId().block();

        Mono<ApplicationCommandData> readCommand = FicBot.registerReadCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> finishCommand = FicBot.registerFinishCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> endLoopCommand = FicBot.registerEndLoopCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> startLoopCommand = FicBot.registerStartLoopCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> addFicCommand = FicBot.registerAddFicCommand(gateway, DISCORD_SERVER_ID, applicationId);
        Mono<ApplicationCommandData> wordCountCommand = FicBot.registerWordCountCommand(gateway, DISCORD_SERVER_ID, applicationId);

        return Mono.when(readCommand, finishCommand, endLoopCommand, startLoopCommand, addFicCommand, wordCountCommand)
                .doOnSuccess(_ -> log.info("Commands registered successfully"))
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
        log.debug("Creating read command");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("read")
            .description("Tells the bot that the chapter for the specific fic has been read")
            .build();

        return gateway.getRestClient().getApplicationService()
            .createGuildApplicationCommand(applicationId, guildId, commandRequest)
            .onErrorResume(e -> {
                log.error("Failed to register read command", e);
                return Mono.empty();
            });
    }

    public static Mono<ApplicationCommandData> registerWordCountCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        log.debug("Creating wordcount command");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("wordcount")
            .description("Get the total word count of the fics you have finished reading.")
            .build();

        return gateway.getRestClient().getApplicationService()
            .createGuildApplicationCommand(applicationId, guildId, commandRequest)
            .onErrorResume(e -> {
                log.error("Failed to register wordcount command", e);
                return Mono.empty();
            });
    }

    public static Mono<ApplicationCommandData> registerFinishCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        log.debug("Creating finish command");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("finish")
            .description("Tells the bot that you have finished reading the fiction")
            .build();

        return gateway.getRestClient().getApplicationService()
            .createGuildApplicationCommand(applicationId, guildId, commandRequest)
            .onErrorResume(e -> {
                log.error("Failed to register finish command", e);
                return Mono.empty();
            });
    }

    public static Mono<ApplicationCommandData> registerAddFicCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        log.debug("Creating add command");

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
                log.error("Failed to register add command", e);
                return Mono.empty();
            });
    }

    public static Mono<ApplicationCommandData> registerEndLoopCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        log.debug("Creating endloop command");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("endloop")
            .description("Tells the bot that the scraping loop should end")
            .build();

        return gateway.getRestClient().getApplicationService()
            .createGuildApplicationCommand(applicationId, guildId, commandRequest)
            .onErrorResume(e -> {
                log.error("Failed to register endloop command", e);
                return Mono.empty();
            });
    }

    public static Mono<ApplicationCommandData> registerStartLoopCommand(GatewayDiscordClient gateway, String guildIdString, long applicationId) {
        long guildId = Long.parseLong(guildIdString);
        log.debug("Creating startloop command");

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
            .name("startloop")
            .description("Tells the bot that the scraping loop should start")
            .build();

        return gateway.getRestClient().getApplicationService()
            .createGuildApplicationCommand(applicationId, guildId, commandRequest)
            .onErrorResume(e -> {
                log.error("Failed to register startloop command", e);
                return Mono.empty();
            });
    }
}
