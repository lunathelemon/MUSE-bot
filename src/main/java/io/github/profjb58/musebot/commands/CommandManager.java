package io.github.profjb58.musebot.commands;

import io.github.profjb58.musebot.MuseBot;
import io.github.profjb58.musebot.commands.contest.ContestCommand;
import io.github.profjb58.musebot.commands.contest.ShutdownCommand;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager extends ListenerAdapter {
    private final WeakHashMap<String, Command> activeCommands = new WeakHashMap<>();

    public CommandManager(MuseBot instance) {
        var enabledCommands = List.of(
                new ContestCommand(instance),
                new ShutdownCommand()
        );
        for(var command : enabledCommands)
            activeCommands.put(command.getCommandData().getName(), command);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (activeCommands.containsKey(event.getName())) {
            var command = activeCommands.get(event.getName());

            // Check if the command is not from a Guild and a private response is not expected, disable it sending a warning message
            if (!event.isFromGuild() && !command.expectPrivateResponse) {
                event.reply("This command cannot be used in direct messages 💬").queue();
                return;
            }
            // Defer the reply if we should not answer fast
            if (!command.answerFast) {
                event.deferReply(command.isEphemeral).queue();
                if (command.isEphemeral)
                    event.getHook().setEphemeral(true);
            }
            command.onCommand(event);
        }
    }

    public void addCommands(@NotNull GenericGuildEvent event) {
        List<CommandData> commandsData = activeCommands.values().stream()
                .map(Command::getCommandData)
                .toList();
        try {
            event.getGuild().updateCommands().addCommands(commandsData).queue();
        } catch (IllegalArgumentException e) {
            MuseBot.LOGGER.error("Unable to register all Guild commands. Registered too many");
        }
    }
}
