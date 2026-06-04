package toutouchien.itemsadderadditions.feature.recipe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentsManager;

/**
 * Shared helpers and loading loop for all simple (non-crafting) recipe handlers.
 *
 * <h3>Shared load loop</h3>
 * Subclasses that follow the standard
 * <em>iterate -> enabled check -> resolve ingredient -> resolve result -> register</em>
 * pattern call {@link #load(String, ConfigurationSection)} which runs that loop
 * and delegates only the registration to {@link #registerRecipe}.
 *
 * <h3>Item resolution</h3>
 * {@link #resolveItem}, {@link #resolveResult}, and {@link #resolveIngredient} are
 * protected helpers for resolving and cloning ItemsAdder or vanilla items by their
 * config-written IDs.
 */
@NullMarked
public abstract class AbstractRecipeHandler {
    /**
     * Used in log messages to identify this handler type (e.g. {@code "CampfireRecipe"}).
     */
    protected final String logTag;
    private final @Nullable ComponentsManager componentsManager;

    /**
     * Total recipes successfully registered across all {@link #load} calls since the last {@link #resetCount()}.
     */
    private int loadedCount = 0;

    protected AbstractRecipeHandler(String logTag) {
        this(logTag, null);
    }

    protected AbstractRecipeHandler(String logTag, @Nullable ComponentsManager componentsManager) {
        this.logTag = logTag;
        this.componentsManager = componentsManager;
    }

    /**
     * Returns the number of recipes successfully registered since the last {@link #resetCount()}.
     */
    public int loadedCount() {
        return loadedCount;
    }

    /**
     * Resets the counter to zero.
     * Called by {@link toutouchien.itemsadderadditions.feature.recipe.RecipeLoader} before each reload cycle.
     */
    public void resetCount() {
        loadedCount = 0;
    }

    /**
     * Increments the loaded-recipe counter by one.
     * Must be called by {@link #registerRecipe} implementations upon successful NMS registration.
     */
    protected final void incrementCount() {
        loadedCount++;
    }

    /**
     * Iterates every recipe entry in {@code section}, skips disabled entries, and
     * calls {@link #loadEntry} for each valid one.
     *
     * @param namespace the namespace these recipes belong to (e.g. {@code "mypack"})
     * @param section   the YAML section to iterate; silently no-ops when {@code null}
     */
    public void load(String namespace, @Nullable ConfigurationSection section) {
        if (section == null) return;

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(recipeId);
            if (entry == null || !entry.getBoolean("enabled", true)) continue;

            loadEntry(namespace, recipeId, entry);
        }
    }

    /**
     * Parses one recipe entry and registers it.
     *
     * <p>The default implementation resolves {@code ingredient} and {@code result}
     * sections then delegates to {@link #registerRecipe}. Override for custom shapes.
     */
    protected void loadEntry(String namespace, String recipeId, ConfigurationSection entry) {
        ConfigurationSection ingredientSection = entry.getConfigurationSection("ingredient");
        if (ingredientSection == null) {
            Log.warn(logTag, "Missing 'ingredient' for {}:{}", namespace, recipeId);
            return;
        }

        ItemStack ingredient = resolveIngredient(namespace, recipeId, ingredientSection);
        if (ingredient == null) return;

        ConfigurationSection resultSection = entry.getConfigurationSection("result");
        if (resultSection == null) {
            Log.warn(logTag, "Missing 'result' for {}:{}", namespace, recipeId);
            return;
        }

        ItemStack result = resolveResult(namespace, recipeId, resultSection);
        if (result == null) return;

        registerRecipe(namespace, recipeId, entry, ingredient, result);
    }

    /**
     * Performs the handler-specific NMS registration for one resolved recipe.
     *
     * @param namespace  the recipe's namespace
     * @param recipeId   the recipe's local ID
     * @param entry      the full YAML entry (read extra fields like {@code cook_time} here)
     * @param ingredient the resolved ingredient item (already cloned)
     * @param result     the resolved result item (already cloned, amount applied)
     */
    protected abstract void registerRecipe(
            String namespace,
            String recipeId,
            ConfigurationSection entry,
            ItemStack ingredient,
            ItemStack result
    );

    /**
     * Clears all recipes registered by this handler.
     */
    public abstract void unregisterAll();

    /**
     * Resolves an item by its raw config-written ID and clones it.
     * Logs a warning and returns {@code null} when the item cannot be found.
     *
     * @param namespace the namespace to prepend when {@code itemValue} has no colon
     * @param recipeId  used in log messages
     * @param role      human-readable label (e.g. {@code "ingredient"}, {@code "result"})
     * @param itemValue the raw item ID from config, or {@code null}
     */
    @Nullable
    protected final ItemStack resolveItem(
            String namespace,
            String recipeId,
            String role,
            @Nullable String itemValue
    ) {
        if (itemValue == null) {
            Log.warn(logTag, "Missing '{}.item' for {}:{}", role, namespace, recipeId);
            return null;
        }

        ItemStack item = NamespaceUtils.itemByID(namespace, itemValue);
        if (item == null) {
            Log.warn(logTag, "Could not resolve {} item '{}' for {}:{}",
                    role, itemValue, namespace, recipeId);
            return null;
        }

        return item.clone();
    }

    /**
     * Resolves the {@code result} sub-section: resolves the item, applies optional
     * {@code components:} via the normal component system, then applies {@code amount}.
     */
    @Nullable
    protected final ItemStack resolveResult(
            String namespace,
            String recipeId,
            ConfigurationSection resultSection
    ) {
        ConfigurationSection actualResultSection = unwrapResultSection(namespace, recipeId, resultSection);
        if (actualResultSection == null) return null;

        ItemStack item = resolveItem(namespace, recipeId, "result", actualResultSection.getString("item"));
        if (item == null) return null;

        item = applyResultComponents(namespace, recipeId, item, actualResultSection);
        item.setAmount(actualResultSection.getInt("amount", 1));
        return item;
    }

    @Nullable
    protected final ConfigurationSection unwrapResultSection(
            String namespace,
            String recipeId,
            ConfigurationSection resultSection
    ) {
        if (resultSection.isString("item")) return resultSection;

        ConfigurationSection nested = null;
        for (String key : resultSection.getKeys(false)) {
            ConfigurationSection child = resultSection.getConfigurationSection(key);
            if (child == null || !child.isString("item")) continue;
            if (nested != null) {
                Log.warn(logTag,
                        "Result for {}:{} has multiple nested item sections; expected exactly one.",
                        namespace, recipeId);
                return null;
            }
            nested = child;
        }

        if (nested != null) return nested;

        Log.warn(logTag, "Missing 'result.item' for {}:{}", namespace, recipeId);
        return null;
    }

    protected final ItemStack applyResultComponents(
            String namespace,
            String recipeId,
            ItemStack item,
            ConfigurationSection resultSection
    ) {
        if (!resultSection.contains("components")) return item;

        ConfigurationSection componentsSection = resultSection.getConfigurationSection("components");
        if (componentsSection == null) {
            Log.warn(logTag,
                    "'result.components' for {}:{} must be a section/map - skipping components.",
                    namespace, recipeId);
            return item;
        }

        if (componentsManager == null) {
            Log.warn(logTag,
                    "'result.components' for {}:{} cannot be applied because the component system is unavailable.",
                    namespace, recipeId);
            return item;
        }

        return componentsManager.applyComponentsToStack(
                item,
                componentsSection,
                namespace + ":" + recipeId + " result"
        );
    }

    /**
     * Resolves the {@code ingredient} sub-section into a single item stack.
     */
    @Nullable
    protected final ItemStack resolveIngredient(
            String namespace,
            String recipeId,
            ConfigurationSection ingredientSection
    ) {
        return resolveItem(namespace, recipeId, "ingredient", ingredientSection.getString("item"));
    }
}
