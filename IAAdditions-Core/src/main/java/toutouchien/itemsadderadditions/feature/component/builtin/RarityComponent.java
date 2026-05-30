package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "rarity")
public final class RarityComponent extends ComponentExecutor {
    @Nullable
    private ItemRarity rarity;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'rarity' must be a string (COMMON, UNCOMMON, RARE, EPIC)");
            return false;
        }

        try {
            this.rarity = ItemRarity.valueOf(raw.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            Log.itemWarn("Components", namespacedID,
                    "'rarity' value '{}' is not valid. Use COMMON, UNCOMMON, RARE, or EPIC.", raw);
            return false;
        }
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (rarity != null) {
            itemStack.setData(DataComponentTypes.RARITY, rarity);
        }
        return itemStack;
    }
}
