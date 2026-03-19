package toutouchien.itemsadderadditions.components;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SimpleComponentProperty<T> extends ComponentProperty {
    @Override
    default void applyComponent(ConfigurationSection itemSection, String itemID, ItemStack itemStack) {

    }

    void applyComponent(T parameter, String itemID, ItemStack itemStack);
}
