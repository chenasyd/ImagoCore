package org.a.imagoCore.config;

import org.a.imagoCore.ImagoCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages GUI registrations from the {@code plugins/ImagoCore/gui/}
 * directory.
 *
 * <p>Each sub-directory represents a slot size (e.g. {@code 54/}).
 * Inside each folder, a {@code gui.yml} defines one or more entries,
 * each referencing a different texture PNG:
 *
 * <pre>{@code
 * slots: 54
 * defaults:
 *   ascent: 13
 *   height: 222
 *   shift_x: -8
 * entries:
 *   default:
 *     texture: "default.png"
 *   premium:
 *     texture: "premium.png"
 * }</pre>
 *
 * <p>The master {@code gui/gui.yml} tracks Unicode char assignments:
 * <pre>{@code
 * registrations:
 *   "54-default": "\uE800"
 *   "54-premium": "\uE803"
 * }</pre>
 */
public class GuiRegistry {

    private static final String CHAR_POOL_START = "\uE820";
    private static final int CHAR_START = CHAR_POOL_START.codePointAt(0);

    private final ImagoCore plugin;
    private final File guiDir;
    private final File masterFile;

    private String shiftCharCoarse = "\uE801";
    private String shiftCharFine = "\uE802";
    private int defaultAscent = 13;
    private int defaultHeight = 222;
    private int defaultShiftX = -8;

    private final List<GuiEntry> entries = new ArrayList<>();

    public GuiRegistry(ImagoCore plugin) {
        this.plugin = plugin;
        this.guiDir = new File(plugin.getDataFolder(), "gui");
        this.masterFile = new File(guiDir, "gui.yml");
    }

    /**
     * Loads (or reloads) all GUI entries.
     * Auto-assigns Unicode chars to new unregistered entries.
     */
    public void load() {
        entries.clear();
        if (!guiDir.exists()) return;

        YamlConfiguration master = YamlConfiguration.loadConfiguration(masterFile);
        shiftCharCoarse = master.getString("shift_char_coarse", "\uE801");
        shiftCharFine = master.getString("shift_char_fine", "\uE802");

        ConfigurationSection defSec = master.getConfigurationSection("defaults");
        if (defSec != null) {
            defaultAscent = defSec.getInt("ascent", 13);
            defaultHeight = defSec.getInt("height", 222);
            defaultShiftX = defSec.getInt("shift_x", -8);
        }

        // Load existing registrations:  id -> char
        ConfigurationSection regSec = master.getConfigurationSection("registrations");
        Map<String, String> registrations = new LinkedHashMap<>();
        if (regSec != null) {
            for (String key : regSec.getKeys(false)) {
                registrations.put(key, regSec.getString(key));
            }
        }

        // Pre-reserve shift chars so auto-assignment skips them
        registrations.put("__builtin_shift_coarse__", shiftCharCoarse);
        registrations.put("__builtin_shift_fine__", shiftCharFine);

        // Scan slot-size folders (sorted for determinism)
        File[] dirs = guiDir.listFiles(File::isDirectory);
        if (dirs == null) return;
        Arrays.sort(dirs, Comparator.comparing(File::getName));

        int nextCharIdx = 0;
        boolean modified = false;

        for (File folder : dirs) {
            String folderId = folder.getName();
            File infoFile = new File(folder, "gui.yml");
            if (!infoFile.exists()) continue;
            if (folderId.startsWith(".")) continue;

            YamlConfiguration folderCfg = YamlConfiguration.loadConfiguration(infoFile);
            int folderSlots = folderCfg.getInt("slots", 54);

            ConfigurationSection entrySec = folderCfg.getConfigurationSection("entries");
            if (entrySec == null) continue;

            // Per-entry defaults (optional overrides)
            int entryDefAscent = folderCfg.getInt("defaults.ascent", defaultAscent);
            int entryDefHeight = folderCfg.getInt("defaults.height", defaultHeight);
            int entryDefShiftX = folderCfg.getInt("defaults.shift_x", defaultShiftX);

            for (String entryName : entrySec.getKeys(false)) {
                ConfigurationSection entryCfg = entrySec.getConfigurationSection(entryName);
                if (entryCfg == null) continue;

                String textureName = entryCfg.getString("texture");
                if (textureName == null || textureName.isEmpty()) continue;

                File texFile = new File(folder, textureName);
                if (!texFile.exists()) continue;

                int slots = entryCfg.getInt("slots", folderSlots);
                int ascent = entryCfg.getInt("ascent", entryDefAscent);
                int height = entryCfg.getInt("height", entryDefHeight);
                int shiftX = entryCfg.getInt("shift_x", entryDefShiftX);

                // Unique entry ID: folderId-entryName
                String entryId = folderId + "-" + entryName;

                // Assign or retrieve Unicode char
                String assigned = registrations.get(entryId);
                if (assigned == null || assigned.isEmpty()) {
                    while (registrations.containsValue(charAt(nextCharIdx))) {
                        nextCharIdx++;
                    }
                    assigned = charAt(nextCharIdx);
                    registrations.put(entryId, assigned);
                    modified = true;
                    nextCharIdx++;
                }

                // Read PNG dimensions for rendered-width calculation
                int texWidth = 176;  // fallback: standard MC GUI width
                int texHeight = height;
                try {
                    BufferedImage img = ImageIO.read(texFile);
                    if (img != null) {
                        texWidth = img.getWidth();
                        texHeight = img.getHeight();
                    }
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.FINE,
                            "Could not read PNG dimensions for " + texFile, ex);
                }

                GuiEntry entry = new GuiEntry(folderId, entryName, slots,
                        ascent, height, shiftX, assigned, texFile,
                        texWidth, texHeight);
                entries.add(entry);
            }
        }

        // Persist new assignments (skip __builtin_ sentinels)
        if (modified) {
            for (Map.Entry<String, String> e : registrations.entrySet()) {
                if (e.getKey().startsWith("__builtin_")) continue;
                master.set("registrations." + e.getKey(), e.getValue());
            }
            try {
                master.save(masterFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to save GUI registry", ex);
            }
        }

        plugin.getLogger().info("Loaded " + entries.size() + " GUI registrations.");
    }

    /** All registered entries. */
    public List<GuiEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Lookup by full ID (e.g. "54-default"). */
    public GuiEntry getEntry(String id) {
        for (GuiEntry e : entries) {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }

    public String getShiftCharCoarse() { return shiftCharCoarse; }
    public String getShiftCharFine() { return shiftCharFine; }

    // ── Persistence / auto-rebuild support ───────────────────────

    /**
     * Returns true if the current entry set differs from the last
     * built state (new entries added, chars reassigned, etc.).
     */
    public boolean isStale() {
        String current = computeFingerprint();
        String stored = YamlConfiguration.loadConfiguration(masterFile)
                .getString("last_built_fingerprint", "");
        return !current.equals(stored);
    }

    /**
     * Saves the current fingerprint so that future starts know the
     * resource pack is up-to-date.
     */
    public void markBuilt() {
        YamlConfiguration master = YamlConfiguration.loadConfiguration(masterFile);
        master.set("last_built_fingerprint", computeFingerprint());
        try {
            master.save(masterFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to persist build fingerprint", ex);
        }
    }

    /**
     * Builds a short hash over all entry IDs, chars, texture paths
     * and shift chars, so any change invalidates the fingerprint.
     */
    private String computeFingerprint() {
        String raw = entries.stream()
                .sorted(Comparator.comparing(GuiEntry::getId))
                .map(e -> e.getId() + "|" + e.getBackgroundChar()
                        + "|" + e.getTexturePackPath() + "|" + e.getSlots())
                .collect(Collectors.joining("\n"));
        raw += "\nshift_c:" + shiftCharCoarse + "\nshift_f:" + shiftCharFine;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16); // first 16 hex chars is enough
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private static String charAt(int idx) {
        return new String(Character.toChars(CHAR_START + idx));
    }
}
