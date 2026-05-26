package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

@NullMarked
public abstract class AbstractTriggerHandler implements Listener {
    protected final AdvancementRegistry registry;

    protected AbstractTriggerHandler(AdvancementRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns a canonical item identifier for {@code item}:
     * <ul>
     *   <li>ItemsAdder items → {@code namespace:id} (e.g. {@code my_pack:ruby_sword})</li>
     *   <li>MMOItems items → {@code mmoitems:type:id} (e.g. {@code mmoitems:sword:fire_sword})</li>
     *   <li>Vanilla items → {@code minecraft:material_key} (e.g. {@code minecraft:diamond_sword})</li>
     * </ul>
     * Returns {@code null} for null or air items.
     */
    @Nullable
    protected static String getItemId(@Nullable ItemStack item) {
        return NamespaceUtils.itemID(item);
    }

    protected NamespacedKey advancementKeyFor(AdvancementCriterionDefinition criterion) {
        return registry.all().stream()
                .filter(def -> def.criteria().contains(criterion))
                .map(AdvancementDefinition::key)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Criterion " + criterion.name() + " has no owning advancement"));
    }

    protected void award(Player player, NamespacedKey advancementKey, String criterionName) {
        NmsManager.instance().handler().advancements().award(player, advancementKey, criterionName);
    }
}
