package toutouchien.itemsadderadditions.feature.recipe.brewing;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record BrewingRecipeMatch(BrewingRecipeData recipe, boolean[] slots, int matchedSlots) {
    public boolean matchesSlot(int slot) {
        return slot >= 0 && slot < slots.length && slots[slot];
    }
}
