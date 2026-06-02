package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

class AdvancementPredicateSupportTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        NamespaceUtils.initVanillaCache();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @Nested
    class Conversions {
        @Test
        void booleanObjectParsesBoolAndString() {
            assertEquals(Boolean.TRUE, booleanObject(true));
            assertEquals(Boolean.FALSE, booleanObject("false"));
            assertEquals(Boolean.TRUE, booleanObject("true"));
            assertNull(booleanObject(""));
            assertNull(booleanObject(5));
            assertNull(booleanObject(null));
        }

        @Test
        void intObjectParsesNumberAndString() {
            assertEquals(7, intObject(7));
            assertEquals(7, intObject(7.9));
            assertEquals(42, intObject("42"));
            assertNull(intObject("nope"));
            assertNull(intObject(""));
            assertNull(intObject(null));
        }

        @Test
        void doubleObjectParsesNumberAndString() {
            assertEquals(2.5, doubleObject(2.5));
            assertEquals(3.0, doubleObject("3.0"));
            assertNull(doubleObject("x"));
            assertNull(doubleObject(""));
            assertNull(doubleObject(null));
        }

        @Test
        void stringAndEmptyToNull() {
            assertEquals("5", string(5));
            assertNull(string(null));
            assertNull(emptyToNull(null));
            assertNull(emptyToNull("  "));
            assertEquals("hi", emptyToNull("hi"));
        }

        @Test
        void firstNonBlankString() {
            assertEquals("b", AdvancementPredicateSupport.firstNonBlankString(null, "", "b", "c"));
            assertNull(AdvancementPredicateSupport.firstNonBlankString(null, "", "  "));
        }

        @Test
        void capitalize() {
            assertEquals("Foo", AdvancementPredicateSupport.capitalize("foo"));
            assertEquals("", AdvancementPredicateSupport.capitalize(""));
            assertEquals("X", AdvancementPredicateSupport.capitalize("X"));
        }

        @Test
        void stripNamespace() {
            assertEquals("stone", AdvancementPredicateSupport.stripNamespace("minecraft:Stone"));
            assertEquals("foo", AdvancementPredicateSupport.stripNamespace("FOO"));
        }
    }

    @Nested
    class StructureHelpers {
        @Test
        void valueAndSectionFromMap() {
            Map<String, Object> m = map("a", 1, "child", map("b", 2));
            assertEquals(1, value(m, "a"));
            assertNull(value(m, "missing"));
            assertNotNull(section(m, "child"));
            assertNull(section(m, "a"));
            assertEquals(2, value(section(m, "child"), "b"));
        }

        @Test
        void sectionOrValuePrefersSection() {
            Map<String, Object> m = map("child", map("k", "v"), "scalar", "s");
            assertEquals("s", sectionOrValue(m, "scalar"));
            assertTrue(isSection(sectionOrValue(m, "child")));
        }

        @Test
        void keysFromMapAndNonContainer() {
            assertEquals(Set.of("a", "b"), keys(map("a", 1, "b", 2)));
            assertEquals(Set.of(), keys("not a container"));
        }

        @Test
        void isSectionDetectsMap() {
            assertTrue(isSection(map("a", 1)));
            assertFalse(isSection("string"));
            assertFalse(isSection(null));
        }

        @Test
        void mapOfKeepsOnlyNestedSections() {
            Map<String, Object> m = map("nested", map("x", 1), "scalar", "v");
            Map<String, Object> out = AdvancementPredicateSupport.mapOf(m);
            assertTrue(out.containsKey("nested"));
            assertTrue(out.containsKey("scalar"));
            assertEquals(Map.of(), AdvancementPredicateSupport.mapOf(null));
        }

        @Test
        void hasAnyChecksKeys() {
            Map<String, Object> m = map("a", 1);
            assertTrue(AdvancementPredicateSupport.hasAny(m, "x", "a"));
            assertFalse(AdvancementPredicateSupport.hasAny(m, "x", "y"));
        }

        @Test
        void boolByPath() {
            Map<String, Object> m = map("flag", "true", "no", false);
            assertEquals(Boolean.TRUE, AdvancementPredicateSupport.bool(m, "flag"));
            assertEquals(Boolean.FALSE, AdvancementPredicateSupport.bool(m, "no"));
            assertNull(AdvancementPredicateSupport.bool(m, "missing"));
        }

        @Test
        void readStringListHandlesListCollectionAndScalar() {
            assertEquals(List.of("a", "b"), readStringList(map("k", List.of("a", "b")), "k"));
            assertEquals(List.of("solo"), readStringList(map("k", "solo"), "k"));
            assertEquals(List.of(), readStringList(map("k", ""), "k"));
            assertEquals(List.of(), readStringList(map(), "k"));
        }
    }

    @Nested
    class UnwrapAndParse {
        @Test
        void unwrapEntityProperties() {
            Map<String, Object> raw = map("condition", "minecraft:entity_properties", "predicate", map("type", "minecraft:zombie"));
            Object out = unwrapPredicateCondition(raw);
            assertTrue(isSection(out));
            assertEquals("minecraft:zombie", value(out, "type"));
        }

        @Test
        void unwrapLocationCheckWrapsUnderLocation() {
            Map<String, Object> raw = map("condition", "minecraft:location_check", "predicate", map("dimension", "minecraft:overworld"));
            Object out = unwrapPredicateCondition(raw);
            assertNotNull(value(out, "location"));
        }

        @Test
        void unwrapLocationCheckWithoutPredicateReturnsRaw() {
            Map<String, Object> raw = map("condition", "minecraft:location_check");
            assertSame(raw, unwrapPredicateCondition(raw));
        }

        @Test
        void unwrapNoConditionReturnsRaw() {
            Map<String, Object> raw = map("type", "x");
            assertSame(raw, unwrapPredicateCondition(raw));
            assertNull(unwrapPredicateCondition(null));
        }

        @Test
        void unwrapDefaultUsesNestedPredicate() {
            Map<String, Object> raw = map("condition", "minecraft:random_chance", "predicate", map("k", "v"));
            Object out = unwrapPredicateCondition(raw);
            assertEquals("v", value(out, "k"));
        }

        @Test
        void parseEffectsNormalizesKeys() {
            Map<String, Object> raw = map("speed", map("amplifier", 1), "minecraft:jump_boost", map());
            Map<String, EffectPredicate> out = parseEffects(raw);
            assertTrue(out.containsKey("minecraft:speed"));
            assertTrue(out.containsKey("minecraft:jump_boost"));
            assertEquals(Map.of(), parseEffects(null));
        }

        @Test
        void parseEquipmentKeepsValidSlotsOnly() {
            Map<String, Object> raw = map("mainhand", map("items", "minecraft:stone"), "bogus", map());
            Map<String, ItemPredicate> out = parseEquipment("ns", raw);
            assertTrue(out.containsKey("mainhand"));
            assertFalse(out.containsKey("bogus"));
            assertEquals(Map.of(), parseEquipment("ns", null));
        }

        @Test
        void parses_parseEnchantments() {
            Map<String, Object> raw = map("enchantments", List.of(
                    map("enchantment", "minecraft:sharpness", "levels", map("min", 2)),
                    map("id", "minecraft:looting"),
                    map("nothing", "x")
            ));
            List<EnchantmentPredicate> out = AdvancementPredicateSupport.parseEnchantments(raw);
            assertEquals(2, out.size());
            assertEquals(List.of(), AdvancementPredicateSupport.parseEnchantments(map("enchantments", "not a list")));
        }

        @Test
        void parseStatesScalarAndRange() {
            Map<String, Object> raw = map("facing", "north", "age", map("min", "1", "max", "3"));
            Map<String, StringRange> out = parseStates(raw);
            assertTrue(out.get("facing").matches("north"));
            assertFalse(out.get("facing").matches("south"));
            assertTrue(out.get("age").matches("2"));
            assertFalse(out.get("age").matches("4"));
            assertEquals(Map.of(), parseStates(null));
        }

        @Test
        void parses_parseGameModes() {
            Set<GameMode> out = AdvancementPredicateSupport.parseGameModes(map("gamemode", List.of("survival", "minecraft:creative", "bogus")));
            assertTrue(out.contains(GameMode.SURVIVAL));
            assertTrue(out.contains(GameMode.CREATIVE));
            assertEquals(2, out.size());
            assertEquals(Set.of(), AdvancementPredicateSupport.parseGameModes(map()));
        }

        @Test
        void parseAdvancementsDoneAndCriteria() {
            Map<String, Object> raw = map(
                    "minecraft:story/root", true,
                    "custom:thing", map("crit_a", true, "crit_b", false)
            );
            Map<NamespacedKey, AdvancementRequirement> out = parseAdvancements(raw);
            assertEquals(2, out.size());
            assertEquals(Map.of(), parseAdvancements(null));
        }

        @Test
        void parses_parseRecipes() {
            Map<String, Object> raw = map("minecraft:diamond_sword", true, "custom:thing", false);
            Map<NamespacedKey, Boolean> out = AdvancementPredicateSupport.parseRecipes(raw);
            assertEquals(2, out.size());
            assertTrue(out.containsKey(new NamespacedKey("minecraft", "diamond_sword")));
            assertEquals(Map.of(), AdvancementPredicateSupport.parseRecipes(null));
        }

        @Test
        void parses_parseStats() {
            Map<String, Object> raw = map("stats", List.of(
                    map("type", "minecraft:custom", "stat", "minecraft:jump", "value", map("min", 1)),
                    map("type", "minecraft:custom")
            ));
            List<StatPredicate> out = AdvancementPredicateSupport.parseStats(raw);
            assertEquals(1, out.size());
            assertEquals(List.of(), AdvancementPredicateSupport.parseStats(map("stats", "not list")));
        }
    }

    @Nested
    class Lookups {
        @Test
        void namespacedKey() {
            assertEquals(new NamespacedKey("minecraft", "stone"), AdvancementPredicateSupport.namespacedKey("stone"));
            assertEquals(new NamespacedKey("custom", "thing"), AdvancementPredicateSupport.namespacedKey("custom:thing"));
            assertNull(AdvancementPredicateSupport.namespacedKey(":"));
            assertNull(AdvancementPredicateSupport.namespacedKey(":blank"));
        }

        @Test
        void materialResolvesVanilla() {
            assertEquals(Material.DIAMOND_SWORD, AdvancementPredicateSupport.material("minecraft:diamond_sword"));
            assertNull(AdvancementPredicateSupport.material("minecraft:not_real_item"));
        }

        @Test
        void entityTypeByKey() {
            assertEquals(EntityType.ZOMBIE, AdvancementPredicateSupport.entityType("minecraft:zombie"));
            assertNull(AdvancementPredicateSupport.entityType("minecraft:not_an_entity"));
        }

        @Test
        void parses_statisticFor() {
            assertEquals(Statistic.JUMP, AdvancementPredicateSupport.statisticFor("minecraft:custom", "minecraft:jump"));
            assertEquals(Statistic.CRAFT_ITEM, AdvancementPredicateSupport.statisticFor("minecraft:crafted", "x"));
            assertEquals(Statistic.MINE_BLOCK, AdvancementPredicateSupport.statisticFor("minecraft:mined", "x"));
            assertEquals(Statistic.KILL_ENTITY, AdvancementPredicateSupport.statisticFor("minecraft:killed", "x"));
            assertNull(AdvancementPredicateSupport.statisticFor("minecraft:unknown_type", "x"));
            assertNull(AdvancementPredicateSupport.statisticFor("minecraft:custom", "minecraft:not_a_stat"));
        }
    }

    @Nested
    class StateAndDimension {
        @Test
        void parses_blockStateValue() {
            String data = "minecraft:oak_stairs[facing=north,half=top]";
            assertEquals("north", AdvancementPredicateSupport.blockStateValue(data, "facing"));
            assertEquals("top", AdvancementPredicateSupport.blockStateValue(data, "half"));
            assertNull(AdvancementPredicateSupport.blockStateValue(data, "missing"));
            assertNull(AdvancementPredicateSupport.blockStateValue("minecraft:stone", "facing"));
        }

        @Test
        void compareStateValuesNumericThenLexical() {
            assertTrue(compareStateValues("3", "10") < 0);
            assertEquals(0, compareStateValues("5", "5.0"));
            assertTrue(compareStateValues("abc", "abd") < 0);
        }

        @Test
        void parses_matchesDimension() {
            assertTrue(AdvancementPredicateSupport.matchesDimension(world, "minecraft:overworld"));
            assertFalse(AdvancementPredicateSupport.matchesDimension(world, "minecraft:the_nether"));
            assertTrue(AdvancementPredicateSupport.matchesDimension(world, "world"));
        }

        @Test
        void blockRelativeLocationShiftsDown() {
            Location base = new Location(world, 0, 64, 0);
            Location below = blockRelativeLocation(base, 1.0);
            assertEquals(63.0, below.getY());
            assertEquals(64.0, base.getY(), "original unchanged");
        }

        @Test
        void canSeeSkyAtHighAltitude() {
            assertTrue(canSeeSky(new Location(world, 0, 300, 0)));
        }
    }

    @Nested
    class ItemAndRangeHelpers {
        @Test
        void remainingDurability() {
            ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
            var meta = sword.getItemMeta();
            ((Damageable) meta).setDamage(61);
            sword.setItemMeta(meta);
            int max = Material.DIAMOND_SWORD.getMaxDurability();
            assertEquals(max - 61, AdvancementPredicateSupport.remainingDurability(sword));

            assertEquals(0, AdvancementPredicateSupport.remainingDurability(ItemStack.of(Material.STONE)));
        }

        @Test
        void parseRange() {
            assertArrayEquals(new int[]{3, 3}, AdvancementPredicateSupport.parseRange("3"));
            assertArrayEquals(new int[]{1, 4}, AdvancementPredicateSupport.parseRange("1-4"));
            assertNull(AdvancementPredicateSupport.parseRange("xx"));
        }

        @Test
        void matchesSlotRangeNamedMainhand() {
            PlayerMock player = server.addPlayer();
            player.getInventory().setItemInMainHand(ItemStack.of(Material.DIAMOND_SWORD));
            ItemPredicate predicate = ItemPredicate.parse("ns", map("items", "minecraft:diamond_sword"));
            assertTrue(matchesSlotRange(player, "mainhand", predicate));
            assertTrue(matchesSlotRange(player, "weapon.mainhand", predicate));
        }

        @Test
        void matchesSlotRangeNumericBounds() {
            PlayerMock player = server.addPlayer();
            player.getInventory().setItem(5, ItemStack.of(Material.DIAMOND));
            ItemPredicate predicate = ItemPredicate.parse("ns", map("items", "minecraft:diamond"));
            assertTrue(matchesSlotRange(player, "container.0-8", predicate));
            assertFalse(matchesSlotRange(player, "not_a_slot", predicate));
        }
    }

    @Nested
    class Reflection {
        record Holder(ItemStack item) {
            public ItemStack getStack() {
                return item;
            }

            public boolean isJumping() {
                return true;
            }

            public boolean sneaking() {
                return false;
            }
        }

        @Test
        void itemFromMethod() {
            Holder h = new Holder(ItemStack.of(Material.STICK));
            assertEquals(Material.STICK, AdvancementPredicateSupport.itemFromMethod(h, "getStack").getType());
            assertNull(AdvancementPredicateSupport.itemFromMethod(h, "noSuchMethod"));
        }

        @Test
        void parses_reflectBoolean() {
            Holder h = new Holder(null);
            assertEquals(Optional.of(true), AdvancementPredicateSupport.reflectBoolean(h, "isJumping"));
            assertEquals(Optional.of(false), AdvancementPredicateSupport.reflectBoolean(h, "sneaking"));
            assertEquals(Optional.empty(), AdvancementPredicateSupport.reflectBoolean(h, "missing"));
        }

        @Test
        void readInputBooleanTriesIsPrefixThenRaw() {
            Holder h = new Holder(null);
            assertEquals(Optional.of(true), readInputBoolean(h, "jumping"));
            assertEquals(Optional.of(false), readInputBoolean(h, "sneaking"));
            assertEquals(Optional.empty(), readInputBoolean(h, "absent"));
        }
    }

    @Nested
    class EntityHelpers {
        @Test
        void matchesLimitedNbtBlankAlwaysTrue() {
            PlayerMock player = server.addPlayer();
            assertTrue(matchesLimitedNbt(player, "   "));
        }

        @Test
        void matchesLimitedNbtTags() {
            PlayerMock player = server.addPlayer();
            player.addScoreboardTag("foo");
            assertTrue(matchesLimitedNbt(player, "{Tags:[\"foo\"]}"));
            assertFalse(matchesLimitedNbt(player, "{Tags:[\"bar\"]}"));
        }

        @Test
        void matchesLimitedNbtCustomNameAbsent() {
            PlayerMock player = server.addPlayer();
            assertFalse(matchesLimitedNbt(player, "{CustomName:'x'}"));
        }

        @Test
        void entityTeamNullWhenNoTeam() {
            PlayerMock player = server.addPlayer();
            assertNull(AdvancementPredicateSupport.entityTeam(player));
        }
    }

    @Nested
    class NormalizeIdOrTag {
        @Test
        void minecraftIdOrTagHandlesTagPrefix() {
            assertEquals("#minecraft:logs", AdvancementPredicateSupport.normalizeMinecraftIdOrTag("#logs"));
            assertEquals("minecraft:stone", AdvancementPredicateSupport.normalizeMinecraftIdOrTag("Stone"));
        }
    }
}
