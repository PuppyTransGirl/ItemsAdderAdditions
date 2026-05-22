package toutouchien.itemsadderadditions.feature.recipe;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeActionsTest {
    private static Sound sound() {
        return Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.MASTER, 1.0f, 1.0f);
    }

    @Test
    void empty_constant_isEmpty() {
        assertTrue(RecipeActions.EMPTY.isEmpty());
    }

    @Test
    void isEmpty_nullSoundEmptyCommands_returnsTrue() {
        assertTrue(new RecipeActions(null, List.of()).isEmpty());
    }

    @Test
    void isEmpty_withSound_returnsFalse() {
        assertFalse(new RecipeActions(sound(), List.of()).isEmpty());
    }

    @Test
    void isEmpty_withCommand_returnsFalse() {
        List<RecipeCommand> commands = List.of(new RecipeCommand("give {player} diamond 1", false));
        assertFalse(new RecipeActions(null, commands).isEmpty());
    }

    @Test
    void isEmpty_withSoundAndCommands_returnsFalse() {
        List<RecipeCommand> commands = List.of(new RecipeCommand("say hi", true));
        assertFalse(new RecipeActions(sound(), commands).isEmpty());
    }

    @Test
    void commands_areCopied() {
        // Mutations to the original list must not affect the stored commands.
        java.util.ArrayList<RecipeCommand> mutable = new java.util.ArrayList<>();
        mutable.add(new RecipeCommand("give {player} diamond 1", false));
        RecipeActions actions = new RecipeActions(null, mutable);
        assertFalse(actions.isEmpty());

        mutable.clear();
        assertFalse(actions.isEmpty());
    }
}
