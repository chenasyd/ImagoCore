package org.a.imagoCore.command.test;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * A single action under the {@code /imagocore test} command tree.
 *
 * @see TestCommand
 */
public interface TestAction {

    /** Primary action name (e.g. "gui"). */
    @NotNull String getName();

    /** Aliases (e.g. ["g"]). May be empty. */
    default @NotNull List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Execute this test action.
     *
     * @param sender command sender
     * @param args   remaining arguments after the action name
     * @return true if the syntax was valid
     */
    boolean execute(@NotNull CommandSender sender, @NotNull String[] args);

    /**
     * Tab-complete this action.
     */
    default @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String[] args) {
        return Collections.emptyList();
    }
}
