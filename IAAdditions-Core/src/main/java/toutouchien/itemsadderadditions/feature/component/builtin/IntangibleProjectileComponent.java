package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "intangible_projectile")
public final class IntangibleProjectileComponent extends ComponentExecutor {

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (configData instanceof Boolean b) return b;
        return configData != null;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.INTANGIBLE_PROJECTILE);
        return itemStack;
    }
}
