package toutouchien.itemsadderadditions.feature.component;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ComponentProperty {
    void applyComponent(ConfigurationSection itemSection, String itemID, ItemStack itemStack);
}
