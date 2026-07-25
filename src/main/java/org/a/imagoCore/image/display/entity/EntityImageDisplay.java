package org.a.imagoCore.image.display.entity;

import org.a.imagoCore.image.ImageRenderer;
import org.a.imagoCore.image.display.ImageDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;

/**
 * An {@link ImageDisplay} backed by a Minecraft entity (e.g. item-frame
 * or glow-item-frame) used to display images in the world.
 *
 * <p>Entity operations are always scheduled through the entity's region
 * scheduler (Folia-safe).
 */
public class EntityImageDisplay implements ImageDisplay {

    private final UUID worldId;
    private final int entityId;
    private final ImageRenderer renderer;
    private final String id;
    private boolean active;

    public EntityImageDisplay(UUID worldId, int entityId,
                              ImageRenderer renderer, String id) {
        this.worldId = worldId;
        this.entityId = entityId;
        this.renderer = renderer;
        this.id = id;
        this.active = true;
    }

    @Override
    public void show(BufferedImage frame) {
        // TODO: look up entity by worldId + entityId, apply frame via renderer
    }

    @Override
    public void hide() {
        this.active = false;
        // TODO: remove map/clear item-frame
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

    public UUID getWorldId() {
        return worldId;
    }

    public int getEntityId() {
        return entityId;
    }
}
