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
     * <p>Minecraft's bitmap font provider automatically prepends
     * {@code textures/} to the path, so the font JSON says
     * {@code minecraft:textures/char/...} but the file is stored at
     * {@code textures/textures/char/...} inside the zip.
     */
    public String getTexturePackPath() {
        return "minecraft:textures/char/" + textureFile.getName();
    }

    /**
     * Zip entry path for the texture file.
     *
     * <p>Double {@code textures/} accounts for Minecraft's auto-prefix.
     */
    public String getZipEntryPath() {
        return "assets/minecraft/textures/textures/char/"
                + textureFile.getName();
    }
}
