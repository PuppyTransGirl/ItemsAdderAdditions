package toutouchien.itemsadderadditions.feature.recipe;

import org.jspecify.annotations.NullMarked;

/**
 * A single command entry inside an {@code on_complete.commands} block.
 *
 * <pre>{@code
 * give_reward:
 *   command: 'give {player} gold_ingot 16'
 *   as_console: true   # optional, default: false
 * }</pre>
 */
@NullMarked
public record RecipeCommand(String command, boolean asConsole) {}
