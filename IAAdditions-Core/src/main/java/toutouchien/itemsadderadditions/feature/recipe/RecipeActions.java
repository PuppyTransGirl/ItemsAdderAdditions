package toutouchien.itemsadderadditions.feature.recipe;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Sound + commands to fire when a recipe completes.
 *
 * <h3>YAML structure (under {@code on_complete:})</h3>
 * <pre>{@code
 * on_complete:
 *   sound:
 *     name:   "minecraft:entity.player.levelup"
 *     source: master    # optional
 *     volume: 1.0       # optional
 *     pitch:  1.0       # optional
 *   commands:
 *     give_reward:
 *       command:    'give {player} gold_ingot 16'
 *       as_console: true    # optional, default: false
 *     notify:
 *       command:    'tellraw {player} {"text":"Done!","color":"gold"}'
 *       as_console: true
 * }</pre>
 *
 * <p>{@code {player}} in command strings is replaced with the player's name at execution time.
 */
@NullMarked
public final class RecipeActions {
    public static final RecipeActions EMPTY = new RecipeActions(null, List.of());

    private final @Nullable Sound sound;
    private final List<RecipeCommand> commands;

    public RecipeActions(@Nullable Sound sound, List<RecipeCommand> commands) {
        this.sound = sound;
        this.commands = List.copyOf(commands);
    }

    public boolean isEmpty() {
        return sound == null && commands.isEmpty();
    }

    /**
     * Plays the sound for the player and dispatches all configured commands.
     * {@code {player}} placeholders in command strings are replaced with the player's name.
     */
    public void execute(Player player) {
        if (sound != null) {
            player.playSound(sound);
        }
        for (RecipeCommand cmd : commands) {
            String resolved = cmd.command().replace("{player}", player.getName());
            if (cmd.asConsole()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            } else {
                player.performCommand(resolved);
            }
        }
    }
}
