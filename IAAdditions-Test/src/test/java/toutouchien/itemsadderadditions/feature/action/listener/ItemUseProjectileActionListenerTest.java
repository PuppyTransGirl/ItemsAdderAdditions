package toutouchien.itemsadderadditions.feature.action.listener;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;
import toutouchien.itemsadderadditions.feature.action.loading.ActionBindings;
import toutouchien.itemsadderadditions.testsupport.RecordingActionExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemUseProjectileActionListenerTest {
    private static ServerMock server;
    private static WorldMock world;
    private ItemUseProjectileActionListener listener;
    private PlayerMock player;

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
        ActionBindings.clear();
        listener = new ItemUseProjectileActionListener(new ActionDispatcher());
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        ActionBindings.clear();
    }

    @Test
    void eatingFoodFiresItemEat() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:apple", TriggerType.ITEM_EAT, rec);

        ItemStack apple = new ItemStack(Material.APPLE);
        apple.setData(DataComponentTypes.CONSUMABLE,
                Consumable.consumable().animation(ItemUseAnimation.EAT).build());
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, apple, EquipmentSlot.HAND);
        listener.onItemConsume(event);

        assertEquals(1, rec.count());
    }

    @Test
    void drinkingPotionFiresItemDrink() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:potion", TriggerType.ITEM_DRINK, rec);

        ItemStack potion = new ItemStack(Material.POTION);
        potion.setData(DataComponentTypes.CONSUMABLE,
                Consumable.consumable().animation(ItemUseAnimation.DRINK).build());
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, potion, EquipmentSlot.HAND);
        listener.onItemConsume(event);

        assertEquals(1, rec.count());
    }

    @Test
    void bowShotFiresItemBowShot() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:bow", TriggerType.ITEM_BOW_SHOT, rec);

        ItemStack bow = new ItemStack(Material.BOW);
        Arrow arrow = (Arrow) world.spawnEntity(world.getSpawnLocation(), EntityType.ARROW);

        EntityShootBowEvent event = new EntityShootBowEvent(player, bow, arrow, 1.0f);
        listener.onBowShot(event);

        assertEquals(1, rec.count());
    }

    @Test
    void projectileThrowThenHitGroundFires() {
        RecordingActionExecutor throwRec = new RecordingActionExecutor();
        RecordingActionExecutor hitRec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:snowball", TriggerType.ITEM_THROW, throwRec);
        ActionBindings.add("minecraft:snowball", TriggerType.ITEM_HIT_GROUND, hitRec);

        player.getInventory().setItemInMainHand(new ItemStack(Material.SNOWBALL));
        Snowball snowball = (Snowball) world.spawnEntity(world.getSpawnLocation(), EntityType.SNOWBALL);
        snowball.setShooter(player);

        listener.onItemThrow(new ProjectileLaunchEvent(snowball));
        assertEquals(1, throwRec.count());

        listener.onProjectileHit(new ProjectileHitEvent(snowball));
        assertEquals(1, hitRec.count());
    }

    @Test
    void projectileHitWithoutPriorThrowDoesNothing() {
        RecordingActionExecutor hitRec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:snowball", TriggerType.ITEM_HIT_GROUND, hitRec);

        Snowball snowball = (Snowball) world.spawnEntity(world.getSpawnLocation(), EntityType.SNOWBALL);
        snowball.setShooter(player);

        listener.onProjectileHit(new ProjectileHitEvent(snowball));
        assertEquals(0, hitRec.count());
    }
}
