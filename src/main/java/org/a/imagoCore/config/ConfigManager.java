package org.a.imagoCore.config;

import org.a.imagoCore.ImagoCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * Manages plugin configuration files with hot-reload support.
 *
 * <p>Main config is accessed via {@link #getMainConfig()} and supports
 * {@code /guildadmin reload}.  Bukkit's plain {@code getConfig()} is
 * reserved for bootstrap-only values and does NOT support hot-reload.
 */
public class ConfigManager {

    private final ImagoCore plugin;
    private MainConfig mainConfig;

    public ConfigManager(ImagoCore plugin) {
        this.plugin = plugin;
    }

    /** Load (or reload) all configuration files. */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.mainConfig = new MainConfig(plugin.getConfig());
        plugin.getLogger().info("Configuration loaded.");
    }

    /**
     * Returns the typed main config.
     * Values reflect the latest {@code /guildadmin reload} call.
     */
    public MainConfig getMainConfig() {
        return mainConfig;
    }
}
