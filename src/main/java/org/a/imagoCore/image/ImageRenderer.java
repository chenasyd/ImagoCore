package org.a.imagoCore.image;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;

/**
 * Core image rendering interface.
 *
 * Implementations handle converting a {@link BufferedImage} into
 * visible output on various Minecraft surfaces:
 * <ul>
 *   <li>Item-frames / Maps (entity display)</li>
 *   <li>GUI inventories (item lore / custom items)</li>
 * </ul>
 */
public interface ImageRenderer {

    /**
     * Renders a frame of an image onto a map canvas.
     *
     * @param canvas the map canvas to draw onto
     * @param frame  the current image frame (BufferedImage)
     */
    void render(MapCanvas canvas, BufferedImage frame);

    /**
     * Creates a rendered ItemStack for GUI display.
     *
     * @param image    the source image
     * @param player   target player (for per-player state)
     * @return ItemStack suitable for inventory display
     */
    ItemStack renderAsItem(BufferedImage image, Player player);

    /**
     * Returns the display name / identifier of this renderer.
     */
    String getName();
}
