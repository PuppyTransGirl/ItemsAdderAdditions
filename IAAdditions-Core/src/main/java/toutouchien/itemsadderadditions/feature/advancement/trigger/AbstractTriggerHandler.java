package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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

    @Nullable
    protected static String getIaId(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        CustomStack cs = CustomStack.byItemStack(item);
        return cs == null ? null : cs.getNamespacedID();
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
