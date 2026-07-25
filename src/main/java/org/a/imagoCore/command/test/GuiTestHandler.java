package org.a.imagoCore.command.test;

import net.kyori.adventure.text.Component;
import org.a.imagoCore.ImagoCore;
import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.config.GuiRegistry;
import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /imagocore test gui [id] [fill]} — opens a test inventory with the
 * registered GUI background image.  Defaults to the first registered GUI
 * (usually "54-default") if no id is given.
 *
 * <p>Append {@code false} / {@code off} / {@code 0} to suppress filler
 * items, leaving the GUI slots empty so the background image is clearly
 * visible without obstructions.  Filler is on by default.
 *
 * <p>Examples:
 * <pre>{@code
 *   /ic t g                    # first GUI, filler on
 *   /ic t g 54-default         # specific GUI, filler on
 *   /ic t g 54-default false   # specific GUI, no filler
 *   /ic t g off                # first GUI, no filler
 * }</pre>
 */
public class GuiTestHandler implements TestAction {

    private static final List<String> ALIASES = List.of("g");
    private static final List<String> FILL_OFF_VALUES = List.of("false", "off", "0", "no");

    private final ImagoCore plugin;

    public GuiTestHandler(ImagoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "gui";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return ALIASES;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        GuiRegistry registry = plugin.getGuiRegistry();
        GuiEntry entry;
        String entryId;
        boolean fill = true;

        // Parse: args may be [id] [fill]  or just [fill]
        String first = args.length >= 1 && !args[0].isEmpty() ? args[0] : null;
        String last = args.length >= 2 && !args[args.length - 1].isEmpty()
                ? args[args.length - 1] : null;

        if (last != null && FILL_OFF_VALUES.contains(last.toLowerCase())) {
            fill = false;
        }

        // Determine if first arg is a GUI id or a fill flag
        if (first != null && !FILL_OFF_VALUES.contains(first.toLowerCase())) {
            entry = registry.getEntry(first);
            if (entry == null) {
                sender.sendMessage("§cGUI not found: " + first);
                sender.sendMessage("§7Available: " + registry.getEntries().stream()
                        .map(GuiEntry::getId).collect(Collectors.joining(", ")));
                return true;
            }
            entryId = entry.getId();
        } else {
            List<GuiEntry> all = registry.getEntries();
            if (all.isEmpty()) {
                sender.sendMessage("§cNo GUI registrations found. Add directories under gui/.");
                return true;
            }
            entry = all.get(0);
            entryId = entry.getId();
            if (first != null) fill = false; // first was the fill flag
        }

        Component title = GuiTitleRenderer.build(entry, registry);
        Inventory inv = createInventoryWithComponent(entry.getSlots(), title, entryId);

        if (fill) {
            ItemStack indicator = createIndicatorItem();
            int slots = entry.getSlots();
            for (int i = 0; i < slots; i++) {
                inv.setItem(i, indicator);
            }
        }

        player.openInventory(inv);

        int codePoint = entry.getBackgroundChar().codePointAt(0);
        String hex = Integer.toString(codePoint, 16);
        sender.sendMessage("§aOpened GUI §7" + entryId + "§a (\\u" + hex
                + ", " + entry.getSlots() + " slots, shiftX=" + entry.getShiftX()
                + ", fill=" + fill + ")");

        plugin.getLogger().info("Player " + player.getName()
                + " opened GUI test id=" + entryId
                + " char=U+" + hex + " slots=" + entry.getSlots()
                + " fill=" + fill);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> ids = plugin.getGuiRegistry().getEntries().stream()
                    .map(GuiEntry::getId)
                    .filter(id -> id.startsWith(partial))
                    .collect(Collectors.toList());
            // Also suggest fill flags if partial matches
            for (String val : FILL_OFF_VALUES) {
                if (val.startsWith(partial)) ids.add(val);
            }
            return ids;
        }
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return FILL_OFF_VALUES.stream()
                    .filter(v -> v.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static ItemStack createIndicatorItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7ImagoCore GUI Test");
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Reflection helper (same as GuiController) ───────────────

    private static Method CREATE_INV;
    private static boolean INIT = false;

    private static Inventory createInventoryWithComponent(int size, Component title, String fallback) {
        if (!INIT) {
            INIT = true;
            try {
                CREATE_INV = Bukkit.class.getMethod("createInventory",
                        org.bukkit.inventory.InventoryHolder.class, int.class, Component.class);
            } catch (NoSuchMethodException e) {
                CREATE_INV = null;
            }
        }
        if (CREATE_INV != null) {
            try {
                return (Inventory) CREATE_INV.invoke(null, null, size, title);
            } catch (Exception ignored) {
            }
        }
        return Bukkit.createInventory(null, size, fallback);
    }
}
