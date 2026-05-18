package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface StorageInventorySpec {
    record Typed(InventoryType inventoryType) implements StorageInventorySpec {}

    @SuppressWarnings("UnstableApiUsage")
    record Menu(MenuType.Typed<? extends InventoryView, ?> menuType) implements StorageInventorySpec {}
}
