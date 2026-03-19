package toutouchien.itemsadderadditions.components.properties;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.components.ComponentProperty;
import toutouchien.itemsadderadditions.components.annotations.Property;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@Property(key = "use_cooldown")
public class UseCooldownProperty implements ComponentProperty {
    @Parameter(key = "cooldown_seconds", type = Float.class, required = true)
    private Float cooldownSeconds = 1F;

    @Nullable
    @Parameter(key = "group", type = String.class)
    private String groupString;

    @Override
    public void applyComponent(ConfigurationSection itemSection, String itemID, ItemStack itemStack) {
        Key group = groupString == null
                ? Key.key("itemsadder", itemSection.getName())
                : Key.key(groupString);

        itemStack.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(cooldownSeconds).cooldownGroup(group));
    }
}
