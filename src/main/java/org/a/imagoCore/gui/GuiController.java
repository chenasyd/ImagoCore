package org.a.imagoCore.gui;

import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.config.GuiRegistry;
import org.a.imagoCore.image.display.gui.GuiImageDisplay;
import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.a.imagoCore.image.display.gui.TitleComposition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central controller for managing GUI image bindings and per-player sessions.
 *
 * <h3>Architecture</h3>
 * <p>
 * {@code GuiController} is the single orchestrator for ImagoCore's GUI
 * image overlay system. It manages:
 * <ul>
 *   <li><b>Image Bindings</b> — maps {@code guiId → GuiEntry} associations</li>
 *   <li><b>Player Sessions</b> — per-player {{@link GuiSession}} instances</li>
 *   <li><b>Inventory Creation</b> — builds Bukkit inventories with background images</li>
 * </ul>
 *
 * <pre>{@code
 *   // Bind a GUI image
 *   controller.bind("GuildSettingsGUI", guiRegistry.getEntry("27", "settings"));
 *
 *   // Open a GUI for a player with the bound image
 *   controller.openGui(player, "GuildSettingsGUI", 27, "Settings");
 * }</pre>
 *
 * @see GuiSession
 * @see GuiTitleRenderer
 */
public class GuiController {

    private final GuiRegistry guiRegistry;
    private final Logger logger;

    /** guiId (e.g. "GuildSettingsGUI") → GuiEntry (background image). */
    private final Map<String, GuiEntry> bindings = new ConcurrentHashMap<>();

    /** Player UUID → active GUI session. */
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public GuiController(GuiRegistry guiRegistry, Logger logger) {
        this.guiRegistry = guiRegistry;
        this.logger = logger;
    }

    // ── Lifecycle ────────────────────────────────────────────────

    /** Call during ImagoCore onEnable. */
    public void initialize() {
        logger.info("[GuiController] Initialized with "
                + guiRegistry.getEntries().size() + " GUI entries.");
    }

    /** Call during ImagoCore onDisable. */
    public void shutdown() {
        // Close all active sessions
        for (UUID uuid : sessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                closeSession(player);
            }
        }
        sessions.clear();
        bindings.clear();
        logger.info("[GuiController] Shut down.");
    }

    // ── Image Bindings ───────────────────────────────────────────

    /**
     * Bind a GUI entry (background image) to a logical GUI identifier.
     *
     * @param guiId  logical GUI ID (e.g. "GuildSettingsGUI") matching Guild Plugin's GUI types
     * @param entry  the GUI entry (background char + shift config)
     */
    public void bind(String guiId, GuiEntry entry) {
        bindings.put(guiId, entry);
        logger.info("[GuiController] Bound guiId=" + guiId + " → entry=" + entry.getId());
    }

    /**
     * Unbind a GUI identifier.
     * @return the removed entry, or null if not bound
     */
    public GuiEntry unbind(String guiId) {
        GuiEntry removed = bindings.remove(guiId);
        if (removed != null) {
            logger.info("[GuiController] Unbound guiId=" + guiId);
        }
        return removed;
    }

    /** Get the bound entry for a GUI identifier. */
    public GuiEntry getBinding(String guiId) {
        return bindings.get(guiId);
    }

    /** @return true if this guiId has a bound background image. */
    public boolean hasBinding(String guiId) {
        return bindings.containsKey(guiId);
    }

    /** @return an immutable copy of all current bindings. */
    public Map<String, GuiEntry> getBindings() {
        return Map.copyOf(bindings);
    }

    // ── GUI Opening ──────────────────────────────────────────────

    /**
     * Creates an {@link Inventory} with the bound background image title
     * for the given GUI ID, <b>without opening it</b>.
     *
     * <p>This allows the calling plugin to populate the inventory with
     * items before opening it for the player. Returns {@code null} if
     * no binding exists for the given guiId.
     *
     * @param size  inventory size (must be a multiple of 9)
     * @param guiId logical GUI ID (must be bound via {@link #bind})
     * @return the created inventory with image title, or null if no binding
     */
    public Inventory createTitledInventory(int size, String guiId) {
        GuiEntry entry = bindings.get(guiId);
        if (entry == null) {
            return null;
        }

        Component titleComponent = GuiTitleRenderer.build(entry, guiRegistry);
        return createInventoryWithComponent(size, titleComponent, guiId);
    }

    /**
     * Creates an {@link Inventory} with a multi-layer title composition
     * (background + overlay decorations), <b>without opening it</b>.
     *
     * @param size        inventory size (must be a multiple of 9)
     * @param composition the title composition (background + overlays)
     * @return the created inventory with multi-layer image title
     */
    public Inventory createTitledInventory(int size, TitleComposition composition) {
        Component titleComponent = GuiTitleRenderer.build(composition);
        return createInventoryWithComponent(size, titleComponent, "ImagoCore GUI");
    }

    /**
     * Builds the title {@link Component} for a bound GUI ID.
     *
     * @param guiId logical GUI ID
     * @return the title component, or null if no binding exists
     */
    public Component buildTitleComponent(String guiId) {
        GuiEntry entry = bindings.get(guiId);
        if (entry == null) {
            return null;
        }
        return GuiTitleRenderer.build(entry, guiRegistry);
    }

    /**
     * Builds a multi-layer title {@link Component} for a bound GUI ID
     * with additional overlay decorations.
     *
     * @param guiId    logical GUI ID (background)
     * @param overlays overlay specifications
     * @return the title component, or null if no binding exists
     */
    public Component buildTitleComponent(String guiId,
                                         java.util.List<GuiTitleRenderer.OverlaySpec> overlays) {
        GuiEntry entry = bindings.get(guiId);
        if (entry == null) {
            return null;
        }
        if (overlays == null || overlays.isEmpty()) {
            return GuiTitleRenderer.build(entry, guiRegistry);
        }
        return GuiTitleRenderer.buildWithOverlays(entry, overlays);
    }

    /**
     * Open a GUI with the bound background image for a player.
     *
     * @param player  the player
     * @param guiId   logical GUI ID (must be bound via {@link #bind})
     * @param size    inventory size (must be a multiple of 9)
     * @param title   fallback title text (used if no binding exists)
     * @return the created {@link GuiSession}, or null on failure
     */
    public GuiSession openGui(Player player, String guiId, int size, String title) {
        // Close existing session for this player
        closeSession(player);

        GuiEntry entry = bindings.get(guiId);

        Component titleComponent;
        if (entry != null) {
            titleComponent = GuiTitleRenderer.build(entry, guiRegistry);
        } else {
            titleComponent = Component.text(title);
            logger.fine("[GuiController] No binding for guiId=" + guiId
                    + " — using plain title.");
        }

        Inventory inv = createInventoryWithComponent(size, titleComponent, title);
        player.openInventory(inv);

        GuiSession session = new GuiSession(player, guiId, inv, entry);
        sessions.put(player.getUniqueId(), session);

        return session;
    }

    /**
     * Open a GUI overlay session — the player already has an inventory open
     * (e.g. Guild Plugin's GuildSettingsGUI), and ImagoCore applies the
     * background image overlay.
     *
     * @param player the player viewing the GUI
     * @param guiId  logical GUI ID
     * @param inv    the currently open inventory (from Guild Plugin)
     * @return the created session, or null if one already exists
     */
    public GuiSession openOverlay(Player player, String guiId, Inventory inv) {
        GuiSession existing = sessions.get(player.getUniqueId());
        if (existing != null && existing.getGuiId().equals(guiId)) {
            return existing; // Already showing the right overlay
        }

        closeSession(player);

        GuiEntry entry = bindings.get(guiId);
        GuiSession session = new GuiSession(player, guiId, inv, entry);
        sessions.put(player.getUniqueId(), session);

        // TODO: Re-render the inventory title with the bound image
        // This requires modifying the open inventory's title, which may
        // require closing and reopening in some Bukkit versions.

        return session;
    }

    // ── Session Management ───────────────────────────────────────

    /** Get the active GUI session for a player, or null. */
    public GuiSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /** @return true if the player has an active GUI session. */
    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /** Close and cleanup a player's GUI session. */
    public void closeSession(Player player) {
        GuiSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.close();
        }
    }

    /** @return number of active GUI sessions. */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    // ── Image Display ────────────────────────────────────────────

    /**
     * Create a {@link GuiImageDisplay} for a player's current GUI session.
     * <p>
     * TODO: Implement rendering pipeline — create ItemStack from image,
     * place in player's open inventory at the given slot.
     *
     * @param player   the player
     * @param slot     inventory slot for the image item
     * @param imageId  identifier for tracing
     * @return the display handle, or null if no active session
     */
    public GuiImageDisplay createImageDisplay(Player player, int slot, String imageId) {
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            logger.warning("[GuiController] No active session for player " + player.getName());
            return null;
        }

        // TODO: Implement via ImageRenderer → ItemStack → inventory.setItem(slot, item)
        // GuiImageDisplay display = new GuiImageDisplay(player, renderer, slot, imageId);
        // session.addDisplay(display);
        // display.show(renderer.nextFrame());
        // return display;

        logger.fine("[GuiController] createImageDisplay: stub — player="
                + player.getName() + " slot=" + slot + " imageId=" + imageId);
        return null;
    }

    // ── Overlay Management ───────────────────────────────────────

    /**
     * Register this GUI ID as managed by ImagoCore.
     * When Guild Plugin opens or closes a GUI with this ID, the bridge
     * will notify ImagoCore to apply/remove the overlay.
     */
    public void registerOverlayTarget(String guiId, GuiEntry entry) {
        bind(guiId, entry);
    }

    /** Unregister an overlay target. */
    public void unregisterOverlayTarget(String guiId) {
        unbind(guiId);
    }

    // ── Reflection helper ───────────────────────────────────────

    private static Method CREATE_INVENTORY_COMPONENT;
    private static boolean REFLECTION_INIT = false;

    /**
     * Creates an Inventory with a Component title using reflection.
     * Paper/Folia servers support {@code Bukkit.createInventory(null, size, Component)}.
     * On Spigot, falls back to a plain string title.
     */
    private static Inventory createInventoryWithComponent(int size, Component title, String fallbackTitle) {
        if (!REFLECTION_INIT) {
            REFLECTION_INIT = true;
            try {
                CREATE_INVENTORY_COMPONENT = Bukkit.class.getMethod(
                        "createInventory",
                        org.bukkit.inventory.InventoryHolder.class,
                        int.class,
                        Component.class
                );
            } catch (NoSuchMethodException e) {
                CREATE_INVENTORY_COMPONENT = null;
            }
        }

        if (CREATE_INVENTORY_COMPONENT != null) {
            try {
                return (Inventory) CREATE_INVENTORY_COMPONENT.invoke(null, null, size, title);
            } catch (Exception e) {
                // fall through to string fallback
            }
        }

        return Bukkit.createInventory(null, size, fallbackTitle);
    }
}
