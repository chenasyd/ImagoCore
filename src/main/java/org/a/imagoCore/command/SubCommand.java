package org.a.imagoCore.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * A single sub-command within the ImagoCore command tree.
 *
 * <p>Implementations must return a name and alias list, and provide
 * execution / tab-completion logic.
 */
public interface SubCommand {

    /** Primary sub-command name (e.g. "test"). */
    @NotNull String getName();

    /** Aliases (e.g. ["t"]). May be empty. */
    default @NotNull List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Execute this sub-command.
     *
     * @param sender command sender
     * @param args   remaining arguments after the sub-command name
     * @return true if the syntax was valid
     */
    boolean execute(@NotNull CommandSender sender, @NotNull String[] args);

    /**
     * Tab-complete this sub-command.
     *
     * @param sender command sender
     * @param args   remaining arguments after the sub-command name
     * @return list of completions (empty = none)
     */
    default @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String[] args) {
        return Collections.emptyList();
    }
}
