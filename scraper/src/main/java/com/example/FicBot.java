package com.example;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.entity.RestChannel;
import reactor.core.publisher.Mono;

public class FicBot {
    
    public static GatewayDiscordClient login(String token) {
        DiscordClient client = DiscordClient.create(token); 
        return client.login().block();
    }


    public static void sendMessage(GatewayDiscordClient gateWay, String channelID, String message) {
        Snowflake channelId = Snowflake.of(channelID);

        gateWay.getChannelById(channelId)
            .ofType(MessageChannel.class)
            .flatMap(channel -> channel.createMessage(message))
            .then().block();
    }
}
