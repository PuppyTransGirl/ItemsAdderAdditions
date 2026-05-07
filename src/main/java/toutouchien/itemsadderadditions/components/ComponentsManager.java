package toutouchien.itemsadderadditions.components;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.components.annotations.Property;
import toutouchien.itemsadderadditions.components.properties.UseCooldownProperty;
import toutouchien.itemsadderadditions.utils.ReflectionUtils;
import toutouchien.itemsadderadditions.utils.other.ConfigUtils;
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.utils.other.ParameterInjector;

import java.util.List;

/**
 * Injects IAA-specific item components into ItemsAdder items via the
 * {@link ItemsAdder.Advanced#injectItemModifier} hook.
 *
 * <h3>Two kinds of component property</h3>
 * <dl>
 *   <dt>{@link SimpleComponentProperty}</dt>
 *   <dd>Reads a single top-level value (e.g. an integer or string) and applies it.
 *       The generic type parameter declares the expected value type; coercion from
 *       YAML number types is automatic.</dd>
 *
 *   <dt>{@link ComponentProperty} (non-simple)</dt>
 *   <dd>Reads a whole {@link ConfigurationSection} and injects {@code @Parameter}-
 *       annotated fields via {@link ParameterInjector} before calling
 *       {@link ComponentProperty#applyComponent}.</dd>
 * </dl>
 *
 * <h3>Adding a new property</h3>
 * <ol>
 *   <li>Implement {@link SimpleComponentProperty} or {@link ComponentProperty}.</li>
 *   <li>Annotate the class with {@link Property @Property(key = "your_yaml_key")}.</li>
 *   <li>Add an instance to {@link #PROPERTIES}.</li>
 * </ol>
 */
@NullMarked
public final class ComponentsManager {
    /**
     * All registered component properties.
     * These are prototype instances - they are <em>not</em> mutated during apply.
     */
    private static final List<ComponentProperty> PROPERTIES = List.of(
            new UseCooldownProperty()
    );

    /**
     * Applies one property to {@code itemStack} and returns the (possibly modified) stack.
     */
    private static ItemStack applyProperty(
            ComponentProperty property,
            CustomStack customStack,
            ItemStack itemStack
    ) {
        Property annotation = property.getClass().getAnnotation(Property.class);
        if (annotation == null) {
            Log.warn("Components", "Missing @Property annotation on: {}",
                    property.getClass().getName());
            return itemStack;
        }

        FileConfiguration config = customStack.getConfig();
        ConfigurationSection itemSection =
                config.getConfigurationSection("items." + customStack.getId());

        if (itemSection == null || !itemSection.contains(annotation.key()))
            return itemStack; // This item doesn't use this property - skip.

        if (property instanceof SimpleComponentProperty<?> simple) {
            return applySimpleProperty(simple, annotation, itemSection, customStack, itemStack);
        }

        // Section-based property: inject @Parameter fields, then apply.
        ConfigurationSection propertySection = itemSection.getConfigurationSection(annotation.key());
        if (propertySection == null) return itemStack;

        if (ParameterInjector.inject(property, propertySection, customStack.getNamespacedID())) {
            property.applyComponent(propertySection, customStack.getId(), itemStack);
        }

        return itemStack;
    }

    /**
     * Reads a single YAML value, coerces its numeric type if necessary, and
     * delegates to {@link SimpleComponentProperty#applyComponent}.
     */
    @SuppressWarnings("unchecked")
    private static ItemStack applySimpleProperty(
            SimpleComponentProperty<?> property,
            Property annotation,
            ConfigurationSection itemSection,
            CustomStack customStack,
            ItemStack itemStack
    ) {
        @Nullable Class<?> expectedType = ReflectionUtils.getTypeArgument(property.getClass());
        @Nullable Object value = itemSection.get(annotation.key());

        if (expectedType != null) {
            value = ConfigUtils.coerceNumber(value, expectedType);
        }

        if (expectedType == null || !expectedType.isInstance(value)) {
            if (value != null && expectedType != null) {
                Log.itemWarn("Components", customStack.getNamespacedID(),
                        "property '{}' has wrong type: expected {}, got {}",
                        annotation.key(), expectedType.getSimpleName(),
                        value.getClass().getSimpleName());
            }
            return itemStack;
        }

        // Safe: we just checked expectedType.isInstance(value).
        ((SimpleComponentProperty<Object>) property)
                .applyComponent(value, customStack.getId(), itemStack);

        return itemStack;
    }

    /**
     * Registers the item modifier with ItemsAdder so it is called for every item
     * at load time. Safe to call multiple times (IAA handles deduplication).
     */
    public void applyComponents() {
/*        ItemsAdder.Advanced.ModifierHandler modifier = (namespacedID, itemStack) -> {
            CustomStack customStack = CustomStack.byItemStack(itemStack);
            if (customStack == null) return itemStack;

            for (ComponentProperty property : PROPERTIES) {
                itemStack = applyProperty(property, customStack, itemStack);
            }

            return itemStack;
        };

        ItemsAdder.Advanced.injectItemModifier(ItemsAdderAdditions.instance(), modifier);*/
    }
}
