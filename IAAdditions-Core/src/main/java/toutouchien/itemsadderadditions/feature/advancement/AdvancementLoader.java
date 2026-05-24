package toutouchien.itemsadderadditions.feature.advancement;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.advancement.trigger.RuntimeTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@NullMarked
public final class AdvancementLoader {
    private static final String LOG_TAG = "Advancement";

    private AdvancementLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static List<AdvancementDefinition> loadAll(
            String namespace, @Nullable ConfigurationSection section
    ) {
        if (section == null) return List.of();
        List<AdvancementDefinition> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;
            if (!entry.getBoolean("enabled", true)) continue;
            try {
                AdvancementDefinition def = parse(namespace, id, entry);
                if (def != null) result.add(def);
            } catch (Exception e) {
                Log.warn(LOG_TAG, "Failed to load advancement {}:{} - {}", namespace, id, e.getMessage());
            }
        }
        return result;
    }

    @Nullable
    private static AdvancementDefinition parse(
            String namespace, String id, ConfigurationSection entry
    ) {
        NamespacedKey key = new NamespacedKey(namespace, id.toLowerCase(Locale.ROOT));
        NamespacedKey parent = parseParent(namespace, entry.getString("parent"));

        ConfigurationSection displaySec = entry.getConfigurationSection("display");
        if (displaySec == null) {
            Log.warn(LOG_TAG, "Missing 'display' for {}:{}", namespace, id);
            return null;
        }
        AdvancementDisplayDefinition display = parseDisplay(namespace, id, displaySec);
        if (display == null) return null;

        ConfigurationSection criteriaSec = entry.getConfigurationSection("criteria");
        List<AdvancementCriterionDefinition> criteria = parseCriteria(
                namespace, id, criteriaSec
        );

        if (criteria.isEmpty() && criteriaSec != null && parent != null) {
            Log.warn(LOG_TAG, "Advancement {}:{} has no valid criteria, skipping.", namespace, id);
            return null;
        }

        AdvancementRewardDefinition rewards = parseRewards(
                namespace, entry.getConfigurationSection("rewards")
        );

        CompletionActions onComplete = CompletionActionsParser.parse(
                entry.getConfigurationSection("on_complete")
        );

        return new AdvancementDefinition(key, parent, display, criteria, rewards, onComplete);
    }

    @Nullable
    private static NamespacedKey parseParent(String namespace, @Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            return new NamespacedKey(parts[0], parts[1].toLowerCase(Locale.ROOT));
        }
        return new NamespacedKey(namespace, raw.toLowerCase(Locale.ROOT));
    }

    @Nullable
    private static AdvancementDisplayDefinition parseDisplay(
            String namespace, String id, ConfigurationSection sec
    ) {
        String title = sec.getString("title");
        if (title == null || title.isBlank()) {
            Log.warn(LOG_TAG, "Missing 'display.title' for {}:{}", namespace, id);
            return null;
        }
        String description = sec.getString("description", "");
        String frame = sec.getString("frame", "task").toLowerCase(Locale.ROOT);
        String iconId = sec.getString("icon", "minecraft:barrier");
        String background = sec.getString("background");

        ItemStack icon = resolveIcon(namespace, iconId, id);
        boolean showToast = sec.getBoolean("show_toast", true);
        boolean announce = sec.getBoolean("announce_to_chat", true);
        boolean hidden = sec.getBoolean("hidden", false);

        return new AdvancementDisplayDefinition(
                icon, title, description, frame, background, showToast, announce, hidden
        );
    }

    private static ItemStack resolveIcon(String namespace, String iconId, String advId) {
        ItemStack item = NamespaceUtils.itemByID(namespace, iconId);
        if (item != null) return item;
        if (iconId.contains(":")) {
            String[] parts = iconId.split(":", 2);
            item = NamespaceUtils.itemByID(parts[0], parts[1]);
            if (item != null) return item;
            // Try as vanilla material using only the key part
            try {
                Material mat = Material.matchMaterial(parts[1]);
                if (mat != null && mat.isItem()) return new ItemStack(mat);
            } catch (Exception ignored) {
            }
        }
        Log.warn(LOG_TAG, "Could not resolve icon '{}' for advancement {}", iconId, advId);
        return new ItemStack(Material.BARRIER);
    }

    private static List<AdvancementCriterionDefinition> parseCriteria(
            String namespace, String advId, @Nullable ConfigurationSection sec
    ) {
        if (sec == null) return List.of();
        List<AdvancementCriterionDefinition> result = new ArrayList<>();
        for (String name : sec.getKeys(false)) {
            ConfigurationSection entry = sec.getConfigurationSection(name);
            if (entry == null) continue;
            String triggerStr = entry.getString("trigger", "impossible");
            RuntimeTrigger trigger = RuntimeTrigger.fromYaml(triggerStr);
            if (trigger == null) {
                Log.warn(LOG_TAG, "Unknown trigger '{}' in criterion '{}:{}.{}'",
                        triggerStr, namespace, advId, name);
                continue;
            }
            AdvancementConditions conditions = parseConditions(
                    namespace, trigger, entry.getConfigurationSection("conditions")
            );
            result.add(new AdvancementCriterionDefinition(name, trigger, conditions));
        }
        return result;
    }

    private static AdvancementConditions parseConditions(
            String namespace, RuntimeTrigger trigger, @Nullable ConfigurationSection sec
    ) {
        return switch (trigger) {
            case OBTAIN_ITEM -> {
                List<String> rawItems = sec != null ? sec.getStringList("items") : List.of();
                List<String> items = rawItems.stream()
                        .map(id -> normalizeItemId(namespace, id))
                        .toList();
                int amount = sec != null ? sec.getInt("amount", 1) : 1;
                yield new AdvancementConditions.ObtainItem(items, amount);
            }
            case CONSUME_ITEM -> new AdvancementConditions.ConsumeItem(
                    normalizeItemId(namespace, sec != null ? sec.getString("item", "") : "")
            );
            case PLACE_BLOCK -> new AdvancementConditions.PlaceBlock(
                    sec != null ? sec.getString("block", "") : ""
            );
            case BREAK_BLOCK -> new AdvancementConditions.BreakBlock(
                    sec != null ? sec.getString("block", "") : ""
            );
            case PLACE_FURNITURE -> new AdvancementConditions.PlaceFurniture(
                    sec != null ? sec.getString("furniture", "") : ""
            );
            case BREAK_FURNITURE -> new AdvancementConditions.BreakFurniture(
                    sec != null ? sec.getString("furniture", "") : ""
            );
            case INTERACT_FURNITURE -> new AdvancementConditions.InteractFurniture(
                    sec != null ? sec.getString("furniture", "") : ""
            );
            case CRAFT_RECIPE -> new AdvancementConditions.CraftRecipe(
                    sec != null ? sec.getString("recipe", "") : ""
            );
            case KILL_ENTITY_WITH_ITEM -> new AdvancementConditions.KillEntityWithItem(
                    normalizeItemId(namespace, sec != null ? sec.getString("item", "") : ""),
                    sec != null ? sec.getString("entity_type") : null
            );
            case PERMISSION -> new AdvancementConditions.Permission(
                    sec != null ? sec.getString("permission", "") : ""
            );
            case IN_BIOME -> new AdvancementConditions.InBiome(
                    sec != null ? sec.getString("biome", "") : "",
                    sec != null ? sec.getString("world") : null
            );
            case USING_ITEM -> new AdvancementConditions.UsingItem(
                    normalizeItemId(namespace, sec != null ? sec.getString("item", "") : "")
            );
            case TAME_ANIMAL -> new AdvancementConditions.TameAnimal(
                    sec != null ? sec.getString("entity_type") : null
            );
            case ENCHANTED_ITEM -> new AdvancementConditions.EnchantedItem(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null),
                    sec != null ? sec.getInt("min_levels", 0) : 0,
                    sec != null ? sec.getInt("max_levels", Integer.MAX_VALUE) : Integer.MAX_VALUE
            );
            case BRED_ANIMALS -> new AdvancementConditions.BredAnimals(
                    sec != null ? sec.getString("entity_type") : null,
                    sec != null ? sec.getString("parent_type") : null,
                    sec != null ? sec.getString("partner_type") : null
            );
            case CHANGED_DIMENSION -> new AdvancementConditions.ChangedDimension(
                    sec != null ? sec.getString("to") : null,
                    sec != null ? sec.getString("from") : null
            );
            case PLAYER_HURT_ENTITY -> new AdvancementConditions.PlayerHurtEntity(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null),
                    sec != null ? sec.getString("entity_type") : null
            );
            case ENTITY_HURT_PLAYER -> new AdvancementConditions.EntityHurtPlayer(
                    sec != null ? sec.getString("entity_type") : null
            );
            case SHOOT_BOW -> new AdvancementConditions.ShootBow(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case VILLAGER_TRADE -> new AdvancementConditions.VillagerTrade(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case FILLED_BUCKET -> new AdvancementConditions.FilledBucket(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case FISHING_ROD_HOOKED -> new AdvancementConditions.FishingRodHooked(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("rod") : null),
                    sec != null ? sec.getString("caught_entity_type") : null
            );
            case PLAYER_KILLED_ENTITY -> new AdvancementConditions.PlayerKilledEntity(
                    sec != null ? sec.getString("entity_type") : null,
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case RECIPE_UNLOCKED -> new AdvancementConditions.RecipeUnlocked(
                    sec != null ? sec.getString("recipe", "") : ""
            );
            case EFFECTS_CHANGED -> new AdvancementConditions.EffectsChanged(
                    sec != null ? sec.getString("effect") : null
            );
            case BEE_NEST_DESTROYED -> new AdvancementConditions.BeeNestDestroyed(
                    sec != null ? sec.getString("block") : null
            );
            case ENTITY_KILLED_PLAYER -> new AdvancementConditions.EntityKilledPlayer(
                    sec != null ? sec.getString("entity_type") : null
            );
            case ITEM_DURABILITY_CHANGED -> new AdvancementConditions.ItemDurabilityChanged(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case ITEM_USED_ON_BLOCK -> new AdvancementConditions.ItemUsedOnBlock(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null),
                    sec != null ? sec.getString("block") : null
            );
            case KILLED_BY_ARROW -> new AdvancementConditions.KilledByArrow(
                    sec != null ? sec.getString("entity_type") : null
            );
            case PLAYER_INTERACTED_WITH_ENTITY -> new AdvancementConditions.PlayerInteractedWithEntity(
                    sec != null ? sec.getString("entity_type") : null,
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case PLAYER_SHEARED_EQUIPMENT -> new AdvancementConditions.PlayerShearedEquipment(
                    sec != null ? sec.getString("entity_type") : null
            );
            case RECIPE_CRAFTED -> new AdvancementConditions.RecipeCrafted(
                    sec != null ? sec.getString("recipe", "") : ""
            );
            case SHOT_CROSSBOW -> new AdvancementConditions.ShotCrossbow(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case STARTED_RIDING -> new AdvancementConditions.StartedRiding(
                    sec != null ? sec.getString("entity_type") : null
            );
            case HELD_ITEM -> new AdvancementConditions.HeldItem(
                    normalizeItemIdNullable(namespace, sec != null ? sec.getString("item") : null)
            );
            case SLEPT_IN_BED, USED_TOTEM, FALL_FROM_HEIGHT, USED_ENDER_EYE, IMPOSSIBLE -> AdvancementConditions.None.INSTANCE;
        };
    }

    private static String normalizeItemId(String namespace, String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains(":")) return lower;
        CustomStack cs = NamespaceUtils.customItemByID(namespace, lower);
        if (cs != null) return cs.getNamespacedID();
        return NamespaceUtils.normalizeID("minecraft", lower);
    }

    @Nullable
    private static String normalizeItemIdNullable(String namespace, @Nullable String raw) {
        if (raw == null) return null;
        return normalizeItemId(namespace, raw);
    }

    private static AdvancementRewardDefinition parseRewards(
            String namespace, @Nullable ConfigurationSection sec
    ) {
        if (sec == null) return AdvancementRewardDefinition.EMPTY;
        int exp = sec.getInt("experience", 0);
        List<String> loot = sec.getStringList("loot");
        List<String> recipeIds = sec.getStringList("recipes");
        List<NamespacedKey> recipes = new ArrayList<>();
        for (String rid : recipeIds) {
            if (rid.contains(":")) {
                String[] parts = rid.split(":", 2);
                recipes.add(new NamespacedKey(parts[0], parts[1]));
            } else {
                recipes.add(new NamespacedKey(namespace, rid));
            }
        }
        return new AdvancementRewardDefinition(exp, loot, recipes);
    }
}
