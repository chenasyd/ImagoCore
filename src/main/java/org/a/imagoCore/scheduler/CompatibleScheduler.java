package org.a.imagoCore.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Unified scheduler that abstracts Spigot (BukkitScheduler) and Folia
 * (RegionizedScheduler) differences.
 *
 * <p>All Folia APIs are accessed via reflection to maintain compile-time
 * compatibility with Spigot API. At runtime on Folia, the reflective
 * calls resolve to the regionized schedulers.
 *
 * <p>Usage:
 * <pre>{@code
 * CompatibleScheduler sched = plugin.getScheduler();
 * sched.runTask(plugin, () -> player.sendMessage("Hello"));
 * sched.runTask(plugin, entity, () -> entity.teleport(loc));
 * }</pre>
 */
public class CompatibleScheduler {

    private final boolean folia;

    public CompatibleScheduler() {
        this.folia = detectFolia();
    }

    // ── Detection ────────────────────────────────────────────────

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    // ── Task scheduling ──────────────────────────────────────────

    /**
     * Runs a task on the global region (Spigot: main thread).
     */
    public void runTask(Plugin plugin, Runnable runnable) {
        if (plugin != null && !plugin.isEnabled()) return;

        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("run", Plugin.class, Consumer.class)
                        .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run());
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /**
     * Runs a task on the region that owns the given entity.
     * Safe for cross-world cross-region entity operations on Folia.
     */
    public void runTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (plugin != null && !plugin.isEnabled()) return;

        if (folia) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class)
                        .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(), (Runnable) () -> {});
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /**
     * Runs a repeating task on the entity's region.
     */
    public void runTaskTimer(Plugin plugin, Entity entity, Runnable runnable,
                             long delay, long period) {
        if (plugin != null && !plugin.isEnabled()) return;

        if (folia) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                scheduler.getClass().getMethod("runAtFixedRate",
                                Plugin.class, Consumer.class, Runnable.class, long.class, long.class)
                        .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(),
                                (Runnable) () -> {}, delay, period);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }

    /**
     * Runs a task after the specified delay on the global region.
     */
    public void runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        if (plugin != null && !plugin.isEnabled()) return;

        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                        .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(), delay);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    /**
     * Runs a task asynchronously.
     */
    public void runTaskAsync(Plugin plugin, Runnable runnable) {
        if (plugin != null && !plugin.isEnabled()) return;

        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class)
                        .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run());
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Cancels all tasks owned by the given plugin.
     * Call this in {@code onDisable()}.
     */
    public void cancelAll(Plugin plugin) {
        if (folia) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("cancelTasks", Plugin.class)
                        .invoke(globalScheduler, plugin);
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                asyncScheduler.getClass().getMethod("cancelTasks", Plugin.class)
                        .invoke(asyncScheduler, plugin);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
