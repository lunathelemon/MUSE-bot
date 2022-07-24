package io.github.profjb58.musebot;

import io.github.profjb58.musebot.config.ConfigFile;
import io.github.profjb58.musebot.config.ConfigHandler;
import io.github.profjb58.musebot.exceptions.SingleInstanceException;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Contest {

    public static final long CONTEST_CHANNEL_ID = 999695077268013147L;
    public static final long CONTESTANT_ROLE_ID = 888698866696871937L;
    private static final int SUBMISSION_DELAY_DEFAULT = 1;
    public static final int SUBMISSION_PERIOD_DEFAULT = 3;
    public static final int VOTING_PERIOD_DEFAULT = 3;

    private static final int MAX_SUBMISSIONS = 100;
    private static final Emoji VOTE_EMOJI = Emoji.fromUnicode("U+2764"); // Currently set to a Red Heart
    private static final String SUBMISSION_PERIOD_NAME = "Image Hunt Submission Period";
    private static final String VOTING_PERIOD_NAME = "Image Hunt Voting Period";

    private final ConfigHandler configHandler;
    private final SortedSet<Score> results;

    private long votingStageLength;
    private int submissionsCounter;

    public enum Stage {
        SUBMISSION_PERIOD,
        VOTING_PERIOD
    }
    private record Score(User user, int score) {}

    Contest(ConfigHandler configHandler) {
        this.configHandler = configHandler;
        this.results = new TreeSet<>(Comparator.comparingInt(s -> s.score));
    }

    public boolean create(int submitStageLength, int votingStageLength, Guild guild) {
        try {
            if(!isActive(guild)) {
                this.votingStageLength = votingStageLength;
                this.submissionsCounter = 0;
                long submitStageEndDate = Instant.now().toEpochMilli() + TimeUnit.MILLISECONDS.convert(submitStageLength, TimeUnit.DAYS);

                guild.createScheduledEvent()
                        .setStartTime(Instant.now().plusSeconds(SUBMISSION_DELAY_DEFAULT * 86400))
                        .setEndTime(Instant.ofEpochMilli(submitStageEndDate))
                        .setName(SUBMISSION_PERIOD_NAME)
                        .setLocation("#contests")
                        .queue();
                save();
                return true;
            }
        } catch (SingleInstanceException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean cancel(Guild guild) {
        try {
            if(isActive(guild)) {
                getAllScheduledEvents(guild).forEach(event -> event.delete().queue());
                votingStageLength = 0;
                submissionsCounter = 0;
                save();
                return true;
            }
        } catch (SingleInstanceException ignored) {}
        return false;
    }

    public boolean finish(Guild guild) {
        var contestChannel = guild.getChannelById(TextChannel.class, CONTEST_CHANNEL_ID);
        if(contestChannel != null) {
            results.clear();

            contestChannel.getHistory().retrievePast(MAX_SUBMISSIONS)
                    .queue(messages -> {
                        messages.forEach(message -> {
                            if(message.getAuthor().isBot()) return; // We found the MUSE bots last message, exit early
                            var messageReaction = message.getReaction(VOTE_EMOJI);
                            var sender = message.getMember();
                            if(messageReaction != null && sender != null)
                                results.add(new Score(sender.getUser(), messageReaction.getCount()));
                        });
                        // Post the results
                        var resultsChannel = guild.getChannelById(TextChannel.class, CONTEST_CHANNEL_ID);
                        if(resultsChannel != null)
                            resultsChannel.sendMessage(getResultsMessage()).queue();
                    });
        }
        return cancel(guild);
    }

    @Nullable
    public Stage nextStage(Guild guild) throws SingleInstanceException {
        var scheduledEvent = getScheduledEvent(guild);
        if(scheduledEvent != null) {
            var currentStage = getCurrentStage(scheduledEvent);
            long epochInstant = Instant.now().toEpochMilli();

            if(currentStage != null) {
                if(currentStage == Stage.SUBMISSION_PERIOD) {
                    // Delete submission period event if it exists and then create a voting period event
                    scheduledEvent.delete().submit().whenComplete((v, error) -> {
                        long votingStageEndDate = epochInstant + TimeUnit.MILLISECONDS.convert(votingStageLength, TimeUnit.DAYS);
                        guild.createScheduledEvent()
                                .setStartTime(Instant.now().plusSeconds(10))
                                .setEndTime(Instant.ofEpochMilli(votingStageEndDate))
                                .setName(VOTING_PERIOD_NAME)
                                .setLocation("#contests")
                                .queue();
                        if(error != null) error.printStackTrace();
                    });
                } else if(currentStage == Stage.VOTING_PERIOD){
                    finish(guild); // Finish the contest
                }
                return currentStage;
            }
        }
        return null;
    }

    private void save() {
        var dataStorage = getDataStorage();
        dataStorage.setLongProperty("voting_stage_length", votingStageLength);
        dataStorage.update();
    }

    public void load() {
        getDataStorage().getLongProperty("voting_stage_length", true);
    }

    private ConfigFile getDataStorage() {
        return configHandler.getDateStorage(ConfigHandler.DataType.CONTESTS);
    }

    private List<GuildScheduledEvent> getAllScheduledEvents(Guild guild) {
        List<GuildScheduledEvent> scheduledEvents = new ArrayList<>();
        scheduledEvents.addAll(guild.getScheduledEventsbyName(SUBMISSION_PERIOD_NAME, false));
        scheduledEvents.addAll(guild.getScheduledEventsbyName(VOTING_PERIOD_NAME, false));
        return scheduledEvents;
    }

    private String getResultsMessage() {
        if(results.size() >= 3) {
            StringBuilder resultsMessage = new StringBuilder("**Dear " + "<@&" + Contest.CONTESTANT_ROLE_ID + ">" + " and non-contestants!\n\n**" +
                    "The voting period is over - here are the results of the latest **Image Hunt!**\n\n >>>");

            String[] medalUnicodeValues = new String[]{"U+1F947", "U+1F948", "U+1F949"};
            Score firstPlaceWinner = results.first();
            for(int i=0; i<3; i++) { // Print the medal results
                var winner = results.iterator().next();
                resultsMessage.append(Emoji.fromUnicode(medalUnicodeValues[i])).append(" - ").append(winner.user.getAsMention())
                        .append(" (").append(winner.score).append(")\n\n");
            }
            resultsMessage.append("__Congratulations ").append(firstPlaceWinner.user.getAsMention()).append("!__\n")
                    .append("Please reach out to a <@&" + MuseBot.MODERATOR_ROLE_ID + "> " + "should you want to decide the specifications of " +
                            "the next Image Hunt or change the color of your role/username.\n\n")
                    .append("**Thank you all very much for your participation!**");
            return resultsMessage.toString();
        } else {
            return "**The Image Hunt has finished!**. There are not enough submissions available to produce rankings";
        }
    }

    public GuildScheduledEvent getScheduledEvent(Guild guild) throws SingleInstanceException {
        List<GuildScheduledEvent> scheduledEvents = getAllScheduledEvents(guild);
        if(scheduledEvents.size() == 1) {
            return scheduledEvents.get(0);
        } else if(scheduledEvents.size() > 1) {
            throw new SingleInstanceException("Multiple Submission and Voting period events cannot exist at the same time");
        }
        return null;
    }

    @Nullable
    public Stage getCurrentStage(GuildScheduledEvent scheduledEvent) {
        if(scheduledEvent != null) {
            switch (scheduledEvent.getName()) {
                case SUBMISSION_PERIOD_NAME -> {
                    return Stage.SUBMISSION_PERIOD;
                }
                case VOTING_PERIOD_NAME -> {
                    return Stage.VOTING_PERIOD;
                }
            }
        }
        return null;
    }

    public boolean isActive(Guild guild) throws SingleInstanceException {
        return getScheduledEvent(guild) != null;
    }
}
