package io.github.profjb58.musebot;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.profjb58.musebot.commands.CommandManager;
import io.github.profjb58.musebot.config.ConfigHandler;
import io.github.profjb58.musebot.listeners.EventListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

public class MuseBot {
    public static final long MODERATOR_ROLE_ID = 774931085992919092L;
    public static final Logger LOGGER = LogManager.getLogger(MuseBot.class);

    private final CommandManager commandManager;
    private final Dotenv envConfig;
    private final ConfigHandler configHandler;
    private final Contest currentContest;

    public MuseBot() throws LoginException {
        envConfig = Dotenv.configure().load();
        configHandler = new ConfigHandler();

        JDA jda = JDABuilder.createDefault(envConfig.get("BOT_TOKEN"))
                .setStatus(OnlineStatus.ONLINE)
                .setMemberCachePolicy(MemberCachePolicy.ONLINE) // Save online members into the cache
                .enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS, CacheFlag.ROLE_TAGS, CacheFlag.GUILD_SCHEDULED_EVENTS) // Cache whenever the online status changes
                .build();

        commandManager = new CommandManager(this);
        currentContest = new Contest(configHandler);

        // Register listeners
        jda.addEventListener(new EventListener(this));
        jda.addEventListener(commandManager);
    }

    public TextChannel getBotChannel(@Nonnull JDA jda) {
        return jda.getTextChannelById(envConfig.get("BOT_CHANNEL_ID"));
    }

    public Dotenv getEnvConfig() {
        return envConfig;
    }

    public ConfigHandler getConfigHandler() { return configHandler; }

    public CommandManager getCommandManager() { return commandManager; }

    public Contest getCurrentContest() { return currentContest; }

    public static void main(String[] args) {
        try {
            new MuseBot();
        } catch (LoginException le) {
            LOGGER.error("Failed to login. Bot Token failed...");
        }
    }
}
