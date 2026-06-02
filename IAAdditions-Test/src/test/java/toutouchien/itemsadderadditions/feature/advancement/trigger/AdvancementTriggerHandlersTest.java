package toutouchien.itemsadderadditions.feature.advancement.trigger;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.advancement.*;
import toutouchien.itemsadderadditions.nms.api.INmsAdvancementHandler;
import toutouchien.itemsadderadditions.nms.api.INmsHandler;
import toutouchien.itemsadderadditions.testsupport.FakeNms;

import java.util.List;

import static org.mockito.Mockito.*;

class AdvancementTriggerHandlersTest {
    private static ServerMock server;
    private static WorldMock world;
    private INmsHandler nms;
    private INmsAdvancementHandler advancements;
    private AdvancementRegistry registry;
    private PlayerMock player;
    private int counter;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        nms = FakeNms.install();
        advancements = nms.advancements();
        registry = new AdvancementRegistry();
        player = server.addPlayer();
        counter = 0;
    }

    @AfterEach
    void tearDown() {
        FakeNms.uninstall();
    }

    private NamespacedKey register(RuntimeTrigger trigger, AdvancementConditions conditions) {
        NamespacedKey key = new NamespacedKey("test", "adv_" + counter++);
        AdvancementCriterionDefinition crit = new AdvancementCriterionDefinition("crit", trigger, conditions);
        AdvancementDisplayDefinition display = new AdvancementDisplayDefinition(
                new ItemStack(Material.STONE), "Title", "Desc", "task", null, true, false, false);
        AdvancementDefinition def = new AdvancementDefinition(
                key, null, display, List.of(crit),
                AdvancementRewardDefinition.EMPTY, CompletionActions.EMPTY);
        registry.setAll(List.of(def));
        return key;
    }

    private void verifyAwarded(NamespacedKey key) {
        verify(advancements).award(player, key, "crit");
    }

    private void verifyNotAwarded() {
        verify(advancements, never()).award(any(), any(), any());
    }

    @Test
    void permissionAwardsWhenPlayerHasNode() {
        NamespacedKey key = register(RuntimeTrigger.PERMISSION,
                new AdvancementConditions.Permission("iaa.adv.test"));
        player.addAttachment(MockBukkit.createMockPlugin(), "iaa.adv.test", true);

        new PermissionTriggerHandler(registry).onJoin(new PlayerJoinEvent(player, Component.empty()));

        verifyAwarded(key);
    }

    @Test
    void permissionDoesNotAwardWithoutNode() {
        register(RuntimeTrigger.PERMISSION, new AdvancementConditions.Permission("iaa.adv.missing"));
        player.setOp(false);

        new PermissionTriggerHandler(registry).onJoin(new PlayerJoinEvent(player, Component.empty()));

        verifyNotAwarded();
    }

    @Test
    void heldItemAwardsOnMatchingItem() {
        NamespacedKey key = register(RuntimeTrigger.HELD_ITEM,
                new AdvancementConditions.HeldItem("minecraft:diamond_sword"));
        player.getInventory().setItem(3, new ItemStack(Material.DIAMOND_SWORD));

        new HeldItemTriggerHandler(registry).onItemHeld(new PlayerItemHeldEvent(player, 0, 3));

        verifyAwarded(key);
    }

    @Test
    void heldItemIgnoresEmptySlot() {
        register(RuntimeTrigger.HELD_ITEM, new AdvancementConditions.HeldItem("minecraft:diamond_sword"));

        new HeldItemTriggerHandler(registry).onItemHeld(new PlayerItemHeldEvent(player, 0, 4));

        verifyNotAwarded();
    }

    @Test
    void heldItemDoesNotAwardOnDifferentItem() {
        register(RuntimeTrigger.HELD_ITEM, new AdvancementConditions.HeldItem("minecraft:diamond_sword"));
        player.getInventory().setItem(3, new ItemStack(Material.STICK));

        new HeldItemTriggerHandler(registry).onItemHeld(new PlayerItemHeldEvent(player, 0, 3));

        verifyNotAwarded();
    }

    @Test
    void consumeItemAwardsOnMatch() {
        NamespacedKey key = register(RuntimeTrigger.CONSUME_ITEM,
                new AdvancementConditions.ConsumeItem("minecraft:apple"));

        new ConsumeItemTriggerHandler(registry).onConsume(
                new PlayerItemConsumeEvent(player, new ItemStack(Material.APPLE), EquipmentSlot.HAND));

        verifyAwarded(key);
    }

    @Test
    void placeBlockAwardsOnVanillaPlace() {
        NamespacedKey key = register(RuntimeTrigger.PLACE_BLOCK,
                new AdvancementConditions.PlaceBlock("minecraft:stone"));
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.STONE);
        BlockPlaceEvent event = new BlockPlaceEvent(
                block, block.getState(), block, new ItemStack(Material.STONE), player, true);

        new PlaceBlockTriggerHandler(registry).onVanillaPlace(event);

        verifyAwarded(key);
    }

    @Test
    void breakBlockAwardsOnVanillaBreak() {
        NamespacedKey key = register(RuntimeTrigger.BREAK_BLOCK,
                new AdvancementConditions.BreakBlock("minecraft:dirt"));
        Block block = world.getBlockAt(1, 64, 0);
        block.setType(Material.DIRT);

        new BreakBlockTriggerHandler(registry).onVanillaBreak(new BlockBreakEvent(block, player));

        verifyAwarded(key);
    }

    @Test
    void breakBlockIgnoresNonMatchingBlock() {
        register(RuntimeTrigger.BREAK_BLOCK, new AdvancementConditions.BreakBlock("minecraft:diamond_ore"));
        Block block = world.getBlockAt(2, 64, 0);
        block.setType(Material.DIRT);

        new BreakBlockTriggerHandler(registry).onVanillaBreak(new BlockBreakEvent(block, player));

        verifyNotAwarded();
    }

    @Test
    void obtainItemAwardsWhenEnoughCollected() {
        NamespacedKey key = register(RuntimeTrigger.OBTAIN_ITEM,
                new AdvancementConditions.ObtainItem(List.of("minecraft:diamond"), 1));
        Item dropped = world.dropItem(world.getSpawnLocation(), new ItemStack(Material.DIAMOND, 5));

        new ObtainItemTriggerHandler(registry).onPickup(new EntityPickupItemEvent(player, dropped, 0));

        verifyAwarded(key);
    }

    @Test
    void bredAnimalsAwardsOnAnyType() {
        NamespacedKey key = register(RuntimeTrigger.BRED_ANIMALS,
                new AdvancementConditions.BredAnimals(null, null, null));
        Cow child = (Cow) world.spawnEntity(world.getSpawnLocation(), EntityType.COW);
        Cow mother = (Cow) world.spawnEntity(world.getSpawnLocation(), EntityType.COW);
        Cow father = (Cow) world.spawnEntity(world.getSpawnLocation(), EntityType.COW);

        EntityBreedEvent event = new EntityBreedEvent(child, mother, father, player, null, 1);
        new BredAnimalsTriggerHandler(registry).onBreed(event);

        verifyAwarded(key);
    }

    @Test
    void playerHurtEntityAwards() {
        NamespacedKey key = register(RuntimeTrigger.PLAYER_HURT_ENTITY,
                new AdvancementConditions.PlayerHurtEntity(null, null));
        org.bukkit.entity.Zombie target = (org.bukkit.entity.Zombie)
                world.spawnEntity(world.getSpawnLocation(), EntityType.ZOMBIE);
        var event = new org.bukkit.event.entity.EntityDamageByEntityEvent(
                player, target, org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK, 4.0);

        new PlayerHurtEntityTriggerHandler(registry).onDamage(event);

        verifyAwarded(key);
    }

    @Test
    void entityHurtPlayerAwards() {
        NamespacedKey key = register(RuntimeTrigger.ENTITY_HURT_PLAYER,
                new AdvancementConditions.EntityHurtPlayer(null));
        org.bukkit.entity.Zombie attacker = (org.bukkit.entity.Zombie)
                world.spawnEntity(world.getSpawnLocation(), EntityType.ZOMBIE);
        var event = new org.bukkit.event.entity.EntityDamageByEntityEvent(
                attacker, player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK, 4.0);

        new EntityHurtPlayerTriggerHandler(registry).onDamage(event);

        verifyAwarded(key);
    }

    @Test
    void playerInteractedWithEntityAwards() {
        NamespacedKey key = register(RuntimeTrigger.PLAYER_INTERACTED_WITH_ENTITY,
                new AdvancementConditions.PlayerInteractedWithEntity(null, null));
        org.bukkit.entity.Cow cow = (org.bukkit.entity.Cow)
                world.spawnEntity(world.getSpawnLocation(), EntityType.COW);

        new PlayerInteractedWithEntityTriggerHandler(registry).onInteract(
                new org.bukkit.event.player.PlayerInteractEntityEvent(player, cow, EquipmentSlot.HAND));

        verifyAwarded(key);
    }

    @Test
    void playerInteractedWithEntityIgnoresOffHand() {
        register(RuntimeTrigger.PLAYER_INTERACTED_WITH_ENTITY,
                new AdvancementConditions.PlayerInteractedWithEntity(null, null));
        org.bukkit.entity.Cow cow = (org.bukkit.entity.Cow)
                world.spawnEntity(world.getSpawnLocation(), EntityType.COW);

        new PlayerInteractedWithEntityTriggerHandler(registry).onInteract(
                new org.bukkit.event.player.PlayerInteractEntityEvent(player, cow, EquipmentSlot.OFF_HAND));

        verifyNotAwarded();
    }

    @Test
    void itemUsedOnBlockAwards() {
        NamespacedKey key = register(RuntimeTrigger.ITEM_USED_ON_BLOCK,
                new AdvancementConditions.ItemUsedOnBlock("minecraft:bone_meal", "minecraft:grass_block"));
        Block block = world.getBlockAt(5, 64, 5);
        block.setType(Material.GRASS_BLOCK);
        ItemStack item = new ItemStack(Material.BONE_MEAL);
        var event = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, item, block,
                org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);

        new ItemUsedOnBlockTriggerHandler(registry).onInteract(event);

        verifyAwarded(key);
    }

    @Test
    void changedDimensionAwardsWithNoFilter() {
        NamespacedKey key = register(RuntimeTrigger.CHANGED_DIMENSION,
                new AdvancementConditions.ChangedDimension(null, null));

        new ChangedDimensionTriggerHandler(registry).onWorldChange(
                new org.bukkit.event.player.PlayerChangedWorldEvent(player, world));

        verifyAwarded(key);
    }

    @Test
    void tameAnimalAwards() {
        NamespacedKey key = register(RuntimeTrigger.TAME_ANIMAL,
                new AdvancementConditions.TameAnimal(null));
        org.bukkit.entity.Wolf wolf = (org.bukkit.entity.Wolf)
                world.spawnEntity(world.getSpawnLocation(), EntityType.WOLF);

        new TameAnimalTriggerHandler(registry).onTame(
                new org.bukkit.event.entity.EntityTameEvent(wolf, player));

        verifyAwarded(key);
    }

    @Test
    void usingItemAwards() {
        NamespacedKey key = register(RuntimeTrigger.USING_ITEM,
                new AdvancementConditions.UsingItem("minecraft:carrot_on_a_stick"));
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        var event = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.RIGHT_CLICK_AIR, item, null,
                org.bukkit.block.BlockFace.SELF, EquipmentSlot.HAND);

        new UsingItemTriggerHandler(registry).onInteract(event);

        verifyAwarded(key);
    }

    @Test
    void filledBucketAwards() {
        NamespacedKey key = register(RuntimeTrigger.FILLED_BUCKET,
                new AdvancementConditions.FilledBucket("minecraft:water_bucket"));
        Block block = world.getBlockAt(6, 64, 0);
        var event = new org.bukkit.event.player.PlayerBucketFillEvent(
                player, block, block, org.bukkit.block.BlockFace.UP, Material.BUCKET,
                new ItemStack(Material.WATER_BUCKET));

        new FilledBucketTriggerHandler(registry).onFill(event);

        verifyAwarded(key);
    }

    @Test
    void beeNestDestroyedAwards() {
        NamespacedKey key = register(RuntimeTrigger.BEE_NEST_DESTROYED,
                new AdvancementConditions.BeeNestDestroyed("minecraft:bee_nest"));
        Block block = world.getBlockAt(7, 64, 0);
        block.setType(Material.BEE_NEST);

        new BeeNestDestroyedTriggerHandler(registry).onBreak(new BlockBreakEvent(block, player));

        verifyAwarded(key);
    }

    @Test
    void shotCrossbowAwards() {
        NamespacedKey key = register(RuntimeTrigger.SHOT_CROSSBOW,
                new AdvancementConditions.ShotCrossbow(null));
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        org.bukkit.entity.Arrow arrow = (org.bukkit.entity.Arrow)
                world.spawnEntity(world.getSpawnLocation(), EntityType.ARROW);
        var event = new org.bukkit.event.entity.EntityShootBowEvent(player, crossbow, arrow, 1.0f);

        new ShotCrossbowTriggerHandler(registry).onShoot(event);

        verifyAwarded(key);
    }

    @Test
    void fallFromHeightAwardsWithinRange() {
        NamespacedKey key = register(RuntimeTrigger.FALL_FROM_HEIGHT,
                new AdvancementConditions.FallFromHeight(0, Double.MAX_VALUE));
        var event = new org.bukkit.event.entity.EntityDamageEvent(
                player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL, 4.0);

        new FallFromHeightTriggerHandler(registry).onDamage(event);

        verifyAwarded(key);
    }

    @Test
    void fallFromHeightIgnoresNonFallDamage() {
        register(RuntimeTrigger.FALL_FROM_HEIGHT,
                new AdvancementConditions.FallFromHeight(0, Double.MAX_VALUE));
        var event = new org.bukkit.event.entity.EntityDamageEvent(
                player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE, 4.0);

        new FallFromHeightTriggerHandler(registry).onDamage(event);

        verifyNotAwarded();
    }

    @Test
    void usedEnderEyeAwards() {
        NamespacedKey key = register(RuntimeTrigger.USED_ENDER_EYE, new AdvancementConditions.None());
        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        var event = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.RIGHT_CLICK_AIR, eye, null,
                org.bukkit.block.BlockFace.SELF, EquipmentSlot.HAND);

        new UsedEnderEyeTriggerHandler(registry).onInteract(event);

        verifyAwarded(key);
    }

    @Test
    void bredAnimalsByNonPlayerIsIgnored() {
        register(RuntimeTrigger.BRED_ANIMALS, new AdvancementConditions.BredAnimals(null, null, null));
        Cow child = (Cow) world.spawnEntity(world.getSpawnLocation(), EntityType.COW);
        Cow mother = (Cow) world.spawnEntity(world.getSpawnLocation(), EntityType.COW);
        Cow father = (Cow) world.spawnEntity(world.getSpawnLocation(), EntityType.COW);

        EntityBreedEvent event = new EntityBreedEvent(child, mother, father, mother, null, 1);
        new BredAnimalsTriggerHandler(registry).onBreed(event);

        verifyNotAwarded();
    }
}
