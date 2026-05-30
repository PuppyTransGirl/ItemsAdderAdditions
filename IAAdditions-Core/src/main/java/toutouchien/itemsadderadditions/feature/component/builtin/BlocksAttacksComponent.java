package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Makes the item block incoming attacks, like a shield.
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>block_delay_seconds</td><td>Float</td><td>0.0</td></tr>
 * <tr><td>disable_cooldown_scale</td><td>Float</td><td>1.0</td></tr>
 * <tr><td>bypassed_by</td><td>String</td><td>null (damage type tag, e.g. #minecraft:bypasses_shield)</td></tr>
 * <tr><td>block_sound</td><td>String</td><td>null (sound key)</td></tr>
 * <tr><td>disable_sound</td><td>String</td><td>null (sound key)</td></tr>
 * <tr><td>damage_reductions</td><td>List of sections</td><td>[]</td></tr>
 * <tr><td>item_damage</td><td>Section</td><td>null (threshold, base, factor)</td></tr>
 * </table>
 * <p>
 * damage_reductions entry:
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>type</td><td>String or List</td><td>null (damage type tag/IDs; null = all types)</td></tr>
 * <tr><td>horizontal_blocking_angle</td><td>Float</td><td>90.0</td></tr>
 * <tr><td>base</td><td>Float</td><td>0.0</td></tr>
 * <tr><td>factor</td><td>Float</td><td>1.0</td></tr>
 * </table>
 * <p>
 * item_damage section:
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>threshold</td><td>Float</td><td>0.0</td></tr>
 * <tr><td>base</td><td>Float</td><td>0.0</td></tr>
 * <tr><td>factor</td><td>Float</td><td>1.0</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "blocks_attacks")
public final class BlocksAttacksComponent extends ComponentExecutor {
    private float blockDelaySeconds = 0f;
    private float disableCooldownScale = 1f;
    private List<DamageReduction> damageReductions = List.of();
    private @Nullable ItemDamageFunction itemDamage;
    private @Nullable TagKey<DamageType> bypassedBy;
    private @Nullable Key blockSound;
    private @Nullable Key disableSound;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'blocks_attacks' must be a configuration section");
            return false;
        }

        Object rawDelay = section.get("block_delay_seconds");
        if (rawDelay instanceof Number n) this.blockDelaySeconds = n.floatValue();

        Object rawScale = section.get("disable_cooldown_scale");
        if (rawScale instanceof Number n) this.disableCooldownScale = n.floatValue();

        String rawBypassed = section.getString("bypassed_by");
        if (rawBypassed != null) {
            String bk = rawBypassed.trim().toLowerCase(Locale.ROOT);
            if (bk.startsWith("#")) bk = bk.substring(1);
            if (!bk.contains(":")) bk = "minecraft:" + bk;
            this.bypassedBy = TagKey.create(RegistryKey.DAMAGE_TYPE, Key.key(bk));
        }

        String rawBlockSound = section.getString("block_sound");
        if (rawBlockSound != null) {
            String sk = rawBlockSound.trim().toLowerCase(Locale.ROOT);
            if (!sk.contains(":")) sk = "minecraft:" + sk;
            this.blockSound = Key.key(sk);
        }

        String rawDisableSound = section.getString("disable_sound");
        if (rawDisableSound != null) {
            String sk = rawDisableSound.trim().toLowerCase(Locale.ROOT);
            if (!sk.contains(":")) sk = "minecraft:" + sk;
            this.disableSound = Key.key(sk);
        }

        List<?> rawReductions = section.getList("damage_reductions");
        if (rawReductions != null) {
            List<DamageReduction> parsed = new ArrayList<>();
            for (Object entry : rawReductions) {
                if (!(entry instanceof ConfigurationSection rs)) continue;
                DamageReduction reduction = parseDamageReduction(rs, namespacedID);
                if (reduction == null) return false;
                parsed.add(reduction);
            }
            this.damageReductions = List.copyOf(parsed);
        }

        ConfigurationSection itemDamageSection = section.getConfigurationSection("item_damage");
        if (itemDamageSection != null) {
            this.itemDamage = parseItemDamage(itemDamageSection);
        }

        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        BlocksAttacks.Builder builder = BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(blockDelaySeconds)
                .disableCooldownScale(disableCooldownScale)
                .bypassedBy(bypassedBy)
                .blockSound(blockSound)
                .disableSound(disableSound);
        if (!damageReductions.isEmpty()) builder.damageReductions(damageReductions);
        if (itemDamage != null) builder.itemDamage(itemDamage);
        itemStack.setData(DataComponentTypes.BLOCKS_ATTACKS, builder.build());
        return itemStack;
    }

    private @Nullable DamageReduction parseDamageReduction(ConfigurationSection section, String namespacedID) {
        DamageReduction.Builder builder = DamageReduction.damageReduction()
                .horizontalBlockingAngle(90f)
                .base(0f)
                .factor(1f);

        Object rawType = section.get("type");
        if (rawType != null) {
            RegistryKeySet<DamageType> typeSet = parseDamageTypeSet(rawType, namespacedID);
            if (typeSet == null) return null;
            builder.type(typeSet);
        }

        Object rawAngle = section.get("horizontal_blocking_angle");
        if (rawAngle instanceof Number n) builder.horizontalBlockingAngle(n.floatValue());

        Object rawBase = section.get("base");
        if (rawBase instanceof Number n) builder.base(n.floatValue());

        Object rawFactor = section.get("factor");
        if (rawFactor instanceof Number n) builder.factor(n.floatValue());

        return builder.build();
    }

    private ItemDamageFunction parseItemDamage(ConfigurationSection section) {
        ItemDamageFunction.Builder builder = ItemDamageFunction.itemDamageFunction()
                .threshold(0f)
                .base(0f)
                .factor(1f);

        Object rawThreshold = section.get("threshold");
        if (rawThreshold instanceof Number n) builder.threshold(n.floatValue());

        Object rawBase = section.get("base");
        if (rawBase instanceof Number n) builder.base(n.floatValue());

        Object rawFactor = section.get("factor");
        if (rawFactor instanceof Number n) builder.factor(n.floatValue());

        return builder.build();
    }

    private @Nullable RegistryKeySet<DamageType> parseDamageTypeSet(Object raw, String namespacedID) {
        if (raw instanceof String s) {
            String key = s.trim().toLowerCase(Locale.ROOT);
            if (key.startsWith("#")) {
                String tagKey = key.substring(1);
                if (!tagKey.contains(":")) tagKey = "minecraft:" + tagKey;
                RegistryKeySet<DamageType> tag = Registry.DAMAGE_TYPE.getTag(
                        TagKey.create(RegistryKey.DAMAGE_TYPE, Key.key(tagKey)));
                if (tag == null) {
                    Log.itemWarn("Components", namespacedID, "'blocks_attacks' damage type tag '{}' not found.", s);
                    return null;
                }
                return tag;
            }
            if (!key.contains(":")) key = "minecraft:" + key;
            return RegistrySet.keySet(RegistryKey.DAMAGE_TYPE, TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(key)));
        }
        if (raw instanceof List<?> list) {
            List<TypedKey<DamageType>> keys = new ArrayList<>();
            for (Object entry : list) {
                if (!(entry instanceof String s)) continue;
                String k = s.trim().toLowerCase(Locale.ROOT);
                if (!k.contains(":")) k = "minecraft:" + k;
                keys.add(TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(k)));
            }
            if (keys.isEmpty()) {
                Log.itemWarn("Components", namespacedID, "'blocks_attacks' damage_reduction 'type' list must not be empty");
                return null;
            }
            return RegistrySet.keySet(RegistryKey.DAMAGE_TYPE, keys);
        }
        Log.itemWarn("Components", namespacedID, "'blocks_attacks' damage_reduction 'type' must be a tag or list of damage type IDs");
        return null;
    }
}
