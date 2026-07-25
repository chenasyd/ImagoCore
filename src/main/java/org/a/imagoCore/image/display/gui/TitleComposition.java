package org.a.imagoCore.image.display.gui;

import org.a.imagoCore.config.CharEntry;
import org.a.imagoCore.config.GuiEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complete title composition consisting of a background layer
 * and zero or more overlay layers.
 *
 * <p>Use the {@link Builder} to construct instances:
 * <pre>{@code
 * TitleComposition comp = TitleComposition.builder()
 *     .background(guiEntry)
 *     .overlay(charEntry, 20)          // icon at x=20px
 *     .overlay(charEntry2, 100, 15)    // icon at x=100px, ascent=15
 *     .build();
 *
 * Component title = GuiTitleRenderer.build(comp);
 * }</pre>
 *
 * @see TitleLayer
 * @see GuiTitleRenderer
 */
public class TitleComposition {

    private final TitleLayer background;
    private final List<TitleLayer> overlays;

    private TitleComposition(TitleLayer background, List<TitleLayer> overlays) {
        this.background = background;
        this.overlays = Collections.unmodifiableList(overlays);
    }

    /** @return the background layer (always present) */
    public TitleLayer getBackground() {
        return background;
    }

    /** @return immutable list of overlay layers (may be empty) */
    public List<TitleLayer> getOverlays() {
        return overlays;
    }

    /** @return true if this composition has overlay layers */
    public boolean hasOverlays() {
        return !overlays.isEmpty();
    }

    /** @return total number of layers (1 background + N overlays) */
    public int getLayerCount() {
        return 1 + overlays.size();
    }

    // ── Builder ─────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TitleLayer background;
        private final List<TitleLayer> overlays = new ArrayList<>();

        /**
         * Sets the background layer.
         *
         * @param entry the GUI background entry
         */
        public Builder background(GuiEntry entry) {
            this.background = TitleLayer.background(entry);
            return this;
        }

        /**
         * Adds an overlay layer at the given X offset, using the
         * char entry's default ascent.
         *
         * @param entry   character image entry
         * @param offsetX horizontal offset from left edge (pixels)
         */
        public Builder overlay(CharEntry entry, int offsetX) {
            overlays.add(TitleLayer.overlay(entry, offsetX));
            return this;
        }

        /**
         * Adds an overlay layer at the given X offset with a custom ascent.
         *
         * @param entry   character image entry
         * @param offsetX horizontal offset from left edge (pixels)
         * @param ascent  vertical positioning override
         */
        public Builder overlay(CharEntry entry, int offsetX, int ascent) {
            overlays.add(TitleLayer.overlay(entry, offsetX, ascent));
            return this;
        }

        /**
         * Adds a pre-built overlay layer.
         */
        public Builder overlay(TitleLayer layer) {
            if (!layer.isOverlay()) {
                throw new IllegalArgumentException("Layer must be an OVERLAY type");
            }
            overlays.add(layer);
            return this;
        }

        public TitleComposition build() {
            if (background == null) {
                throw new IllegalStateException("Background layer is required");
            }
            return new TitleComposition(background, new ArrayList<>(overlays));
        }
    }
}
