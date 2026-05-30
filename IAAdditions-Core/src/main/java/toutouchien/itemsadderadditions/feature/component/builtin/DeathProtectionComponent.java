package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.utils.PotionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prevents death, optionally applying effects when triggered.
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>death_effects</td><td>List of sections</td><td>[] (optional; omit for a bare protection totem)</td></tr>
 * </table>
 * <p>
 * death_effects entry types:
 * <pre>
 * - type: apply_effects
 *   probability: 1.0
 *   effects:
 *     - type: regeneration
 *       duration: 900
 *       amplifier: 1
 * - type: remove_effects
 *   effects: [wither, poison]
 * - type: clear_all_effects
 * - type: teleport_randomly
 *   diameter: 16.0
 * - type: play_sound
 *   sound: minecraft:item.totem.use
 * </pre>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "death_protection")
public final class DeathProtectionComponent extends ComponentExecutor {
    private List<ConsumeEffect> deathEffects = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'death_protection' must be a configuration section");
            return false;
        }

        List<?> rawEffects = section.getList("death_effects");
        if (rawEffects != null) {
            List<ConsumeEffect> parsed = new ArrayList<>();
            for (Object entry : rawEffects) {
                if (!(entry instanceof ConfigurationSection effectSection)) continue;
                ConsumeEffect effect = parseConsumeEffect(effectSection, namespacedID);
                if (effect == null) return false;
                parsed.add(effect);
            }
            this.deathEffects = List.copyOf(parsed);
        }

        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection(deathEffects));
        return itemStack;
    }

    static @Nullable ConsumeEffect parseConsumeEffect(ConfigurationSection section, String namespacedID) {
        String type = section.getString("type", "");
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "apply_effects" -> parseApplyEffects(section, namespacedID);
            case "remove_effects" -> parseRemoveEffects(section, namespacedID);
            case "clear_all_effects" -> ConsumeEffect.clearAllStatusEffects();
            case "teleport_randomly" -> {
                float diameter = (float) section.getDouble("diameter", 16.0);
                yield ConsumeEffect.teleportRandomlyEffect(diameter);
            }
            case "play_sound" -> {
                String raw = section.getString("sound");
                if (raw == null || raw.isBlank()) {
                    Log.itemWarn("Components", namespacedID, "'death_protection' play_sound effect is missing 'sound'");
                    yield null;
                }
                String sk = raw.trim().toLowerCase(Locale.ROOT);
                if (!sk.contains(":")) sk = "minecraft:" + sk;
                yield ConsumeEffect.playSoundConsumeEffect(Key.key(sk));
            }
            default -> {
                Log.itemWarn("Components", namespacedID,
                        "'death_protection' unknown effect type '{}'. Valid: apply_effects, remove_effects, clear_all_effects, teleport_randomly, play_sound", type);
                yield null;
            }
        };
    }

    private static @Nullable ConsumeEffect parseApplyEffects(ConfigurationSection section, String namespacedID) {
        List<?> rawEffects = section.getList("effects");
        if (rawEffects == null || rawEffects.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'death_protection' apply_effects entry is missing 'effects' list");
            return null;
        }
        List<PotionEffect> effects = new ArrayList<>();
        for (Object entry : rawEffects) {
            if (!(entry instanceof ConfigurationSection effectSection)) continue;
            PotionEffect effect = PotionUtils.parsePotion(effectSection);
            if (effect == null) {
                Log.itemWarn("Components", namespacedID, "'death_protection' apply_effects contains an invalid potion effect entry");
                return null;
            }
            effects.add(effect);
        }
        if (effects.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'death_protection' apply_effects list must not be empty");
            return null;
        }
        float probability = (float) section.getDouble("probability", 1.0);
        return ConsumeEffect.applyStatusEffects(effects, probability);
    }

    private static @Nullable ConsumeEffect parseRemoveEffects(ConfigurationSection section, String namespacedID) {
        List<?> rawList = section.getList("effects");
        if (rawList == null || rawList.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'death_protection' remove_effects entry is missing 'effects' list");
            return null;
        }
        List<TypedKey<PotionEffectType>> keys = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof String s)) continue;
            String k = s.trim().toLowerCase(Locale.ROOT);
            if (!k.contains(":")) k = "minecraft:" + k;
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(Key.key(k));
            if (type == null) {
                Log.itemWarn("Components", namespacedID, "'death_protection' remove_effects unknown effect type '{}'", s);
                return null;
            }
            keys.add(TypedKey.create(RegistryKey.MOB_EFFECT, Key.key(k)));
        }
        if (keys.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'death_protection' remove_effects list must not be empty");
            return null;
        }
        return ConsumeEffect.removeEffects(RegistrySet.keySet(RegistryKey.MOB_EFFECT, keys));
    }
}
