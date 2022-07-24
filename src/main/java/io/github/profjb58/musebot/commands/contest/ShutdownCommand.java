package io.github.profjb58.musebot.commands.contest;

import io.github.profjb58.musebot.commands.Command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class ShutdownCommand extends Command {

    @Override
    protected void onCommand(@NotNull SlashCommandInteractionEvent interaction) {
        interaction.reply("🛑 Bot shutting down...").complete();
        interaction.getJDA().shutdown();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("shutdown", "Shutdown the MUSE bot");
    }
}
