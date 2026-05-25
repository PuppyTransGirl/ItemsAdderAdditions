package toutouchien.itemsadderadditions.feature.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeCommandTest {
    @Test
    void recordStoresCommandAndConsoleFlag() {
        RecipeCommand command = new RecipeCommand("give {player} diamond 1", true);

        assertEquals("give {player} diamond 1", command.command());
        assertTrue(command.asConsole());
    }

    @Test
    void equalCommandsHaveSameHashCode() {
        RecipeCommand first = new RecipeCommand("say hi", false);
        RecipeCommand second = new RecipeCommand("say hi", false);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.toString().contains("say hi"));
    }
}
