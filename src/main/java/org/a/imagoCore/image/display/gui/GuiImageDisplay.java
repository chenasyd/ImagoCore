package org.a.imagoCore.image.display.gui;

import org.a.imagoCore.image.ImageRenderer;
import org.a.imagoCore.image.display.ImageDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

/**
 * An {@link ImageDisplay} that renders an image as part of an
 * inventory GUI (e.g. a custom item in a fixed slot).
 *
 * <p>This is used by the image overlay system — other plugins attach
 * image data to inventory slots via this display.
 */
public class GuiImageDisplay implements ImageDisplay {

    private final Player player;
    private final ImageRenderer renderer;
    private final int slot;
    private final String id;
    private boolean active;

    public GuiImageDisplay(Player player, ImageRenderer renderer,
                           int slot, String id) {
        this.player = player;
        this.renderer = renderer;
        this.slot = slot;
        this.id = id;
        this.active = true;
    }

    @Override
    public void show(BufferedImage frame) {
        if (!active) return;
        // TODO: create ItemStack via renderer and set it in the player's
        //       open inventory at `slot`
    }

    @Override
    public void hide() {
        this.active = false;
        // TODO: clear the slot in the player's open inventory
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public ImageRenderer getRenderer() {
        return renderer;
    }

    @Override
    public String getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public int getSlot() {
        return slot;
    }
}
