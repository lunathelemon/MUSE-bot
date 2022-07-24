package io.github.profjb58.musebot.commands;

import io.github.profjb58.musebot.MuseBot;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PurgeCommand extends Command {

    private static final long HAS_READ_RULES_ROLE_ID = 788294226705514497L;

    public PurgeCommand() {
        this.answerFast = false;
        this.isEphemeral = true;
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteractionEvent interaction) {
        List<Member> purgedMembers = new ArrayList<>();
        String purgedMembersMessage = "**Purged the following members**\n\n";

        interaction.getGuild().getMembers().forEach(member -> {
            var epochInstant = Time.from(Instant.now()).getTime() / 1000;
            var epochTimeJoined = member.getTimeJoined().toEpochSecond();
            var roles = member.getRoles();

            boolean hasReadRules = false;
            for(var role : roles) {
                if(role.getIdLong() == HAS_READ_RULES_ROLE_ID) // Player has the "Read Rules" role
                    hasReadRules = true;
            }
            // Purge players who have not read the rules and joined more than a week ago
            if(!hasReadRules && (epochInstant - epochTimeJoined) > 604800) {
                //member.kick("You have not accepting the rules within a week of joining").queue();
                purgedMembers.add(member);
            }
        });
        for(var purgedMember : purgedMembers)
            purgedMembersMessage = purgedMembersMessage + purgedMember.getAsMention() + "\n";
        //interaction.getHook().sendMessage(purgedMembersMessage).queue();

        interaction.getHook().sendMessage("**Successfully purged " + purgedMembers.size() + " members!**").queue();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("purge", "Purge players who have not accepted the rules within a week");
    }
}
