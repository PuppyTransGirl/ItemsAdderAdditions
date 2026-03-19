package toutouchien.itemsadderadditions.components;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.utils.ParameterInjector;
import toutouchien.itemsadderadditions.components.annotations.Property;
import toutouchien.itemsadderadditions.utils.ConfigUtils;
import toutouchien.itemsadderadditions.utils.ReflectionUtils;

import java.util.List;

@NullMarked
public class ComponentsManager {
    private static final List<ComponentProperty> PROPERTY_INSTANCES = List.of(
            new toutouchien.itemsadderadditions.components.properties.UseCooldownProperty()
    );

    public void applyComponents() {
        ItemsAdder.Advanced.ModifierHandler modifier = (namespacedID, itemStack) -> {
            CustomStack customStack = CustomStack.byItemStack(itemStack);
            if (customStack == null)
                return itemStack;

            for (ComponentProperty componentProperty : PROPERTY_INSTANCES) {
                Property property = componentProperty.getClass().getAnnotation(Property.class);
                if (property == null) {
                    ItemsAdderAdditions.instance().getSLF4JLogger().warn(
                            "[Components] Missing @Property annotation on: {}",
                            componentProperty.getClass().getName()
                    );
                    continue;
                }

                FileConfiguration config = customStack.getConfig();
                ConfigurationSection itemSection = config.getConfigurationSection("items." + customStack.getId());
                if (itemSection == null || !itemSection.contains(property.key()))
                    continue;

                if (componentProperty instanceof SimpleComponentProperty<?> simpleProperty) {
                    Object value = itemSection.get(property.key());
                    Class<?> expectedType = ReflectionUtils.getTypeArgument(simpleProperty.getClass());

                    if (expectedType != null)
                        value = ConfigUtils.coerceNumber(value, expectedType);

                    if (expectedType != null && expectedType.isInstance(value)) {
                        @SuppressWarnings("unchecked")
                        SimpleComponentProperty<Object> cast = (SimpleComponentProperty<Object>) simpleProperty;
                        cast.applyComponent(value, customStack.getId(), itemStack);
                    } else if (value != null && expectedType != null) {
                        ItemsAdderAdditions.instance().getSLF4JLogger().warn(
                                "[Components] Invalid type for property '{}'. Expected {}, got {} for item {}",
                                property.key(), expectedType.getSimpleName(),
                                value.getClass().getSimpleName(), customStack.getNamespacedID()
                        );
                    }
                    continue;
                }

                ConfigurationSection propertySection = itemSection.getConfigurationSection(property.key());
                if (propertySection == null)
                    continue;

                if (ParameterInjector.inject(componentProperty, propertySection, customStack.getNamespacedID()))
                    componentProperty.applyComponent(propertySection, customStack.getId(), itemStack);
            }

            return itemStack;
        };

        ItemsAdder.Advanced.injectItemModifier(ItemsAdderAdditions.instance(), modifier);
    }
}
