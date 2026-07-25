package org.a.imagoCore.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Typed access to the main plugin configuration.
 * Values are re-read from the ConfigurationSection on each get call
 * to support hot-reload without restart.
 */
public class MainConfig {

    private final ConfigurationSection section;
    private GuiTitleConfig guiTitleConfig;

    public MainConfig(ConfigurationSection section) {
        this.section = section;
        reload(section);
    }

    /**
     * Reloads the underlying configuration section.
     * Called by ConfigManager on /guildadmin reload.
     */
    public void reload(ConfigurationSection section) {
        this.guiTitleConfig = new GuiTitleConfig(
                section.getConfigurationSection("gui.title")
        );
    }

    // ──────────────────────────────────────────────
    //  Getters — expand as needed
    // ──────────────────────────────────────────────

    public boolean isVerboseLogging() {
        return section.getBoolean("verbose-logging", false);
    }

    public int getMaxAnimations() {
        return section.getInt("max-animations", 50);
    }

    public int getRenderTickInterval() {
        return section.getInt("render-tick-interval", 1);
    }

    /** GUI title image configuration (resource-pack-backed). */
    public GuiTitleConfig getGuiTitleConfig() {
        return guiTitleConfig;
    }

    /** Default output path for the generated resource pack zip. */
    public String getResourcePackOutput() {
        return section.getString("resource-pack.output", "plugins/ImagoCore/build.zip");
    }

    /** Default ascent for character images. */
    public int getCharDefaultAscent() {
        return section.getInt("char.defaults.ascent", 8);
    }

    /** Default height for character images. */
    public int getCharDefaultHeight() {
        return section.getInt("char.defaults.height", 8);
    }
}
