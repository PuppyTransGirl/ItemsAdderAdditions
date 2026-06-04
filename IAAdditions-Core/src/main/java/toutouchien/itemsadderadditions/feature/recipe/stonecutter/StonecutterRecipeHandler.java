package toutouchien.itemsadderadditions.feature.recipe.stonecutter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.component.ComponentsManager;
import toutouchien.itemsadderadditions.feature.recipe.AbstractRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActionsParser;
import toutouchien.itemsadderadditions.integration.bridge.StonecutterPatchBridge;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads and registers custom stonecutter recipes.
 *
 * <h3>YAML structure</h3>
 * <pre>{@code
 * recipes:
 *   stonecutter:
 *     my_recipe:
 *       enabled: true          # optional, default: true
 *       ingredient:
 *         item: "mypack:raw_stone"
 *       result:
 *         item: "mypack:polished_stone"
 *         amount: 1            # optional, default: 1
 *       on_complete:           # optional
 *         sound:
 *           name: "minecraft:block.anvil.use"
 *         commands:
 *           reward:
 *             command:    'give {player} diamond 1'
 *             as_console: true
 * }</pre>
 */
@NullMarked
public final class StonecutterRecipeHandler extends AbstractRecipeHandler {
    private final List<StonecutterEntry> entries = new ArrayList<>();

    public StonecutterRecipeHandler() {
        this(null);
    }

    public StonecutterRecipeHandler(@Nullable ComponentsManager componentsManager) {
        super("StonecutterRecipe", componentsManager);
    }

    @Override
    protected void registerRecipe(
            String namespace,
            String recipeId,
            ConfigurationSection entry,
            ItemStack ingredient,
            ItemStack result
    ) {
        NmsManager.instance()
                .handler()
                .stonecutterRecipes()
                .register(namespace, recipeId, ingredient, result);

        StonecutterPatchBridge.register(ingredient, result);

        RecipeActions actions = RecipeActionsParser.parse(entry.getConfigurationSection("on_complete"));
        if (!actions.isEmpty()) {
            entries.add(new StonecutterEntry(ingredient.clone(), result.clone(), actions));
        }

        incrementCount();
    }

    /**
     * Finds the first entry whose ingredient and result both match the given items.
     *
     * @return matching actions, or {@code null} if none
     */
    @Nullable
    public RecipeActions actionsFor(ItemStack ingredient, ItemStack result) {
        for (StonecutterEntry e : entries) {
            if (e.ingredient().isSimilar(ingredient) && e.result().isSimilar(result)) {
                return e.actions();
            }
        }
        return null;
    }

    @Override
    public void unregisterAll() {
        NmsManager.instance().handler().stonecutterRecipes().unregisterAll();
        StonecutterPatchBridge.clear();
        entries.clear();
    }

    /**
     * Per-recipe data needed by {@link StonecutterRecipeListener} at event time.
     */
    public record StonecutterEntry(ItemStack ingredient, ItemStack result, RecipeActions actions) {}
}
