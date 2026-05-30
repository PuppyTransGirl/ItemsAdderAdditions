package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.datacomponent.item.Tool.Rule;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;
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
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>default_mining_speed</td><td>Float</td><td>1.0</td></tr>
 * <tr><td>damage_per_block</td><td>Integer</td><td>1</td></tr>
 * <tr><td>can_destroy_in_creative</td><td>Boolean</td><td>true</td></tr>
 * <tr><td>rules</td><td>List of sections</td><td>[]</td></tr>
 * </table>
 * <p>
 * Rule section:
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>blocks</td><td>String or List&lt;String&gt;</td><td>required (block IDs or #tag)</td></tr>
 * <tr><td>speed</td><td>Float</td><td>null (no override)</td></tr>
 * <tr><td>correct_for_drops</td><td>Boolean or null</td><td>null (NOT_SET)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "tool")
public final class ToolComponent extends ComponentExecutor {
    private float defaultMiningSpeed = 1f;
    private int damagePerBlock = 1;
    private boolean canDestroyInCreative = true;
    private List<Tool.Rule> rules = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'tool' must be a configuration section");
            return false;
        }
        Object rawSpeed = section.get("default_mining_speed");
        if (rawSpeed instanceof Number n) this.defaultMiningSpeed = n.floatValue();

        this.damagePerBlock = section.getInt("damage_per_block", 1);
        this.canDestroyInCreative = section.getBoolean("can_destroy_in_creative", true);

        List<?> rawRules = section.getList("rules");
        if (rawRules != null) {
            List<Tool.Rule> parsed = new ArrayList<>();
            for (Object entry : rawRules) {
                if (!(entry instanceof ConfigurationSection ruleSection)) continue;
                Tool.Rule rule = parseRule(ruleSection, namespacedID);
                if (rule == null) return false;
                parsed.add(rule);
            }
            this.rules = List.copyOf(parsed);
        }
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        Tool.Builder builder = Tool.tool()
                .defaultMiningSpeed(defaultMiningSpeed)
                .damagePerBlock(damagePerBlock)
                .canDestroyBlocksInCreative(canDestroyInCreative);
        rules.forEach(builder::addRule);
        itemStack.setData(DataComponentTypes.TOOL, builder.build());
        return itemStack;
    }

    private @Nullable Rule parseRule(ConfigurationSection section, String namespacedID) {
        Object rawBlocks = section.get("blocks");
        RegistryKeySet<BlockType> blockSet = parseBlockSet(rawBlocks, namespacedID);
        if (blockSet == null) return null;

        Object rawSpeed = section.get("speed");
        Float speed = rawSpeed instanceof Number n ? n.floatValue() : null;

        TriState correctForDrops = TriState.NOT_SET;
        if (section.contains("correct_for_drops") && !section.isSet("correct_for_drops")) {
            correctForDrops = TriState.NOT_SET;
        } else if (section.contains("correct_for_drops")) {
            correctForDrops = section.getBoolean("correct_for_drops") ? TriState.TRUE : TriState.FALSE;
        }

        return Tool.rule(blockSet, speed, correctForDrops);
    }

    private @Nullable RegistryKeySet<BlockType> parseBlockSet(@Nullable Object raw, String namespacedID) {
        if (raw instanceof String s) {
            return resolveBlockKeySet(s.trim(), namespacedID);
        }
        if (raw instanceof List<?> list) {
            List<TypedKey<BlockType>> keys = new ArrayList<>();
            for (Object entry : list) {
                if (!(entry instanceof String s)) continue;
                String k = s.trim().toLowerCase(Locale.ROOT);
                if (!k.contains(":")) k = "minecraft:" + k;
                keys.add(TypedKey.create(RegistryKey.BLOCK, Key.key(k)));
            }
            if (keys.isEmpty()) {
                Log.itemWarn("Components", namespacedID, "'tool' rule 'blocks' list must not be empty");
                return null;
            }
            return RegistrySet.keySet(RegistryKey.BLOCK, keys);
        }
        Log.itemWarn("Components", namespacedID, "'tool' rule must have a 'blocks' field (block ID, tag, or list)");
        return null;
    }

    private @Nullable RegistryKeySet<BlockType> resolveBlockKeySet(String raw, String namespacedID) {
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.startsWith("#")) {
            String tagKey = key.substring(1);
            if (!tagKey.contains(":")) tagKey = "minecraft:" + tagKey;
            try {
                RegistryKeySet<BlockType> tag = Registry.BLOCK.getTag(TagKey.create(RegistryKey.BLOCK, Key.key(tagKey)));
                if (tag == null) {
                    Log.itemWarn("Components", namespacedID, "'tool' block tag '{}' not found.", raw);
                    return null;
                }
                return tag;
            } catch (Exception e) {
                Log.itemWarn("Components", namespacedID, "'tool' invalid block tag '{}'.", raw);
                return null;
            }
        }
        if (!key.contains(":")) key = "minecraft:" + key;
        return RegistrySet.keySet(RegistryKey.BLOCK, TypedKey.create(RegistryKey.BLOCK, Key.key(key)));
    }
}
