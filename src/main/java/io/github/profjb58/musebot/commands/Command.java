package io.github.profjb58.musebot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;

public abstract class Command {

    protected boolean isEphemeral = false;
    protected boolean expectPrivateResponse = false;
    protected boolean answerFast = true;

    protected abstract void onCommand(@Nonnull SlashCommandInteractionEvent interaction);

    public abstract CommandData getCommandData();
}
