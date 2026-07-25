package org.a.imagoCore.config;

import java.io.File;

/**
 * A single registered GUI background entry.
 *
 * <p>Each entry is defined inside a slot-size folder under {@code gui/}.
 * Multiple entries can coexist in the same folder, each referencing a
 * different texture file via the folder's {@code gui.yml}.
 *
 * <p>The entry ID is formed as {@code <folderId>-<entryName>},
 * e.g. {@code "54-default"} or {@code "54-premium"}.
 */
public class GuiEntry {

    private final String folderId;
    private final String entryName;
    private final int slots;
    private final int ascent;
    private final int height;
    private final int shiftX;
    private final String backgroundChar;
    private final File textureFile;
    private final int textureWidth;   // source PNG pixel width
    private final int textureHeight;  // source PNG pixel height

    public GuiEntry(String folderId, String entryName, int slots,
                    int ascent, int height, int shiftX,
                    String backgroundChar, File textureFile,
                    int textureWidth, int textureHeight) {
        this.folderId = folderId;
        this.entryName = entryName;
        this.slots = slots;
        this.ascent = ascent;
        this.height = height;
        this.shiftX = shiftX;
        this.backgroundChar = backgroundChar;
        this.textureFile = textureFile;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    /**
     * Backward-compatible constructor (assumes texture dimensions match
     * the configured height, i.e. 1:1 scale).
     */
    public GuiEntry(String folderId, String entryName, int slots,
                    int ascent, int height, int shiftX,
                    String backgroundChar, File textureFile) {
        this(folderId, entryName, slots, ascent, height, shiftX,
                backgroundChar, textureFile, 176, height);
    }

    /** Globally unique ID: {@code "<folderId>-<entryName>"}. */
    public String getId() {
        return folderId + "-" + entryName;
    }

    /** Slot-size folder (e.g. "54", "27"). */
    public String getFolderId() { return folderId; }

    /** Entry name within the folder (e.g. "default", "premium"). */
    public String getEntryName() { return entryName; }

    /** Number of inventory slots. */
    public int getSlots() { return slots; }

    /** Font ascent matching the resource-pack font definition. */
    public int getAscent() { return ascent; }

    /** Font height matching the resource-pack font definition. */
    public int getHeight() { return height; }

    /** Total negative X offset for alignment. */
    public int getShiftX() { return shiftX; }

    /** Assigned Unicode private-use character for the background. */
    public String getBackgroundChar() { return backgroundChar; }

    /** The PNG texture file on disk. */
    public File getTextureFile() { return textureFile; }

    /** Source PNG pixel width. */
    public int getTextureWidth() { return textureWidth; }

    /** Source PNG pixel height. */
    public int getTextureHeight() { return textureHeight; }

    /**
     * The on-screen rendered width in pixels.
     *
     * <p>Minecraft scales bitmap font images proportionally:
     * {@code renderedWidth = (textureWidth / textureHeight) * fontHeight}.
     * This value is needed to calculate cursor position after rendering
     * the background, enabling precise overlay placement.
     */
    public int getRenderedWidth() {
        if (textureHeight == 0) return textureWidth;
        return (textureWidth * height) / textureHeight;
    }

    /**
     * Font JSON provider file path.
     *
     * <p>Minecraft's bitmap font provider automatically prepends
     * {@code textures/} to the path, so the font JSON says
     * {@code minecraft:textures/gui/...} but the file is stored at
     * {@code textures/textures/gui/...} inside the zip.
     */
    public String getTexturePackPath() {
        return "minecraft:textures/gui/" + folderId + "/" + textureFile.getName();
    }

    /**
     * Zip entry path for the texture file.
     *
     * <p>Because bitmap font providers auto-prepend {@code textures/},
     * the actual file path inside the zip has an extra level:
     * {@code assets/minecraft/textures/textures/gui/...}
     */
    public String getZipEntryPath() {
        return "assets/minecraft/textures/textures/gui/"
                + folderId + "/" + textureFile.getName();
    }
}
