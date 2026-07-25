package org.a.imagoCore.command.resource;

import org.a.imagoCore.ImagoCore;
import org.a.imagoCore.command.SubCommand;
import org.a.imagoCore.resource.pack.ResourcePackGenerator;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * {@code /imagocore resource} — manage and build the resource pack.
 *
 * <p>Sub-actions:
 * <ul>
 *   <li>{@code build} (alias {@code b}) — generates {@code build.zip}</li>
 * </ul>
 */
public class ResourceCommand implements SubCommand {

    private static final List<String> ALIASES = List.of("r");

    private final ImagoCore plugin;

    public ResourceCommand(ImagoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "resource";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return ALIASES;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0 || !"build".equalsIgnoreCase(args[0])
                && !"b".equalsIgnoreCase(args[0])) {
            sender.sendMessage("Usage: /imagocore resource build [output]");
            sender.sendMessage("Aliases: /ic r b, /ic resource b");
            return true;
        }

        String outputPath = args.length >= 2
                ? args[1]
                : plugin.getConfigManager()
                .getMainConfig().getResourcePackOutput();

        File outputFile = new File(outputPath);
        if (!outputFile.isAbsolute()) {
            if (outputPath.startsWith("plugins/") || outputPath.startsWith("plugins\\")) {
                outputFile = new File(outputPath);
            } else {
                outputFile = new File(plugin.getDataFolder(), outputPath);
            }
        }

        ResourcePackGenerator generator = new ResourcePackGenerator(
                plugin, outputFile, plugin.getGuiRegistry(),
                plugin.getCharRegistry());
        try {
            generator.build();
            sender.sendMessage("§aResource pack built: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            sender.sendMessage("§cFailed to build resource pack: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Resource pack build failed", e);
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            if ("build".startsWith(partial) || "b".startsWith(partial)) {
                return List.of("build", "b");
            }
        }
        return List.of();
    }
}
