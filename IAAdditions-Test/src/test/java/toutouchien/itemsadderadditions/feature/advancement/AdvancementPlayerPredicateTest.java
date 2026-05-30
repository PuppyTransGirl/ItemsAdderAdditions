package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementPlayerPredicateTest {
    private static ServerMock server;
    private static WorldMock world;
    private PlayerMock player;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void freshPlayer() {
        player = server.addPlayer();
        player.setLocation(new Location(world, 0.5D, 64.0D, 0.5D));
        player.getInventory().clear();
        player.setSneaking(false);
        player.setSprinting(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
    }

    private static Object rawPlayer(String yaml) {
        var cfg = new YamlConfiguration();
        try {
            cfg.loadFromString("player:\n" + yaml.indent(2));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg.get("player");
    }

    private static AdvancementPlayerPredicate predicate(String yaml) {
        return AdvancementPlayerPredicate.parse("testns", rawPlayer(yaml));
    }

    @Test
    void anyPredicate_matchesPlayer() {
        assertTrue(AdvancementPlayerPredicate.ANY.matches(player));
        assertTrue(AdvancementPlayerPredicate.parse("testns", null).matches(player));
    }

    @Test
    void entityFields_typeFlagsEquipmentAndEffects_mustAllMatch() {
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(ItemStack.of(Material.DIAMOND_SWORD));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, true));

        AdvancementPlayerPredicate predicate = predicate("""
                type: minecraft:player
                flags:
                  is_sneaking: true
                equipment:
                  mainhand:
                    items: minecraft:diamond_sword
                effects:
                  minecraft:speed:
                    amplifier: 1
                    duration:
                      min: 100
                """);

        assertTrue(predicate.matches(player));

        player.setSneaking(false);
        assertFalse(predicate.matches(player));
    }

    @Test
    void typeSpecificPlayer_gamemodeLevelFoodAndInputFallback_matchPlayerState() {
        player.setGameMode(GameMode.ADVENTURE);
        player.setLevel(12);
        player.setFoodLevel(18);
        player.setSaturation(4.5F);
        player.setSneaking(true);
        player.setSprinting(true);

        AdvancementPlayerPredicate predicate = predicate("""
                type_specific:
                  type: minecraft:player
                  gamemode: adventure
                  level:
                    min: 10
                    max: 15
                  food:
                    level:
                      min: 16
                    saturation:
                      min: 4.0
                      max: 5.0
                  input:
                    sneak: true
                    sprint: true
                """);

        assertTrue(predicate.matches(player));

        player.setGameMode(GameMode.CREATIVE);
        assertFalse(predicate.matches(player));
    }

    @Test
    void locationPredicate_dimensionWorldPositionAndBlock_mustAllMatch() {
        world.getBlockAt(5, 64, 5).setType(Material.STONE);
        player.setLocation(new Location(world, 5.5D, 64.0D, 5.5D));

        AdvancementPlayerPredicate predicate = predicate("""
                location:
                  dimension: minecraft:overworld
                  world: world
                  position:
                    x:
                      min: 5
                      max: 6
                    y: 64
                    z:
                      min: 5
                      max: 6
                  block:
                    blocks: minecraft:stone
                """);

        assertTrue(predicate.matches(player));

        world.getBlockAt(5, 64, 5).setType(Material.DIRT);
        assertFalse(predicate.matches(player));
    }

    @Test
    void slotsPredicate_namedAndInventoryRanges_matchInventoryContents() {
        player.getInventory().setItemInMainHand(ItemStack.of(Material.DIAMOND_SWORD));
        player.getInventory().setItem(9, ItemStack.of(Material.APPLE, 2));

        AdvancementPlayerPredicate predicate = predicate("""
                slots:
                  weapon.mainhand:
                    items: minecraft:diamond_sword
                  inventory.0:
                    items: minecraft:apple
                    count: 2
                """);

        assertTrue(predicate.matches(player));

        player.getInventory().setItem(9, ItemStack.of(Material.CARROT, 2));
        assertFalse(predicate.matches(player));
    }

    @Test
    void listFormat_requiresEveryNestedPredicateToPass() {
        player.setSneaking(true);
        player.setGameMode(GameMode.SURVIVAL);

        AdvancementPlayerPredicate predicate = predicate("""
                - condition: minecraft:entity_properties
                  predicate:
                    flags:
                      is_sneaking: true
                - condition: minecraft:entity_properties
                  predicate:
                    type_specific:
                      type: minecraft:player
                      gamemode: survival
                """);

        assertTrue(predicate.matches(player));

        player.setSneaking(false);
        assertFalse(predicate.matches(player));
    }

    @Test
    void unsupportedComponentsAndPredicates_failClosed() {
        AdvancementPlayerPredicate predicateWithUnsupportedComponents = predicate("""
                components:
                  minecraft:custom_name: "Unsupported at entity level"
                """);
        AdvancementPlayerPredicate predicateWithUnsupportedPredicates = predicate("""
                predicates:
                  minecraft:custom_name: {}
                """);

        assertFalse(predicateWithUnsupportedComponents.matches(player));
        assertFalse(predicateWithUnsupportedPredicates.matches(player));
    }
}
