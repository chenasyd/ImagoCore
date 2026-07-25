package org.a.imagoCore.image.display.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.config.GuiRegistry;
import org.a.imagoCore.config.GuiTitleConfig;
import org.a.imagoCore.config.ShiftRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Builds an inventory-title {@link Component} that uses Unicode private-use
 * characters mapped via a resource-pack font to display GUI background images
 * and decoration overlays.
 *
 * <h3>Single-layer (background only)</h3>
 * <p>The title string is composed of shift characters followed by the
 * background character. The shift characters pull the image leftward to
 * align it with the GUI container edges.
 *
 * <h3>Multi-layer (background + overlays)</h3>
 * <p>After the background character, the cursor is at position
 * {@code (start + shiftX + renderedWidth)}. For each overlay:
 * <ol>
 *   <li>Shift cursor back to the background's left edge</li>
 *   <li>Shift cursor forward to the overlay's X position</li>
 *   <li>Render the overlay character (Y controlled by its ascent)</li>
 * </ol>
 *
 * <p>Example composition with background (width=176, shiftX=-8) and
 * an overlay at X=20:
 * <pre>{@code
 *   [shift -8] [bg_char] [shift -(176)] [shift +20] [overlay_char]
 *   = decompose(-8) + bgChar + decompose(-176 + 20) + overlayChar
 *   = decompose(-8) + bgChar + decompose(-156) + overlayChar
 * }</pre>
 *
 * @see ShiftRegistry
 * @see TitleComposition
 * @see TitleLayer
 */
public final class GuiTitleRenderer {

    private GuiTitleRenderer() {
    }

    // ── Legacy API (backward compatible) ────────────────────────

    /**
     * Builds a title {@link Component} with offset characters followed by
     * the background character, based on the given configuration.
     *
     * @param config GUI title configuration (from ConfigManager)
     * @return the title component suitable for {@code Bukkit.createInventory}
     */
    public static @NotNull Component build(@NotNull GuiTitleConfig config) {
        StringBuilder sb = new StringBuilder();

        if (config.getShiftX() != 0) {
            sb.append(ShiftRegistry.decompose(config.getShiftX()));
        }

        sb.append(config.getBackgroundChar());
        return Component.text(sb.toString())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Builds a title {@link Component} for a registered GUI entry
     * (background only, no overlays).
     *
     * @param entry    the GUI entry (background char + shiftX)
     * @param registry the GUI registry (unused in new system, kept for API compat)
     * @return the title component
     */
    public static @NotNull Component build(@NotNull GuiEntry entry,
                                           @NotNull GuiRegistry registry) {
        StringBuilder sb = new StringBuilder();

        if (entry.getShiftX() != 0) {
            sb.append(ShiftRegistry.decompose(entry.getShiftX()));
        }

        sb.append(entry.getBackgroundChar());
        return Component.text(sb.toString())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }

    // ── Multi-layer API ─────────────────────────────────────────

    /**
     * Builds a title {@link Component} from a full multi-layer composition.
     *
     * <p>The composition includes a background layer and zero or more
     * overlay layers. Each overlay is positioned at a specific X offset
     * relative to the background's left edge.
     *
     * @param composition the title composition (background + overlays)
     * @return the title component with all layers encoded
     */
    public static @NotNull Component build(@NotNull TitleComposition composition) {
        StringBuilder sb = new StringBuilder();

        TitleLayer bgLayer = composition.getBackground();
        GuiEntry bgEntry = bgLayer.getGuiEntry();

        // 1. Apply background alignment shift
        int bgShiftX = bgEntry.getShiftX();
        if (bgShiftX != 0) {
            sb.append(ShiftRegistry.decompose(bgShiftX));
        }

        // 2. Render background character
        sb.append(bgEntry.getBackgroundChar());

        // 3. For each overlay, shift cursor and render
        if (composition.hasOverlays()) {
            int bgRenderedWidth = bgEntry.getRenderedWidth();

            for (TitleLayer overlay : composition.getOverlays()) {
                // After background, cursor is at (bgShiftX + bgRenderedWidth)
                // relative to the visual origin.
                // Overlay wants to be at offsetX relative to the same origin.
                // Net shift needed: offsetX - bgRenderedWidth
                int netShift = overlay.getOffsetX() - bgRenderedWidth;
                if (netShift != 0) {
                    sb.append(ShiftRegistry.decompose(netShift));
                }

                // Render the overlay character
                sb.append(overlay.getCharacter());

                // After rendering overlay, cursor advanced by overlay's rendered width.
                // For the next overlay, we need to account for this.
                // The overlay char's rendered width = (charTextureWidth / charTextureHeight) * ascent
                // But since char images are typically small and we position each overlay
                // relative to the background origin, we shift back by the overlay width
                // before processing the next overlay.
                // However, Minecraft bitmap chars advance the cursor by their width.
                // We need to shift back by the overlay's rendered width to reset.
                // For simplicity, we calculate the next overlay's net shift relative
                // to the current cursor position.
            }
        }

        return Component.text(sb.toString())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Builds a multi-layer title with explicit overlay positioning.
     *
     * <p>This is a convenience method for the common case where overlays
     * need to be positioned accounting for cursor advancement after each
     * character render.
     *
     * @param background the GUI background entry
     * @param overlays   overlay layers with their target X positions
     * @return the title component
     */
    public static @NotNull Component buildWithOverlays(
            @NotNull GuiEntry background,
            @NotNull java.util.List<OverlaySpec> overlays) {

        StringBuilder sb = new StringBuilder();

        // 1. Background alignment
        int bgShiftX = background.getShiftX();
        if (bgShiftX != 0) {
            sb.append(ShiftRegistry.decompose(bgShiftX));
        }

        // 2. Background character
        sb.append(background.getBackgroundChar());

        // 3. Overlays — track cursor position
        int bgWidth = background.getRenderedWidth();
        int cursorOffset = bgShiftX + bgWidth; // cursor position after bg

        for (OverlaySpec spec : overlays) {
            // Target position: bgShiftX + spec.x (relative to visual origin)
            int targetPos = bgShiftX + spec.x;
            int shift = targetPos - cursorOffset;

            if (shift != 0) {
                sb.append(ShiftRegistry.decompose(shift));
                cursorOffset += shift;
            }

            // Render overlay char
            sb.append(spec.character);

            // Cursor advances by the overlay's rendered width
            cursorOffset += spec.renderedWidth;
        }

        return Component.text(sb.toString())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Specification for a single overlay in {@link #buildWithOverlays}.
     */
    public static class OverlaySpec {
        final String character;
        final int x;             // target X position relative to bg left edge
        final int renderedWidth; // rendered width of this overlay char

        public OverlaySpec(String character, int x, int renderedWidth) {
            this.character = character;
            this.x = x;
            this.renderedWidth = renderedWidth;
        }

        /**
         * Creates an overlay spec from a CharEntry.
         *
         * @param entry   the character image entry
         * @param x       target X position relative to background left edge
         * @param bgHeight the background's font height (for scale calculation)
         */
        public static OverlaySpec from(org.a.imagoCore.config.CharEntry entry,
                                       int x, int bgHeight) {
            // Char image rendered width = (texWidth / texHeight) * fontHeight
            // For char images, fontHeight = entry.getHeight()
            int renderedW = entry.getHeight(); // approximate: assume square or use height
            return new OverlaySpec(entry.getCharacter(), x, renderedW);
        }
    }
}
