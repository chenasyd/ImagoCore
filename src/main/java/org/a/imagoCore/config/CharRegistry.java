package org.a.imagoCore.config;

import org.a.imagoCore.ImagoCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages character-image registrations from the
 * {@code plugins/ImagoCore/char/} directory.
 *
 * <p>PNG images placed directly in the {@code char/} folder are
 * auto-discovered.  Each image receives a Unicode private-use
 * character ({@code \uE900}+) and a bitmap font provider.
 *
 * <p>Assignments are persisted in {@code char/char.yml}:
 * <pre>{@code
 * defaults:
 *   ascent: 8
 *   height: 8
 * registrations:
 *   my_icon: "\uE900"
 *   logo:    "\uE901"
 * }</pre>
 */
public class CharRegistry {

    /** Start of the char image Unicode pool (U+E900).
     *  GUI entries use U+E800+, so there is no overlap. */
    private static final String CHAR_POOL_START = "\uE900";
    private static final int CHAR_START = CHAR_POOL_START.codePointAt(0);

    /** Start of the variant Unicode pool (U+EA00).
     *  Variants are the same image with a different ascent. */
    private static final String VARIANT_POOL_START = "\uEA00";
    private static final int VARIANT_START = VARIANT_POOL_START.codePointAt(0);

    private final ImagoCore plugin;
    private final File charDir;
    private final File masterFile;

    private int defaultAscent = 8;
    private int defaultHeight = 8;

    private final List<CharEntry> entries = new ArrayList<>();
    /** Variants: key = "baseName:ascent", value = CharEntry with custom ascent. */
    private final Map<String, CharEntry> variants = new LinkedHashMap<>();

    public CharRegistry(ImagoCore plugin) {
        this.plugin = plugin;
        this.charDir = new File(plugin.getDataFolder(), "char");
        this.masterFile = new File(charDir, "char.yml");
    }

    /**
     * Loads (or reloads) all character-image entries.
     * Auto-assigns Unicode chars to new images.
     *
     * @param defaultAscent fallback ascent if not in char.yml
     * @param defaultHeight fallback height if not in char.yml
     */
    public void load(int defaultAscent, int defaultHeight) {
        entries.clear();
        this.defaultAscent = defaultAscent;
        this.defaultHeight = defaultHeight;

        if (!charDir.exists()) {
            charDir.mkdirs();
        }

        YamlConfiguration master = masterFile.exists()
                ? YamlConfiguration.loadConfiguration(masterFile)
                : new YamlConfiguration();

        // Read persisted defaults
        this.defaultAscent = master.getInt("defaults.ascent", defaultAscent);
        this.defaultHeight = master.getInt("defaults.height", defaultHeight);

        // Load existing registrations:  name -> char
        Map<String, String> registrations = new LinkedHashMap<>();
        ConfigurationSection regSec = master.getConfigurationSection("registrations");
        if (regSec != null) {
            for (String key : regSec.getKeys(false)) {
                registrations.put(key, regSec.getString(key));
            }
        }

        // Scan PNG files (sorted for determinism)
        File[] pngFiles = charDir.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".png")
                        && !name.startsWith("."));
        if (pngFiles == null) return;

        Arrays.sort(pngFiles, Comparator.comparing(File::getName));

        Set<String> usedChars = new HashSet<>(registrations.values());
        int nextCharIdx = 0;
        boolean modified = false;

        for (File png : pngFiles) {
            String filename = png.getName();
            // Strip ".png" extension for the entry name
            String entryName = filename.substring(0, filename.length() - 4);

            // Per-entry override (optional)
            int ascent = master.getInt("entries." + entryName + ".ascent", this.defaultAscent);
            int height = master.getInt("entries." + entryName + ".height", this.defaultHeight);

            // Assign or retrieve Unicode char
            String assigned = registrations.get(entryName);
            if (assigned == null || assigned.isEmpty()) {
                while (usedChars.contains(charAt(nextCharIdx))) {
                    nextCharIdx++;
                }
                assigned = charAt(nextCharIdx);
                registrations.put(entryName, assigned);
                usedChars.add(assigned);
                modified = true;
                nextCharIdx++;
            }

            CharEntry entry = new CharEntry(entryName, assigned, png, ascent, height);
            entries.add(entry);
        }

        // Persist new assignments
        if (modified) {
            master.set("defaults.ascent", this.defaultAscent);
            master.set("defaults.height", this.defaultHeight);
            master.set("registrations", null); // clear
            for (Map.Entry<String, String> e : registrations.entrySet()) {
                master.set("registrations." + e.getKey(), e.getValue());
            }
            try {
                master.save(masterFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to save char registry", ex);
            }
        }

        // ── Load persisted variants (same image, different ascent) ──
        variants.clear();
        ConfigurationSection varSec = master.getConfigurationSection("variants");
        if (varSec != null) {
            for (String key : varSec.getKeys(false)) {
                String varChar = varSec.getString(key);
                if (varChar == null || varChar.isEmpty()) continue;
                // key format: "baseName:ascent"
                int colon = key.lastIndexOf(':');
                if (colon < 0) continue;
                String baseName = key.substring(0, colon);
                int ascent;
                try {
                    ascent = Integer.parseInt(key.substring(colon + 1));
                } catch (NumberFormatException e) {
                    continue;
                }
                CharEntry base = getEntry(baseName);
                if (base == null) continue;
                variants.put(key, new CharEntry(key, varChar,
                        base.getTextureFile(), ascent, base.getHeight()));
            }
        }

        plugin.getLogger().info("Loaded " + entries.size() + " char registrations"
                + (variants.isEmpty() ? "." : " + " + variants.size() + " variants."));
    }

    /** All registered entries. */
    public List<CharEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Lookup by entry name. */
    public CharEntry getEntry(String name) {
        for (CharEntry e : entries) {
            if (e.getName().equals(name)) return e;
        }
        return null;
    }

    /**
     * Returns all entries including variants, for font/resource-pack
     * generation.
     */
    public List<CharEntry> getAllEntries() {
        if (variants.isEmpty()) return getEntries();
        List<CharEntry> all = new ArrayList<>(entries);
        all.addAll(variants.values());
        return Collections.unmodifiableList(all);
    }

    /**
     * Gets or creates a variant of a base char entry with a custom
     * ascent value.  The variant shares the same texture but receives
     * a unique Unicode character from the U+EA00+ pool and its own
     * bitmap font provider in the resource pack.
     *
     * @param baseName the base char entry name (e.g. "test_icon")
     * @param ascent   the desired ascent for the variant
     * @return the variant CharEntry, or null if the base entry is missing
     */
    public CharEntry getOrCreateVariant(String baseName, int ascent) {
        String key = baseName + ":" + ascent;
        CharEntry existing = variants.get(key);
        if (existing != null) return existing;

        CharEntry base = getEntry(baseName);
        if (base == null) return null;

        // Assign a Unicode char from the variant pool
        YamlConfiguration master = masterFile.exists()
                ? YamlConfiguration.loadConfiguration(masterFile)
                : new YamlConfiguration();

        String assigned = master.getString("variants." + key);
        if (assigned == null || assigned.isEmpty()) {
            Set<String> usedChars = new HashSet<>();
            for (CharEntry e : entries) usedChars.add(e.getCharacter());
            for (CharEntry e : variants.values()) usedChars.add(e.getCharacter());

            int idx = 0;
            while (usedChars.contains(variantCharAt(idx))) idx++;
            assigned = variantCharAt(idx);

            master.set("variants." + key, assigned);
            try {
                master.save(masterFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to persist variant " + key, ex);
            }
        }

        CharEntry variant = new CharEntry(key, assigned,
                base.getTextureFile(), ascent, base.getHeight());
        variants.put(key, variant);
        return variant;
    }

    // ── Persistence / auto-rebuild support ───────────────────────

    /**
     * Returns true if the current entry set differs from the last
     * built state.
     */
    public boolean isStale() {
        String current = computeFingerprint();
        String stored = masterFile.exists()
                ? YamlConfiguration.loadConfiguration(masterFile)
                        .getString("last_built_fingerprint", "")
                : "";
        return !current.equals(stored);
    }

    /** Saves the current fingerprint so future starts know the pack is current. */
    public void markBuilt() {
        YamlConfiguration master = masterFile.exists()
                ? YamlConfiguration.loadConfiguration(masterFile)
                : new YamlConfiguration();
        master.set("last_built_fingerprint", computeFingerprint());
        try {
            master.save(masterFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to persist char build fingerprint", ex);
        }
    }

    /**
     * Builds a short hash over all entry names, chars, texture paths,
     * ascent and height values.
     */
    private String computeFingerprint() {
        StringBuilder raw = new StringBuilder();
        for (CharEntry e : entries) {
            raw.append(e.getName()).append("|")
                    .append(e.getCharacter()).append("|")
                    .append(e.getTexturePackPath()).append("|")
                    .append(e.getAscent()).append("|")
                    .append(e.getHeight())
                    .append("\n");
        }
        for (CharEntry e : variants.values()) {
            raw.append("variant:").append(e.getName()).append("|")
                    .append(e.getCharacter()).append("|")
                    .append(e.getAscent())
                    .append("\n");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private static String charAt(int idx) {
        return new String(Character.toChars(CHAR_START + idx));
    }

    private static String variantCharAt(int idx) {
        return new String(Character.toChars(VARIANT_START + idx));
    }
}
