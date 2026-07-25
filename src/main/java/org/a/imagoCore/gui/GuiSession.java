package org.a.imagoCore.gui;

import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.image.display.ImageDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-player GUI state maintained by {@link GuiController}.
 *
 * <p>Each session tracks:
 * <ul>
 *   <li>Which GUI the player is viewing</li>
 *   <li>The bound background {@link GuiEntry} (if any)</li>
 *   <li>Active {@link ImageDisplay} instances (images rendered in slots)</li>
 * </ul>
 */
public class GuiSession {

    private final Player player;
    private final String guiId;
    private final Inventory inventory;
    private final GuiEntry backgroundEntry;
    private final long openedAt;

    /** Active image displays within this session. */
    private final List<ImageDisplay> displays = new CopyOnWriteArrayList<>();

    private volatile boolean closed;

    public GuiSession(Player player, String guiId, Inventory inventory,
                      GuiEntry backgroundEntry) {
        this.player = player;
        this.guiId = guiId;
        this.inventory = inventory;
        this.backgroundEntry = backgroundEntry;
        this.openedAt = System.currentTimeMillis();
    }

    // ── Accessors ────────────────────────────────────────────────

    public Player getPlayer()              { return player; }
    public String getGuiId()               { return guiId; }
    public Inventory getInventory()        { return inventory; }
    public GuiEntry getBackgroundEntry()   { return backgroundEntry; }
    public long getOpenedAt()              { return openedAt; }
    public boolean isClosed()              { return closed; }

    /** @return the time this session has been open in milliseconds. */
    public long getOpenDurationMs() {
        return System.currentTimeMillis() - openedAt;
    }

    // ── Image Displays ───────────────────────────────────────────

    /**
     * Add an image display to this session.
     * TODO: Implement actual rendering pipeline.
     */
    public void addDisplay(ImageDisplay display) {
        displays.add(display);
    }

    /**
     * Remove an image display from this session.
     * TODO: Implement cleanup (clear slot, release resources).
     */
    public void removeDisplay(ImageDisplay display) {
        displays.remove(display);
    }

    /** @return immutable snapshot of active displays. */
    public List<ImageDisplay> getDisplays() {
        return Collections.unmodifiableList(displays);
    }

    /** @return number of active image displays. */
    public int getDisplayCount() {
        return displays.size();
    }

    // ── Lifecycle ────────────────────────────────────────────────

    /**
     * Close this session and cleanup all resources.
     * Called by {@link GuiController#closeSession(Player)}.
     */
    void close() {
        this.closed = true;

        // TODO: Hide all active displays
        for (ImageDisplay display : displays) {
            display.hide();
        }
        displays.clear();

        // TODO: Restore original inventory title if modified
    }

    @Override
    public String toString() {
        return String.format("GuiSession[player=%s gui=%s duration=%ds displays=%d]",
                player.getName(), guiId, getOpenDurationMs() / 1000, displays.size());
    }
}
