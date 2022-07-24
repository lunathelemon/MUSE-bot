package io.github.profjb58.musebot.listeners;

import io.github.profjb58.musebot.MuseBot;
import io.github.profjb58.musebot.exceptions.SingleInstanceException;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.update.GuildScheduledEventUpdateStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class EventListener extends ListenerAdapter {

    private static final int EVENT_COMPLETED_KEY = 3;
    private final MuseBot instance;

    public EventListener(MuseBot instance) {
        this.instance = instance;
    }

    @Override
    public void onGuildScheduledEventUpdateStatus(@NotNull GuildScheduledEventUpdateStatusEvent event) {
        // Called when an event is completed
        if(event.getNewStatus().getKey() == EVENT_COMPLETED_KEY) {
            try {
                instance.getCurrentContest().nextStage(event.getGuild());
            } catch (SingleInstanceException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if(event.getGuild().getId().equals(instance.getEnvConfig().get("GUILD_ID"))) { // Joined the MUSE discord server
            instance.getBotChannel(event.getJDA()).sendMessage("🟢 MUSE Bot Started!").queue();
            instance.getCommandManager().addCommands(event);
        }
        else { // Joined another server, forces it to leave
            var guildChannel = (GuildChannel) event.getGuild().getDefaultChannel();
            if(guildChannel instanceof MessageChannel messageChannel)
                messageChannel.sendMessage("🔥 The MUSE bot is only available to use within the **MUSE discord server:** https://discord.gg/rhYm4WYCW8").queue();
            event.getGuild().leave().complete();
        }
        instance.getCurrentContest().load(); // Load the current contest
    }
}
