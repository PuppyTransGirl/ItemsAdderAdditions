package toutouchien.itemsadderadditions.feature.component;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInComponentsTest {
    @Test
    void createReturnsAllBuiltInComponentKeys() {
        List<ComponentExecutor> components = BuiltInComponents.create();
        Set<String> keys = components.stream().map(ComponentExecutor::key).collect(Collectors.toSet());

        assertEquals(42, components.size());
        assertEquals(Set.of(
                "attack_range",
                "banner_patterns",
                "base_color",
                "blocks_attacks",
                "bundle_contents",
                "can_break",
                "can_place_on",
                "charged_projectiles",
                "damage_resistant",
                "damage_type",
                "death_protection",
                "dyed_color",
                "enchantable",
                "firework_explosion",
                "fireworks",
                "glider",
                "intangible_projectile",
                "kinetic_weapon",
                "lodestone_tracker",
                "map_color",
                "map_decorations",
                "map_id",
                "minimum_attack_charge",
                "ominous_bottle_amplifier",
                "piercing_weapon",
                "pot_decorations",
                "potion_contents",
                "potion_duration_scale",
                "profile",
                "provides_banner_patterns",
                "provides_trim_material",
                "rarity",
                "repairable",
                "stored_enchantments",
                "suspicious_stew_effects",
                "swing_animation",
                "tool",
                "tooltip_display",
                "use_cooldown",
                "use_remainder",
                "weapon",
                "writable_book_content"
        ), keys);
    }

    @Test
    void createReturnsNoDuplicateKeys() {
        List<ComponentExecutor> components = BuiltInComponents.create();
        long distinct = components.stream().map(ComponentExecutor::key).distinct().count();

        assertEquals(components.size(), distinct);
    }

    @Test
    void everyComponentCarriesTheComponentAnnotation() {
        for (ComponentExecutor component : BuiltInComponents.create()) {
            assertNotNull(component.getClass().getAnnotation(Component.class),
                    component.getClass().getSimpleName() + " is missing @Component");
        }
    }

    @Test
    void createReturnsFreshInstancesEachTime() {
        List<ComponentExecutor> first = BuiltInComponents.create();
        List<ComponentExecutor> second = BuiltInComponents.create();

        assertEquals(first.stream().map(ComponentExecutor::key).toList(),
                second.stream().map(ComponentExecutor::key).toList());
        for (int i = 0; i < first.size(); i++) {
            assertNotSame(first.get(i), second.get(i));
            assertEquals(first.get(i).getClass(), second.get(i).getClass());
        }
    }

    @Test
    void returnedListIsImmutable() {
        List<ComponentExecutor> components = BuiltInComponents.create();

        assertThrows(UnsupportedOperationException.class, () -> components.add(components.getFirst()));
    }
}
