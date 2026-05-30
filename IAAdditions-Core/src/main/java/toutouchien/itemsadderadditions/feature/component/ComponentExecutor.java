package toutouchien.itemsadderadditions.feature.component;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.inject.ParameterInjector;
import toutouchien.itemsadderadditions.common.registry.Keyed;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * Base class for all item component executors.
 *
 * <h2>What is a component?</h2>
 * A component applies a Minecraft data component to a custom item's {@link ItemStack}
 * at load time via the ItemsAdder item modifier hook. Unlike behaviours and actions,
 * components are applied once during reload and reflected on every item stack built
 * after that point.
 *
 * <h2>Implementing a component</h2>
 * <ol>
 *   <li>Annotate the subclass with {@link Component @Component(key = "your_key")}.</li>
 *   <li>Declare a no-arg constructor (required by {@link #newInstance()}).</li>
 *   <li>Add {@link toutouchien.itemsadderadditions.common.annotation.Parameter @Parameter}-annotated
 *       fields for any YAML parameters the component needs.</li>
 *   <li>Override {@link #minimumVersion()} if the component requires a specific
 *       Minecraft version (it will be silently skipped on older servers).</li>
 *   <li>Implement {@link #apply(ItemStack, String)} with the component-specific logic.</li>
 * </ol>
 *
 * <h2>YAML structure</h2>
 * <pre>
 * items:
 *   my_item:
 *     components:
 *       your_key:          # matches @Component(key = ...)
 *         some_param: 42   # injected into @Parameter fields
 * </pre>
 *
 * <p>Simple single-value components (like {@code rarity: RARE}) override
 * {@link #configure(Object, String)} directly instead of relying on @Parameter injection.
 */
@NullMarked
public abstract class ComponentExecutor implements Keyed {
    /**
     * {@inheritDoc} Returns the YAML key from the {@link Component} annotation.
     */
    @Override
    public final String key() {
        return annotation().key();
    }

    /**
     * Creates a fresh, injectable instance via no-arg reflection.
     * Each item gets its own copy so field injection is isolated.
     *
     * @throws IllegalStateException if the subclass has no no-arg constructor
     */
    public final ComponentExecutor newInstance() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "ComponentExecutor subclass must expose a no-arg constructor: " + getClass().getName(), e);
        }
    }

    /**
     * Reads YAML configuration into this executor's {@link toutouchien.itemsadderadditions.common.annotation.Parameter}-annotated fields.
     *
     * <p>The default implementation delegates to {@link ParameterInjector} when
     * {@code configData} is a {@link ConfigurationSection}. Subclasses that accept
     * a simple scalar value (string, integer, etc.) should override this method.
     *
     * @param configData   the raw YAML value for this component's key
     * @param namespacedID the item's namespaced ID; used in log messages only
     * @return {@code true} if configuration succeeded and this executor should be applied
     */
    public boolean configure(@Nullable Object configData, String namespacedID) {
        ConfigurationSection section = configData instanceof ConfigurationSection cs ? cs : null;
        return ParameterInjector.inject(this, section, namespacedID);
    }

    /**
     * The minimum Minecraft version required for this component to function.
     *
     * <p>Return {@code null} (default) to allow the component on all supported versions.
     * When a version is declared and the current server is older, the component is
     * skipped with a warning during reload.
     */
    @Nullable
    public VersionUtils minimumVersion() {
        return null;
    }

    /**
     * Returns {@code true} when the current server version meets this component's
     * {@link #minimumVersion()} requirement.
     */
    public final boolean isSupportedOnCurrentVersion() {
        VersionUtils min = minimumVersion();
        return min == null || VersionUtils.isHigherThanOrEquals(min);
    }

    /**
     * Applies this component to the given item stack and returns the (possibly modified) stack.
     *
     * <p>Called once per item per reload cycle, not on every item retrieval.
     *
     * @param itemStack    the item stack to modify
     * @param namespacedID the item's namespaced ID
     * @return the modified item stack
     */
    public abstract ItemStack apply(ItemStack itemStack, String namespacedID);

    private Component annotation() {
        Component c = getClass().getAnnotation(Component.class);
        if (c == null)
            throw new IllegalStateException("Missing @Component annotation on: " + getClass().getName());
        return c;
    }
}
