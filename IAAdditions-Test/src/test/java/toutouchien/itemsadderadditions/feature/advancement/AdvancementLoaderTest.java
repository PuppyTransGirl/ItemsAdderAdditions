package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.common.namespace.CustomTagDefinition;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementLoaderTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @AfterEach
    void clearTags() {
        NamespaceUtils.clearCustomTagRegistry();
    }

    private static YamlConfiguration yamlOf(String yaml) {
        var cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static List<AdvancementDefinition> load(String yaml) {
        return AdvancementLoader.loadAll("testns", yamlOf(yaml).getConfigurationSection("advancements"));
    }

    @Test
    void root_minimalValid_loads() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                """);
        assertEquals(1, result.size());
        var def = result.getFirst();
        assertEquals("testns", def.key().getNamespace());
        assertEquals("myroot", def.key().getKey());
        assertNull(def.parent());
        assertTrue(def.isRoot());
        assertEquals("minecraft:textures/block/stone.png", def.display().background());
    }

    @Test
    void child_missingDisplay_skipped() {
        var result = load("""
                advancements:
                  bad:
                    parent: myroot
                """);
        assertEquals(0, result.size());
    }

    @Test
    void child_missingTitle_skipped() {
        var result = load("""
                advancements:
                  bad:
                    parent: myroot
                    display:
                      description: "desc"
                      icon: minecraft:stone
                """);
        assertEquals(0, result.size());
    }

    @Test
    void child_unknownTrigger_criterionSkipped() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: not_a_trigger
                """);
        assertEquals(0, result.size());
    }

    @Test
    void null_section_returnsEmpty() {
        assertEquals(0, AdvancementLoader.loadAll("testns", null).size());
    }

    private static AdvancementConditions loadSingleCriterion(String trigger, String conditionsYaml) {
        String conditions = conditionsYaml.isBlank() ? "" : "        conditions:\n" + conditionsYaml.indent(10);
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: %s
                """.formatted(trigger) + conditions);
        assertEquals(1, result.size(), "Expected advancement to load for trigger: " + trigger);
        return result.getFirst().criteria().getFirst().conditions();
    }

    @Test
    void parent_withNamespace_keptAsIs() {
        var result = load("""
                advancements:
                  child:
                    parent: "otherns:root"
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        assertEquals("otherns", result.getFirst().parent().getNamespace());
        assertEquals("root", result.getFirst().parent().getKey());
    }

    @Test
    void parent_withoutNamespace_inheritsFileNamespace() {
        var result = load("""
                advancements:
                  child:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        assertEquals("testns", result.getFirst().parent().getNamespace());
        assertEquals("myroot", result.getFirst().parent().getKey());
    }

    @Test
    void enabled_false_skipped() {
        var result = load("""
                advancements:
                  myroot:
                    enabled: false
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                """);
        assertEquals(0, result.size());
    }

    @Test
    void root_withCriteria_loaded() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                    criteria:
                      c1:
                        trigger: break_block
                        conditions:
                          block: "minecraft:stone"
                """);
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().criteria().size());
    }

    @Test
    void localItemTagReference_normalizesAgainstAdvancementNamespace() {
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(new CustomTagDefinition(
                "testns", "ruby_trigger_items", CustomTagType.ITEM,
                List.of("minecraft:diamond_sword"), "test.yml"))));

        AdvancementConditions conditions = loadSingleCriterion("using_item", """
                item: "#ruby_trigger_items"
                """);

        assertInstanceOf(AdvancementConditions.UsingItem.class, conditions);
        assertEquals("#testns:ruby_trigger_items",
                ((AdvancementConditions.UsingItem) conditions).itemId());
    }

    @Test
    void display_invalidFrame_defaultsToTask() {
        var result = load("""
                advancements:
                  child:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                      frame: not_a_frame
                    criteria:
                      c1:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        assertEquals("task", result.getFirst().display().frame());
    }

    @Test
    void display_validFrameGoal_preservedAsIs() {
        var result = load("""
                advancements:
                  child:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                      frame: goal
                    criteria:
                      c1:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        assertEquals("goal", result.getFirst().display().frame());
    }

    @Test
    void display_flags_parsedCorrectly() {
        var result = load("""
                advancements:
                  child:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                      show_toast: false
                      announce_to_chat: false
                      hidden: true
                    criteria:
                      c1:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        var disp = result.getFirst().display();
        assertFalse(disp.showToast());
        assertFalse(disp.announceToChat());
        assertTrue(disp.hidden());
    }

    @Test
    void rewards_experience_parsed() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                    rewards:
                      experience: 100
                """);
        assertEquals(1, result.size());
        assertEquals(100, result.getFirst().rewards().experience());
    }

    @Test
    void rewards_recipes_withNamespace_parsed() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                    rewards:
                      recipes:
                        - "otherns:special_recipe"
                """);
        assertEquals(1, result.size());
        var recipes = result.getFirst().rewards().recipes();
        assertEquals(1, recipes.size());
        assertEquals("otherns", recipes.getFirst().getNamespace());
        assertEquals("special_recipe", recipes.getFirst().getKey());
    }

    @Test
    void rewards_recipes_withoutNamespace_inheritsFileNamespace() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                    rewards:
                      recipes:
                        - "my_recipe"
                """);
        assertEquals(1, result.size());
        var recipes = result.getFirst().rewards().recipes();
        assertEquals(1, recipes.size());
        assertEquals("testns", recipes.getFirst().getNamespace());
        assertEquals("my_recipe", recipes.getFirst().getKey());
    }

    @Test
    void criteria_obtainItem_conditions_parsed() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: obtain_item
                        conditions:
                          items:
                            - "minecraft:diamond"
                          amount: 5
                """);
        assertEquals(1, result.size());
        var crit = result.getFirst().criteria().getFirst();
        assertInstanceOf(AdvancementConditions.ObtainItem.class, crit.conditions());
        var conds = (AdvancementConditions.ObtainItem) crit.conditions();
        assertEquals(List.of("minecraft:diamond"), conds.itemIds());
        assertEquals(5, conds.amount());
    }

    @Test
    void criteria_breakBlock_conditions_parsed() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: break_block
                        conditions:
                          block: "myns:myblock"
                """);
        assertEquals(1, result.size());
        var crit = result.getFirst().criteria().getFirst();
        assertInstanceOf(AdvancementConditions.BreakBlock.class, crit.conditions());
        assertEquals("myns:myblock", ((AdvancementConditions.BreakBlock) crit.conditions()).blockId());
    }

    @Test
    void criteria_inBiome_conditions_parsed() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: in_biome
                        conditions:
                          biome: "minecraft:forest"
                          world: "world"
                """);
        assertEquals(1, result.size());
        var conds = (AdvancementConditions.InBiome) result.getFirst().criteria().getFirst().conditions();
        assertEquals("minecraft:forest", conds.biomeId());
        assertEquals("world", conds.world());
    }

    @Test
    void criteria_inBiome_noWorld_worldIsNull() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: in_biome
                        conditions:
                          biome: "minecraft:plains"
                """);
        assertEquals(1, result.size());
        var conds = (AdvancementConditions.InBiome) result.getFirst().criteria().getFirst().conditions();
        assertNull(conds.world());
    }

    @Test
    void criteria_noneConditions_parsedForNoConditionTrigger() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        assertInstanceOf(AdvancementConditions.None.class, result.getFirst().criteria().getFirst().conditions());
    }

    @Test
    void criteria_multipleCriteria_allLoaded() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: break_block
                        conditions:
                          block: "minecraft:stone"
                      c2:
                        trigger: slept_in_bed
                """);
        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().criteria().size());
    }

    @Test
    void multiple_advancements_allLoaded() {
        var result = load("""
                advancements:
                  root1:
                    display:
                      title: "Root1"
                      description: "d"
                      icon: minecraft:stone
                      background: "minecraft:textures/block/stone.png"
                  root2:
                    display:
                      title: "Root2"
                      description: "d"
                      icon: minecraft:diamond
                      background: "minecraft:textures/block/stone.png"
                """);
        assertEquals(2, result.size());
    }

    @Test
    void criteria_bred_animals_conditions_parsed() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: bred_animals
                        conditions:
                          entity_type: "minecraft:cow"
                """);
        assertEquals(1, result.size());
        var conds = (AdvancementConditions.BredAnimals) result.getFirst().criteria().getFirst().conditions();
        assertEquals("minecraft:cow", conds.entityType());
        assertNull(conds.parentType());
        assertNull(conds.partnerType());
    }

    @Test
    void criteria_enchanted_item_levelsRange_parsed() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: enchanted_item
                        conditions:
                          min_levels: 3
                          max_levels: 10
                """);
        assertEquals(1, result.size());
        var conds = (AdvancementConditions.EnchantedItem) result.getFirst().criteria().getFirst().conditions();
        assertEquals(3, conds.minLevels());
        assertEquals(10, conds.maxLevels());
    }

    @Test
    void criteria_consume_item_parsed() {
        var conds = (AdvancementConditions.ConsumeItem) loadSingleCriterion("consume_item",
                "item: minecraft:apple");
        assertEquals("minecraft:apple", conds.itemId());
    }

    @Test
    void criteria_place_block_parsed() {
        var conds = (AdvancementConditions.PlaceBlock) loadSingleCriterion("place_block",
                "block: minecraft:stone");
        assertEquals("minecraft:stone", conds.blockId());
    }

    @Test
    void criteria_place_furniture_parsed() {
        var conds = (AdvancementConditions.PlaceFurniture) loadSingleCriterion("place_furniture",
                "furniture: myns:my_chair");
        assertEquals("myns:my_chair", conds.furnitureId());
    }

    @Test
    void criteria_break_furniture_parsed() {
        var conds = (AdvancementConditions.BreakFurniture) loadSingleCriterion("break_furniture",
                "furniture: myns:my_chair");
        assertEquals("myns:my_chair", conds.furnitureId());
    }

    @Test
    void criteria_interact_furniture_parsed() {
        var conds = (AdvancementConditions.InteractFurniture) loadSingleCriterion("interact_furniture",
                "furniture: myns:my_chair");
        assertEquals("myns:my_chair", conds.furnitureId());
    }

    @Test
    void criteria_craft_recipe_parsed() {
        var conds = (AdvancementConditions.CraftRecipe) loadSingleCriterion("craft_recipe",
                "recipe: myns:my_recipe");
        assertEquals("myns:my_recipe", conds.recipeId());
    }

    @Test
    void criteria_kill_entity_with_item_parsed() {
        var conds = (AdvancementConditions.KillEntityWithItem) loadSingleCriterion("kill_entity_with_item",
                "item: minecraft:sword\nentity_type: minecraft:zombie");
        assertEquals("minecraft:sword", conds.itemId());
        assertEquals("minecraft:zombie", conds.entityType());
    }

    @Test
    void criteria_permission_parsed() {
        var conds = (AdvancementConditions.Permission) loadSingleCriterion("permission",
                "permission: myns.vip");
        assertEquals("myns.vip", conds.node());
    }

    @Test
    void criteria_using_item_parsed() {
        var conds = (AdvancementConditions.UsingItem) loadSingleCriterion("using_item",
                "item: minecraft:bow");
        assertEquals("minecraft:bow", conds.itemId());
    }

    @Test
    void criteria_tame_animal_parsed() {
        var conds = (AdvancementConditions.TameAnimal) loadSingleCriterion("tame_animal",
                "entity_type: minecraft:wolf");
        assertEquals("minecraft:wolf", conds.entityType());
    }

    @Test
    void criteria_changed_dimension_parsed() {
        var conds = (AdvancementConditions.ChangedDimension) loadSingleCriterion("changed_dimension",
                "to: minecraft:the_nether\nfrom: minecraft:overworld");
        assertEquals("minecraft:the_nether", conds.to());
        assertEquals("minecraft:overworld", conds.from());
    }

    @Test
    void criteria_player_hurt_entity_parsed() {
        var conds = (AdvancementConditions.PlayerHurtEntity) loadSingleCriterion("player_hurt_entity",
                "item: minecraft:sword\nentity_type: minecraft:creeper");
        assertEquals("minecraft:sword", conds.itemId());
        assertEquals("minecraft:creeper", conds.entityType());
    }

    @Test
    void criteria_entity_hurt_player_parsed() {
        var conds = (AdvancementConditions.EntityHurtPlayer) loadSingleCriterion("entity_hurt_player",
                "entity_type: minecraft:skeleton");
        assertEquals("minecraft:skeleton", conds.entityType());
    }

    @Test
    void criteria_shoot_bow_parsed() {
        var conds = (AdvancementConditions.ShootBow) loadSingleCriterion("shoot_bow",
                "item: minecraft:bow");
        assertEquals("minecraft:bow", conds.itemId());
    }

    @Test
    void criteria_villager_trade_parsed() {
        var conds = (AdvancementConditions.VillagerTrade) loadSingleCriterion("villager_trade",
                "item: minecraft:emerald");
        assertEquals("minecraft:emerald", conds.itemId());
    }

    @Test
    void criteria_filled_bucket_parsed() {
        var conds = (AdvancementConditions.FilledBucket) loadSingleCriterion("filled_bucket",
                "item: minecraft:water_bucket");
        assertEquals("minecraft:water_bucket", conds.itemId());
    }

    @Test
    void criteria_fishing_rod_hooked_parsed() {
        var conds = (AdvancementConditions.FishingRodHooked) loadSingleCriterion("fishing_rod_hooked",
                "rod: minecraft:fishing_rod\ncaught_entity_type: minecraft:cod");
        assertEquals("minecraft:fishing_rod", conds.rod());
        assertEquals("minecraft:cod", conds.caughtEntityType());
    }

    @Test
    void criteria_player_killed_entity_parsed() {
        var conds = (AdvancementConditions.PlayerKilledEntity) loadSingleCriterion("player_killed_entity",
                "entity_type: minecraft:enderman\nitem: minecraft:sword");
        assertEquals("minecraft:enderman", conds.entityType());
        assertEquals("minecraft:sword", conds.itemId());
    }

    @Test
    void criteria_recipe_unlocked_parsed() {
        var conds = (AdvancementConditions.RecipeUnlocked) loadSingleCriterion("recipe_unlocked",
                "recipe: myns:my_recipe");
        assertEquals("myns:my_recipe", conds.recipe());
    }

    @Test
    void criteria_effects_changed_parsed() {
        var conds = (AdvancementConditions.EffectsChanged) loadSingleCriterion("effects_changed",
                "effect: minecraft:speed");
        assertEquals("minecraft:speed", conds.effect());
    }

    @Test
    void criteria_bee_nest_destroyed_parsed() {
        var conds = (AdvancementConditions.BeeNestDestroyed) loadSingleCriterion("bee_nest_destroyed",
                "block: minecraft:bee_nest");
        assertEquals("minecraft:bee_nest", conds.blockId());
    }

    @Test
    void criteria_entity_killed_player_parsed() {
        var conds = (AdvancementConditions.EntityKilledPlayer) loadSingleCriterion("entity_killed_player",
                "entity_type: minecraft:zombie");
        assertEquals("minecraft:zombie", conds.entityType());
    }

    @Test
    void criteria_item_durability_changed_parsed() {
        var conds = (AdvancementConditions.ItemDurabilityChanged) loadSingleCriterion("item_durability_changed",
                "item: minecraft:iron_sword");
        assertEquals("minecraft:iron_sword", conds.itemId());
    }

    @Test
    void criteria_item_used_on_block_parsed() {
        var conds = (AdvancementConditions.ItemUsedOnBlock) loadSingleCriterion("item_used_on_block",
                "item: minecraft:bone_meal\nblock: minecraft:grass_block");
        assertEquals("minecraft:bone_meal", conds.itemId());
        assertEquals("minecraft:grass_block", conds.blockId());
    }

    @Test
    void criteria_killed_by_arrow_parsed() {
        var conds = (AdvancementConditions.KilledByArrow) loadSingleCriterion("killed_by_arrow",
                "entity_type: minecraft:skeleton");
        assertEquals("minecraft:skeleton", conds.entityType());
    }

    @Test
    void criteria_player_interacted_with_entity_parsed() {
        var conds = (AdvancementConditions.PlayerInteractedWithEntity) loadSingleCriterion(
                "player_interacted_with_entity",
                "entity_type: minecraft:villager\nitem: minecraft:emerald");
        assertEquals("minecraft:villager", conds.entityType());
        assertEquals("minecraft:emerald", conds.itemId());
    }

    @Test
    void criteria_player_sheared_equipment_parsed() {
        var conds = (AdvancementConditions.PlayerShearedEquipment) loadSingleCriterion(
                "player_sheared_equipment",
                "entity_type: minecraft:sheep");
        assertEquals("minecraft:sheep", conds.entityType());
    }

    @Test
    void criteria_recipe_crafted_parsed() {
        var conds = (AdvancementConditions.RecipeCrafted) loadSingleCriterion("recipe_crafted",
                "recipe: myns:my_recipe");
        assertEquals("myns:my_recipe", conds.recipeId());
    }

    @Test
    void criteria_shot_crossbow_parsed() {
        var conds = (AdvancementConditions.ShotCrossbow) loadSingleCriterion("shot_crossbow",
                "item: minecraft:crossbow");
        assertEquals("minecraft:crossbow", conds.itemId());
    }

    @Test
    void criteria_started_riding_parsed() {
        var conds = (AdvancementConditions.StartedRiding) loadSingleCriterion("started_riding",
                "entity_type: minecraft:horse");
        assertEquals("minecraft:horse", conds.entityType());
    }

    @Test
    void criteria_held_item_parsed() {
        var conds = (AdvancementConditions.HeldItem) loadSingleCriterion("held_item",
                "item: minecraft:torch");
        assertEquals("minecraft:torch", conds.itemId());
    }

    @Test
    void criteria_used_totem_returns_none() {
        assertInstanceOf(AdvancementConditions.None.class,
                loadSingleCriterion("used_totem", ""));
    }

    @Test
    void criteria_fall_from_height_noDistance_usesDefaultRange() {
        var conds = assertInstanceOf(AdvancementConditions.FallFromHeight.class,
                loadSingleCriterion("fall_from_height", ""));
        assertEquals(0.0D, conds.minDistance());
        assertEquals(Double.MAX_VALUE, conds.maxDistance());
    }

    @Test
    void criteria_used_ender_eye_returns_none() {
        assertInstanceOf(AdvancementConditions.None.class,
                loadSingleCriterion("used_ender_eye", ""));
    }

    @Test
    void criteria_impossible_returns_none() {
        assertInstanceOf(AdvancementConditions.None.class,
                loadSingleCriterion("impossible", ""));
    }

    @Test
    void criteria_noConditionsSection_usesDefaults() {
        var result = load("""
                advancements:
                  adv:
                    parent: myroot
                    display:
                      title: "T"
                      description: "D"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: consume_item
                """);
        assertEquals(1, result.size());
        var conds = (AdvancementConditions.ConsumeItem) result.getFirst().criteria().getFirst().conditions();
        assertEquals("", conds.itemId());
    }

    @Test
    void root_missingBackground_backgroundIsNull() {
        var result = load("""
                advancements:
                  myroot:
                    display:
                      title: "Root"
                      description: "desc"
                      icon: minecraft:stone
                """);
        assertEquals(1, result.size());
        assertNull(result.getFirst().display().background());
    }
}
