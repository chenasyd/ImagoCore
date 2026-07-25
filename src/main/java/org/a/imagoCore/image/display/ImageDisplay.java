package org.a.imagoCore.image.display;

import org.a.imagoCore.image.ImageRenderer;

import java.awt.image.BufferedImage;

/**
 * Represents an active image display on a Minecraft surface.
 *
 * <p>Sub-types handle specific contexts:
 * <ul>
 *   <li>{@link EntityImageDisplay} — item-frame / map entity display</li>
 *   <li>{@link GuiImageDisplay}    — inventory GUI overlay</li>
 * </ul>
 *
 * @see EntityImageDisplay
 * @see GuiImageDisplay
 */
public interface ImageDisplay {

    /**
     * Shows the given image frame on this display surface.
     *
     * @param frame the image to show
     */
    void show(BufferedImage frame);

    /**
     * Hides / removes this display from the surface.
     */
    void hide();

    /**
     * Returns whether this display is currently active.
     */
    boolean isActive();

    /**
     * Returns the renderer associated with this display.
     */
    ImageRenderer getRenderer();

    /**
     * Returns a human-readable identifier for this display instance.
     */
    String getId();
}
