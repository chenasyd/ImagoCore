package org.a.imagoCore.command;

import org.a.imagoCore.ImagoCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command executor for {@code /imagocore} ({@code /ic}).
 *
 * <p>Dispatches to registered {@link SubCommand} implementations by
 * matching the first argument against each sub-command's name and aliases.
 *
 * <p>To add a new sub-command:
 * <pre>{@code
 * imagocoreCommand.register(new MySubCommand());
 * }</pre>
 */
public class ImagoCoreCommand implements TabExecutor {

    private final ImagoCore plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public ImagoCoreCommand(ImagoCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a sub-command.  Its name and all aliases are indexed.
     */
    public void register(@NotNull SubCommand sub) {
        subCommands.put(sub.getName().toLowerCase(), sub);
        for (String alias : sub.getAliases()) {
            subCommands.putIfAbsent(alias.toLowerCase(), sub);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /imagocore <subcommand> [args...]");
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage("Unknown sub-command: " + args[0]);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, subArgs);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String label,
                                                 @NotNull String[] args) {
        if (args.length <= 1) {
            // Complete sub-command names
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .collect(Collectors.toList());
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) return Collections.emptyList();

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.tabComplete(sender, subArgs);
    }
}
