package org.a.imagoCore.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the power-of-2 shift character system for pixel-precise
 * horizontal offset control in GUI titles.
 *
 * <p>Each shift character is a Unicode PUA codepoint mapped to a
 * {@code "space"} font provider with a specific advance value.
 * By composing multiple shift characters, any pixel offset within
 * the representable range can be achieved exactly.
 *
 * <h3>Character Layout (U+E801 – U+E810)</h3>
 * <pre>
 *   Negative (move cursor LEFT):
 *     \uE801 = -1px    \uE802 = -2px    \uE803 = -4px
 *     \uE804 = -8px    \uE805 = -16px   \uE806 = -32px
 *     \uE807 = -64px   \uE808 = -128px
 *
 *   Positive (move cursor RIGHT):
 *     \uE809 = +1px    \uE80A = +2px    \uE80B = +4px
 *     \uE80C = +8px    \uE80D = +16px   \uE80E = +32px
 *     \uE80F = +64px   \uE810 = +128px
 * </pre>
 *
 * <p>GUI background entries start at U+E820+ to avoid collision.
 * Character images use U+E900+.
 *
 * @see org.a.imagoCore.image.display.gui.GuiTitleRenderer
 */
public final class ShiftRegistry {

    /** Number of power-of-2 levels (1, 2, 4, 8, 16, 32, 64, 128). */
    public static final int LEVELS = 8;

    /** Maximum representable offset in one direction: 1+2+4+8+16+32+64+128 = 255. */
    public static final int MAX_OFFSET = 255;

    private static final int NEG_BASE = 0xE801; // \uE801 = -1px
    private static final int POS_BASE = 0xE809; // \uE809 = +1px

    /** power-of-2 values: [1, 2, 4, 8, 16, 32, 64] */
    private static final int[] POWERS = new int[LEVELS];

    static {
        for (int i = 0; i < LEVELS; i++) {
            POWERS[i] = 1 << i;
        }
    }

    private ShiftRegistry() {
    }

    // ── Character accessors ─────────────────────────────────────

    /**
     * Returns the Unicode character for a negative shift of {@code -(2^level)} pixels.
     *
     * @param level 0-based level (0 = -1px, 1 = -2px, ..., 6 = -64px)
     */
    public static String negativeChar(int level) {
        return new String(Character.toChars(NEG_BASE + level));
    }

    /**
     * Returns the Unicode character for a positive shift of {@code +(2^level)} pixels.
     *
     * @param level 0-based level (0 = +1px, 1 = +2px, ..., 6 = +64px)
     */
    public static String positiveChar(int level) {
        return new String(Character.toChars(POS_BASE + level));
    }

    // ── Offset decomposition ────────────────────────────────────

    /**
     * Decomposes a pixel offset into a string of shift characters.
     *
     * <p>Positive offset moves the cursor RIGHT (positive shift chars).
     * Negative offset moves the cursor LEFT (negative shift chars).
     * Zero returns an empty string.
     *
     * <p>Example: {@code decompose(-40)} → "\uE803\uE806" (-(4+32) = -36... no)
     * Actually: -40 = -(32 + 8) → "\uE806\uE804"
     *
     * @param pixels the offset in pixels (range: -127 to +127)
     * @return the shift character string
     * @throws IllegalArgumentException if |pixels| > MAX_OFFSET
     */
    public static String decompose(int pixels) {
        if (pixels == 0) return "";
        if (Math.abs(pixels) > MAX_OFFSET) {
            throw new IllegalArgumentException(
                    "Offset " + pixels + " exceeds max representable range ±" + MAX_OFFSET);
        }

        StringBuilder sb = new StringBuilder();
        int remaining = Math.abs(pixels);
        boolean negative = pixels < 0;

        // Decompose from largest power to smallest for minimal char count
        for (int i = LEVELS - 1; i >= 0; i--) {
            if (remaining >= POWERS[i]) {
                remaining -= POWERS[i];
                sb.append(negative ? negativeChar(i) : positiveChar(i));
            }
        }

        return sb.toString();
    }

    // ── Resource pack support ───────────────────────────────────

    /**
     * Returns all shift characters with their advance values,
     * for generating font providers in the resource pack.
     *
     * @return map of character → advance (negative = left, positive = right)
     */
    public static Map<String, Integer> getAllShiftAdvances() {
        Map<String, Integer> advances = new LinkedHashMap<>();
        for (int i = 0; i < LEVELS; i++) {
            advances.put(negativeChar(i), -POWERS[i]);
        }
        for (int i = 0; i < LEVELS; i++) {
            advances.put(positiveChar(i), POWERS[i]);
        }
        return advances;
    }

    /**
     * Returns the power-of-2 value for a given level.
     */
    public static int powerAt(int level) {
        return POWERS[level];
    }
}
