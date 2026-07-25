package org.a.imagoCore;

import org.a.imagoCore.command.ImagoCoreCommand;
import org.a.imagoCore.command.resource.ResourceCommand;
import org.a.imagoCore.command.test.GuiTestHandler;
import org.a.imagoCore.command.test.TestCommand;
import org.a.imagoCore.config.CharRegistry;
import org.a.imagoCore.config.ConfigManager;
import org.a.imagoCore.config.GuiRegistry;
import org.a.imagoCore.gui.GuiController;
import org.a.imagoCore.resource.pack.ResourcePackGenerator;
import org.a.imagoCore.scheduler.CompatibleScheduler;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.logging.Level;

public final class ImagoCore extends JavaPlugin {

    private ConfigManager configManager;
    private CompatibleScheduler scheduler;
    private GuiRegistry guiRegistry;
    private CharRegistry charRegistry;
    private ImagoCoreCommand mainCommand;

    // GUI controller
    private GuiController guiController;

    // ── Lifecycle ────────────────────────────────────────────────

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        // 1. Infrastructure (non-hot-reloadable bootstrap)
        this.scheduler = new CompatibleScheduler();

        // 2. Data directories + GUI template
        initDataDirectories();

        // 3. Config (hot-reloadable)
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // 4. GUI registry (scans gui/ sub-directories)
        this.guiRegistry = new GuiRegistry(this);
        this.guiRegistry.load();

        // 4b. Char registry (scans char/ directory)
        this.charRegistry = new CharRegistry(this);
        this.charRegistry.load(
                configManager.getMainConfig().getCharDefaultAscent(),
                configManager.getMainConfig().getCharDefaultHeight()
        );

        // 4c. Auto-rebuild resource pack if registrations changed
        boolean guiStale = guiRegistry.isStale();
        boolean charStale = charRegistry.isStale();
        if ((guiStale || charStale)
                && (!guiRegistry.getEntries().isEmpty() || !charRegistry.getEntries().isEmpty())) {
            String output = configManager.getMainConfig().getResourcePackOutput();
            File outFile = resolveOutputFile(output);
            try {
                new ResourcePackGenerator(this, outFile, guiRegistry, charRegistry).build();
                guiRegistry.markBuilt();
                charRegistry.markBuilt();
                getLogger().info("Auto-rebuilt resource pack (registrations changed).");
            } catch (IOException e) {
                getLogger().log(Level.WARNING,
                        "Failed to auto-rebuild resource pack", e);
            }
        }

        // 4d. Init GUI controller
        this.guiController = new GuiController(guiRegistry, getLogger());
        guiController.initialize();

        // 5. Register commands
        this.mainCommand = buildCommandTree();
        PluginCommand cmd = getCommand("imagocore");
        Objects.requireNonNull(cmd, "Command 'imagocore' not defined in plugin.yml");
        cmd.setExecutor(mainCommand);
        cmd.setTabCompleter(mainCommand);

        // 6. Register events
        registerListeners();

        getLogger().info(() -> String.format(
                "ImagoCore enabled in %d ms (Folia: %s).",
                System.currentTimeMillis() - start, scheduler.isFolia()));
    }

    @Override
    public void onDisable() {
        if (guiController != null) {
            guiController.shutdown();
        }
        scheduler.cancelAll(this);
        getLogger().info("ImagoCore disabled.");
    }

    // ── Directory initialisation ─────────────────────────────────

    private static final int[] BUILTIN_SLOTS = {9, 18, 27, 36, 45, 54};

    private void initDataDirectories() {
        mkdirs("players", "models", "armors", "gui", "char");

        File guiDir = new File(getDataFolder(), "gui");
        initMasterGuiConfig(guiDir);

        // Create a slot-size folder + template for each built-in size
        for (int slots : BUILTIN_SLOTS) {
            initSlotFolder(guiDir, slots);
        }
    }

    private void initMasterGuiConfig(File guiDir) {
        File masterFile = new File(guiDir, "gui.yml");
        if (masterFile.exists()) return;

        YamlConfiguration master = new YamlConfiguration();
        master.set("shift_char_coarse", "\uE801");
        master.set("shift_char_fine", "\uE802");
        master.set("defaults.ascent", 13);
        master.set("defaults.height", 222);
        master.set("defaults.shift_x", -8);
        master.set("registrations", null);
        try {
            master.save(masterFile);
            getLogger().info("Created gui/gui.yml");
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to save gui/gui.yml", e);
        }
    }

    private void initSlotFolder(File guiDir, int slots) {
        String folderName = String.valueOf(slots);
        File folder = new File(guiDir, folderName);

        // gui.yml
        File infoFile = new File(folder, "gui.yml");
        if (!infoFile.exists()) {
            folder.mkdirs();
            YamlConfiguration info = new YamlConfiguration();
            info.set("slots", slots);
            info.set("defaults.ascent", 13);
            info.set("defaults.height", 222);
            info.set("defaults.shift_x", -8);
            info.set("entries.default.texture", "default.png");
            try {
                info.save(infoFile);
                getLogger().info("Created gui/" + folderName + "/gui.yml");
            } catch (IOException e) {
                getLogger().log(Level.WARNING,
                        "Failed to save gui/" + folderName + "/gui.yml", e);
            }
        }

        // default.png placeholder (copy from 54 template)
        File bgFile = new File(folder, "default.png");
        if (!bgFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream(
                    "/resource-pack-template/assets/minecraft/textures/textures/gui/custom_gui_54.png")) {
                if (in != null) {
                    Files.copy(in, bgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    getLogger().info("Copied placeholder to gui/" + folderName + "/default.png");
                }
            } catch (IOException e) {
                getLogger().log(Level.WARNING,
                        "Failed to copy default background for " + folderName, e);
            }
        }
    }

    private void mkdirs(String... names) {
        for (String name : names) {
            File dir = new File(getDataFolder(), name);
            if (!dir.exists()) {
                dir.mkdirs();
                getLogger().info("Created directory: " + name);
            }
        }
    }

    // ── Command tree ─────────────────────────────────────────────

    private ImagoCoreCommand buildCommandTree() {
        ImagoCoreCommand root = new ImagoCoreCommand(this);

        // /imagocore test
        TestCommand testCmd = new TestCommand(this);
        testCmd.register(new GuiTestHandler(this));
        root.register(testCmd);

        // /imagocore resource
        root.register(new ResourceCommand(this));

        return root;
    }

    private void registerListeners() {
        // TODO: register Bukkit event listeners
    }

    /**
     * Resolves a configurable output path to an absolute file.
     * Paths starting with "plugins/" are relative to the server root.
     */
    private File resolveOutputFile(String path) {
        File f = new File(path);
        if (f.isAbsolute()) return f;
        if (path.startsWith("plugins/") || path.startsWith("plugins\\")) {
            return new File(getDataFolder().getParentFile().getParentFile(), path);
        }
        return new File(getDataFolder(), path);
    }

    // ── Service accessors ────────────────────────────────────────

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CompatibleScheduler getScheduler() {
        return scheduler;
    }

    public GuiRegistry getGuiRegistry() {
        return guiRegistry;
    }

    public CharRegistry getCharRegistry() {
        return charRegistry;
    }

    public GuiController getGuiController() {
        return guiController;
    }
}
