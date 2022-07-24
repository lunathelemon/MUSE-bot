package io.github.profjb58.musebot.commands;

import io.github.profjb58.musebot.Contest;
import io.github.profjb58.musebot.MuseBot;
import io.github.profjb58.musebot.commands.Command;
import io.github.profjb58.musebot.exceptions.SingleInstanceException;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import javax.annotation.Nonnull;
import java.util.List;

public class ContestCommand extends Command {
    private final MuseBot instance;

    public ContestCommand(MuseBot instance) {
        this.instance = instance;
        this.isEphemeral = true;
        this.answerFast = false;
    }

    @Override
    protected void onCommand(@Nonnull SlashCommandInteractionEvent event) {
        String subCommandName = event.getSubcommandName();

        if(subCommandName != null && event.getGuild() != null) {
            switch(subCommandName) {
                case "create" -> onCreateContest(event);
                case "finish" -> onFinishContest(event);
                case "cancel" -> onCancelContest(event);
                case "next_stage" -> onNextStage(event);
            }
        }
    }

    private void onCreateContest(@Nonnull SlashCommandInteractionEvent event) {
        String theme = event.getOption("theme", null, OptionMapping::getAsString);
        String conditions = event.getOption("conditions", null, OptionMapping::getAsString);
        User creator = event.getOption("creator", event.getUser(), OptionMapping::getAsUser);

        try {
            int submissionDuration = event.getOption("submission", Contest.SUBMISSION_PERIOD_DEFAULT, OptionMapping::getAsInt);
            int votingDuration = event.getOption("voting", Contest.VOTING_PERIOD_DEFAULT, OptionMapping::getAsInt);

            // Check for valid inputs
            if (theme != null && conditions != null && submissionDuration > 0 && votingDuration > 0) {
                var contest = instance.getCurrentContest();
                boolean success = contest.create(submissionDuration, votingDuration, event.getGuild()); // Create a new contest

                if(success) {
                    var channel = event.getGuild().getChannelById(TextChannel.class, Contest.CONTEST_CHANNEL_ID);
                    if (channel != null) {
                        // Print contest embed message to the correct contest channel
                        channel.sendMessageEmbeds(getStartContestEmbed(theme, conditions, creator)).queue();
                        event.getHook().sendMessage( // Send command response
                                "**Successfully created a new contest!:** \n\n" +
                                        ">>> ► **Submission period:** " + submissionDuration + " days\n" +
                                        "► **Voting period:** " + votingDuration + " days"
                        ).queue();
                    } else event.getHook().sendMessage("⚠️ Contest channel does not exist or is mapped in-correctly").queue();
                } else event.getHook().sendMessage("⚠️ A contest is already active").queue();
            } else event.getHook().sendMessage("⚠️ Failed to create a new contest. Please specify the contests theme and conditions").queue();
        } catch (ArithmeticException e) {
            event.getHook().sendMessage("⚠️ The value you entered for the submission or voting period is too high").queue();
        }
    }

    private void onFinishContest(@Nonnull SlashCommandInteractionEvent event) {
        // TODO - print contest
        onCancelContest(event);
    }

    private void onCancelContest(@Nonnull SlashCommandInteractionEvent event) {
        var contest = instance.getCurrentContest();
        boolean success = contest.cancel(event.getGuild());
        if(success)
            event.getHook().sendMessage("Finished the current contest").queue();
        else
            event.getHook().sendMessage("⚠️ No contest is currently active").queue();
    }

    private void onNextStage(@Nonnull SlashCommandInteractionEvent event) {
        try {
            Contest.Stage nextStage = instance.getCurrentContest().nextStage(event.getGuild());
            if(nextStage != null) {
                switch(nextStage) {
                    case SUBMISSION_PERIOD -> event.getHook().sendMessage("**🗳 Proceeded to the voting stage**").queue();
                    case VOTING_PERIOD -> event.getHook().sendMessage("**🏁 Finished the current contest**").queue();
                }
            } else event.getHook().sendMessage("⚠️ No contest is currently active").queue();
        } catch (SingleInstanceException e) {
            instance.getBotChannel(event.getJDA()).sendMessage("⚠️ <@&" + MuseBot.MODERATOR_ROLE_ID + "> " +
                    "Image Contest cannot continue to the next stage as multiple submission / voting events exist").queue();
            e.printStackTrace();
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("contest", "Image contest command")
                .addSubcommands(
                        new SubcommandData("create", "Create a new Image Contest")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "theme", "The Contests theme"),
                                        new OptionData(OptionType.STRING, "conditions", "The conditions for the Contest"),
                                        new OptionData(OptionType.USER, "creator", "User who set the theme and conditions for the contest"),
                                        new OptionData(OptionType.INTEGER, "submission", "Length of the submission period in days"),
                                        new OptionData(OptionType.INTEGER, "voting", "Length of the voting period in days")
                                ),
                        new SubcommandData("finish", "Finish the current contest and print the results"),
                        new SubcommandData("cancel", "Cancel the current contest without printing the results"),
                        new SubcommandData("next_stage", "Proceed to the next stage of the contest")
                );
    }

    private MessageEmbed getStartContestEmbed(String theme, String conditions, User creator) {
        return new MessageEmbed(
                null, "Image Hunt Contest",
                "**Dear " + "<@&" + Contest.CONTESTANT_ROLE_ID + ">" + " and non-contestants!\n\n**" +
                        "The event for the next Voting Period has been set up - please 'join' it if you want to receive notifications when it begins.\n\n" +
                        "If you want to participate but are unsure about the rules, feel free to check the pinned posts for a summary!\n\n" +
                        "And here are the details for the next Image Hunt, courtesy of " + creator.getAsMention(),
                EmbedType.RICH, null, 0x34db37, null, null, null, null, null, null,
                List.of(
                        new MessageEmbed.Field("Theme", theme, true),
                        new MessageEmbed.Field("Conditions", conditions, true)
                )
        );
    }
}
