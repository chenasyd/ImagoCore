package org.a.imagoCore.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Config section for resource-pack-based GUI title image rendering.
 *
 * <p>Values map to the custom font definitions in the resource pack:
 * <ul>
 *   <li>{@code background_char} — Unicode private-use char mapped to the background texture</li>
 *   <li>{@code shift_char_coarse} — char with -16px advance (coarse negative offset)</li>
 *   <li>{@code shift_char_fine} — char with -8px advance (fine negative offset)</li>
 *   <li>{@code shift_x} — total negative X offset needed for alignment (negative = left)</li>
 * </ul>
 *
 * @see org.a.imagoCore.image.display.gui.GuiTitleRenderer
 */
public class GuiTitleConfig {

    private final String backgroundChar;
    private final String shiftCharCoarse;
    private final String shiftCharFine;
    private final int shiftX;
    private final int ascent;
    private final int height;

    public GuiTitleConfig(ConfigurationSection section) {
        if (section == null) {
            this.backgroundChar = "\uE800";
            this.shiftCharCoarse = "\uE801";
            this.shiftCharFine = "\uE802";
            this.shiftX = -8;
            this.ascent = 13;
            this.height = 222;
            return;
        }
        this.backgroundChar = section.getString("background_char", "\uE800");
        this.shiftCharCoarse = section.getString("shift_char_coarse", "\uE801");
        this.shiftCharFine = section.getString("shift_char_fine", "\uE802");
        this.shiftX = section.getInt("shift_x", -8);
        this.ascent = section.getInt("ascent", 13);
        this.height = section.getInt("height", 222);
    }

    public String getBackgroundChar() {
        return backgroundChar;
    }

    public String getShiftCharCoarse() {
        return shiftCharCoarse;
    }

    public String getShiftCharFine() {
        return shiftCharFine;
    }

    /** Total negative X offset in pixels (negative = shift left). */
    public int getShiftX() {
        return shiftX;
    }

    /** Font ascent value matching the resource-pack font definition. */
    public int getAscent() {
        return ascent;
    }

    /** Font height value matching the resource-pack font definition. */
    public int getHeight() {
        return height;
    }
}
