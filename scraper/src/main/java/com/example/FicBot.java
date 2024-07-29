package com.example;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.entity.RestChannel;
import reactor.core.publisher.Mono;

public class FicBot {
    //private final static long serverId = 1259531516854276156L;
    private static String token = "MTI1OTUzMDI4MDMzNTU3NzE3MA.G8JiDR.D2Ed5Kqy3WhXSSBGQRiLI2B9Ku3EUF6ybjIuoU";
    private static long serverId = 1259531516854276156L;
    
    
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
        String reply;
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
            jsonDeserializer.setFicChapter(nameOfFic);
            reply = "Latest chapter for " + nameOfFic + "has been updated!";
        } else {
            reply = "Could not find a fic in file that matches the name provided.";
        }

        return event.reply(reply).then();
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
}
