package org.a.imagoCore.command.test;

import org.a.imagoCore.ImagoCore;
import org.a.imagoCore.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * {@code /imagocore test} — container for test-related actions.
 *
 * <p>Dispatches to action handlers registered by name/alias.
 * Currently supports: {@code gui} (alias {@code g}).
 */
public class TestCommand implements SubCommand {

    private static final List<String> ALIASES = List.of("t");

    private final ImagoCore plugin;
    private final Map<String, TestAction> actions = new LinkedHashMap<>();

    public TestCommand(ImagoCore plugin) {
        this.plugin = plugin;
    }

    /** Register a test action. */
    public void register(@NotNull TestAction action) {
        actions.put(action.getName().toLowerCase(), action);
        for (String alias : action.getAliases()) {
            actions.putIfAbsent(alias.toLowerCase(), action);
        }
    }

    @Override
    public @NotNull String getName() {
        return "test";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return ALIASES;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /imagocore test <action> [args...]");
            sender.sendMessage("Actions: gui (g)");
            return true;
        }

        TestAction action = actions.get(args[0].toLowerCase());
        if (action == null) {
            sender.sendMessage("Unknown test action: " + args[0]);
            return true;
        }

        String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);
        return action.execute(sender, actionArgs);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            return actions.keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .toList();
        }

        TestAction action = actions.get(args[0].toLowerCase());
        if (action == null) return Collections.emptyList();
        String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);
        return action.tabComplete(sender, actionArgs);
    }
}
