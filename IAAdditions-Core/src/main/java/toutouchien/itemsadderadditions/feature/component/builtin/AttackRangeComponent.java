package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <p><strong>Minimum Minecraft Version:</strong> 1.21.11
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th><th>Range</th></tr>
 * <tr><td>min_reach</td><td>Float</td><td>0.0</td><td>0.0 - 64.0</td></tr>
 * <tr><td>max_reach</td><td>Float</td><td>3.0</td><td>0.0 - 64.0</td></tr>
 * <tr><td>min_creative_reach</td><td>Float</td><td>0.0</td><td>0.0 - 64.0</td></tr>
 * <tr><td>max_creative_reach</td><td>Float</td><td>5.0</td><td>0.0 - 64.0</td></tr>
 * <tr><td>hitbox_margin</td><td>Float</td><td>0.3</td><td>0.0 - 1.0</td></tr>
 * <tr><td>mob_factor</td><td>Float</td><td>1.0</td><td>0.0 - 2.0</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "attack_range")
public final class AttackRangeComponent extends ComponentExecutor {
    @Parameter(key = "min_reach", type = Float.class, min = 0F, max = 64F)
    private float minReach = 0F;

    @Parameter(key = "max_reach", type = Float.class, min = 0F, max = 64F)
    private float maxReach = 3F;

    @Parameter(key = "min_creative_reach", type = Float.class, min = 0F, max = 64F)
    private float minCreativeReach = 0F;

    @Parameter(key = "max_creative_reach", type = Float.class, min = 0F, max = 64F)
    private float maxCreativeReach = 5F;

    @Parameter(key = "hitbox_margin", type = Float.class, min = 0F, max = 1F)
    private float hitboxMargin = 0.3F;

    @Parameter(key = "mob_factor", type = Float.class, min = 0F, max = 2.0F)
    private float mobFactor = 1F;

    @Nullable
    @Override
    public VersionUtils minimumVersion() {
        return VersionUtils.v1_21_11;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        AttackRange attackRange = AttackRange.attackRange()
                .minReach(minReach)
                .maxReach(maxReach)
                .minCreativeReach(minCreativeReach)
                .maxCreativeReach(maxCreativeReach)
                .hitboxMargin(hitboxMargin)
                .mobFactor(mobFactor)
                .build();

        itemStack.setData(DataComponentTypes.ATTACK_RANGE, attackRange);
        return itemStack;
    }
}
