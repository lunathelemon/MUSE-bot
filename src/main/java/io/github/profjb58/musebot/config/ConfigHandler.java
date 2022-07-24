package io.github.profjb58.musebot.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.crypto.Data;
import java.util.EnumMap;
import java.util.Properties;

public class ConfigHandler {

    // Config directory
    static final String MAIN_DIRECTORY = System.getProperty("user.dir") + "/resources";
    private static ConfigFile settingsConfig;
    private static final EnumMap<DataType, ConfigFile> dataStorage = new EnumMap<>(DataType.class);

    public enum DataType {
        SETTINGS,
        CONTESTS
    }

    public ConfigHandler() {
        Properties defaults = new Properties();
        defaults.setProperty("debug_mode", "true");
        settingsConfig = new ConfigFile("settings", defaults, "MUSE Settings", MAIN_DIRECTORY);

        defaults = new Properties();
        defaults.setProperty("voting_stage_length", "0");
        dataStorage.put(DataType.CONTESTS, new ConfigFile("contests", defaults,
                "MUSE Image Contest data storage", MAIN_DIRECTORY + "/data"));
    }

    public ConfigFile getDateStorage(DataType dataType) {
        return dataStorage.getOrDefault(dataType, null);
    }

    public ConfigFile getSettingsConfig() {
       return settingsConfig;
    }
}
