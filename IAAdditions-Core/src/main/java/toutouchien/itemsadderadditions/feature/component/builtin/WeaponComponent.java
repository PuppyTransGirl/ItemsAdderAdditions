package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Weapon;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <p><strong>Minimum Minecraft Version:</strong> 1.21.5
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>item_damage_per_attack</td><td>Integer</td><td>1</td></tr>
 * <tr><td>disable_blocking_for_seconds</td><td>Float</td><td>0.0</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "weapon")
public final class WeaponComponent extends ComponentExecutor {
    @Parameter(key = "item_damage_per_attack", type = Integer.class, min = 0, max = Double.POSITIVE_INFINITY)
    private int itemDamagePerAttack = 1;

    @Parameter(key = "disable_blocking_for_seconds", type = Float.class, min = 0, max = Double.POSITIVE_INFINITY)
    private float disableBlockingForSeconds = 0f;

    @Override
    public @Nullable VersionUtils minimumVersion() {
        return VersionUtils.v1_21_5;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.WEAPON, Weapon.weapon()
                .itemDamagePerAttack(itemDamagePerAttack)
                .disableBlockingForSeconds(disableBlockingForSeconds)
                .build());
        return itemStack;
    }
}
