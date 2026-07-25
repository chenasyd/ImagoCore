package org.a.imagoCore.config;

import java.io.File;

/**
 * A single registered character-image entry.
 *
 * <p>Each entry maps a PNG image to a Unicode private-use character
 * via a bitmap font provider.  Unlike {@link GuiEntry}, character
 * images have no slot count or positional shift — they are
 * standalone glyphs that can be used anywhere in chat, titles,
 * scoreboards, etc.
 *
 * <p>Images are placed directly in {@code plugins/ImagoCore/char/}
 * and are auto-discovered by filename.
 */
public class CharEntry {

    private final String name;
    private final String character;
    private final File textureFile;
    private final int ascent;
    private final int height;

    public CharEntry(String name, String character, File textureFile,
                     int ascent, int height) {
        this.name = name;
        this.character = character;
        this.textureFile = textureFile;
        this.ascent = ascent;
        this.height = height;
    }

    /** Entry name (filename without {@code .png} extension). */
    public String getName() { return name; }

    /** Assigned Unicode private-use character. */
    public String getCharacter() { return character; }

    /** The PNG texture file on disk. */
    public File getTextureFile() { return textureFile; }

    /** Font ascent matching the resource-pack font definition. */
    public int getAscent() { return ascent; }

    /** Font height matching the resource-pack font definition. */
    public int getHeight() { return height; }

    /**
     * Font JSON provider file path.
     *
     * <p>Minecraft's bitmap font provider resolves {@code namespace:path}
     * to {@code assets/<namespace>/textures/<path>}.  Including the
     * {@code textures/} prefix here means the actual zip entry lives at
     * {@code assets/minecraft/textures/textures/char/...}, which matches
     * {@link #getZipEntryPath()}.
     */
    public String getTexturePackPath() {
        return "minecraft:textures/char/" + textureFile.getName();
    }

    /**
     * Zip entry path for the texture file.
     *
     * <p>The double {@code textures/} is intentional: the font JSON file
     * field already contains {@code textures/}, and Minecraft prepends
     * another {@code textures/} during resolution.
     */
    public String getZipEntryPath() {
        return "assets/minecraft/textures/textures/char/"
                + textureFile.getName();
    }
}
