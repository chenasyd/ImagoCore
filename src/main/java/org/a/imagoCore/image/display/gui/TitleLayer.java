package org.a.imagoCore.image.display.gui;

import org.a.imagoCore.config.CharEntry;
import org.a.imagoCore.config.GuiEntry;

/**
 * Represents a single visual layer in a GUI title composition.
 *
 * <p>A title can contain multiple layers rendered as a sequence of
 * Unicode characters in the inventory title string. Each layer is
 * either:
 * <ul>
 *   <li><b>Background</b> — a full-size GUI background image ({@link GuiEntry})</li>
 *   <li><b>Overlay</b> — a smaller decoration/icon image ({@link CharEntry})
 *       positioned at a specific X/Y offset</li>
 * </ul>
 *
 * <h3>Rendering Order</h3>
 * <p>Layers are rendered in list order. The background should always
 * be first. Overlays are rendered on top by shifting the cursor back
 * to the origin after the background, then forward to the overlay's
 * X position. Y positioning is controlled by each overlay's
 * {@code ascent} value in the resource pack font definition.
 *
 * @see TitleComposition
 * @see GuiTitleRenderer
 */
public class TitleLayer {

    /** Layer type discriminator. */
    public enum Type {
        BACKGROUND,
        OVERLAY
    }

    private final Type type;

    // Background fields
    private final GuiEntry guiEntry;

    // Overlay fields
    private final CharEntry charEntry;
    private final int offsetX;  // horizontal offset from left edge (pixels)
    private final int ascent;   // vertical positioning override (-1 = use charEntry default)

    // ── Factory methods ─────────────────────────────────────────

    /**
     * Creates a background layer from a GUI entry.
     *
     * @param entry the GUI background entry
     */
    public static TitleLayer background(GuiEntry entry) {
        return new TitleLayer(Type.BACKGROUND, entry, null, 0, -1);
    }

    /**
     * Creates an overlay layer from a character image entry.
     *
     * @param entry   the character image entry
     * @param offsetX horizontal offset from the GUI left edge (pixels, 0-based)
     * @param ascent  vertical positioning override, or -1 to use the entry's default
     */
    public static TitleLayer overlay(CharEntry entry, int offsetX, int ascent) {
        return new TitleLayer(Type.OVERLAY, null, entry, offsetX, ascent);
    }

    /**
     * Creates an overlay layer using the entry's default ascent.
     */
    public static TitleLayer overlay(CharEntry entry, int offsetX) {
        return overlay(entry, offsetX, -1);
    }

    private TitleLayer(Type type, GuiEntry guiEntry, CharEntry charEntry,
                       int offsetX, int ascent) {
        this.type = type;
        this.guiEntry = guiEntry;
        this.charEntry = charEntry;
        this.offsetX = offsetX;
        this.ascent = ascent;
    }

    // ── Accessors ───────────────────────────────────────────────

    public Type getType() {
        return type;
    }

    public boolean isBackground() {
        return type == Type.BACKGROUND;
    }

    public boolean isOverlay() {
        return type == Type.OVERLAY;
    }

    /** @return the GUI entry (only for BACKGROUND type) */
    public GuiEntry getGuiEntry() {
        return guiEntry;
    }

    /** @return the char entry (only for OVERLAY type) */
    public CharEntry getCharEntry() {
        return charEntry;
    }

    /** @return horizontal offset in pixels from the left edge */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * @return the effective ascent for this layer.
     *         For overlays: returns the override ascent if set, otherwise the entry's default.
     *         For backgrounds: returns the GUI entry's ascent.
     */
    public int getEffectiveAscent() {
        if (type == Type.BACKGROUND) {
            return guiEntry.getAscent();
        }
        return ascent >= 0 ? ascent : charEntry.getAscent();
    }

    /**
     * @return the Unicode character for this layer's image.
     */
    public String getCharacter() {
        if (type == Type.BACKGROUND) {
            return guiEntry.getBackgroundChar();
        }
        return charEntry.getCharacter();
    }

    @Override
    public String toString() {
        if (type == Type.BACKGROUND) {
            return "TitleLayer[bg=" + guiEntry.getId() + "]";
        }
        return "TitleLayer[overlay=" + charEntry.getName()
                + " x=" + offsetX + " ascent=" + getEffectiveAscent() + "]";
    }
}
