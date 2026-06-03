package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
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

    protected static boolean matchesItem(@Nullable ItemStack item, @Nullable String expectedIdOrTag) {
        return expectedIdOrTag == null || NamespaceUtils.matchesItemIDOrTag(item, expectedIdOrTag);
    }

    protected static boolean matchesBlock(Block block, @Nullable String expectedIdOrTag) {
        return expectedIdOrTag == null || NamespaceUtils.matchesBlockIDOrTag(block, expectedIdOrTag);
    }

    protected static boolean matchesContentId(String actualId, String expectedId) {
        return NamespaceUtils.matchesContentIDOrTag(actualId, expectedId, CustomTagType.BLOCK);
    }

    protected static boolean matchesFurniture(String actualId, String expectedIdOrTag) {
        return NamespaceUtils.matchesFurnitureIDOrTag(actualId, expectedIdOrTag);
    }

    protected static boolean matchesRecipe(String actualRecipeKey, String expectedIdOrTag) {
        return NamespaceUtils.matchesRecipeIDOrTag(actualRecipeKey, expectedIdOrTag);
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
        AdvancementDefinition advancement = registry.get(advancementKey);
        if (advancement != null) {
            AdvancementCriterionDefinition criterion = advancement.criteria().stream()
                    .filter(c -> c.name().equals(criterionName))
                    .findFirst()
                    .orElse(null);
            if (criterion != null && !criterion.playerPredicate().matches(player)) return;
        }

        NmsManager.instance().handler().advancements().award(player, advancementKey, criterionName);
    }
}
