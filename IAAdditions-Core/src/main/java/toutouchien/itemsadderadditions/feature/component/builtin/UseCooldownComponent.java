package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "use_cooldown")
public final class UseCooldownComponent extends ComponentExecutor {
    @Nullable
    @Parameter(key = "cooldown", type = Float.class, required = true)
    private Float cooldown;

    @Nullable
    @Parameter(key = "group", type = String.class)
    private String group;

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        String itemId = namespacedID.contains(":") ? namespacedID.substring(namespacedID.indexOf(':') + 1) : namespacedID;
        Key cooldownGroup = group == null
                ? Key.key("itemsadder", itemId)
                : Key.key(group);

        itemStack.setData(DataComponentTypes.USE_COOLDOWN,
                UseCooldown.useCooldown(cooldown).cooldownGroup(cooldownGroup));
        return itemStack;
    }
}
