package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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

class ItemCombatInventoryActionListenerTest {
    private static ServerMock server;
    private static WorldMock world;
    private ItemCombatInventoryActionListener listener;
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
        listener = new ItemCombatInventoryActionListener(new ActionDispatcher());
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        ActionBindings.clear();
    }

    @Test
    void itemHeldFiresHeldAndUnheld() {
        RecordingActionExecutor held = new RecordingActionExecutor();
        RecordingActionExecutor unheld = new RecordingActionExecutor();
        ActionBindings.add("minecraft:diamond_sword", TriggerType.ITEM_HELD, held);
        ActionBindings.add("minecraft:bow", TriggerType.ITEM_UNHELD, unheld);

        player.getInventory().setItem(0, new ItemStack(Material.BOW));
        player.getInventory().setItem(1, new ItemStack(Material.DIAMOND_SWORD));

        listener.onItemHeld(new PlayerItemHeldEvent(player, 0, 1));

        assertEquals(1, held.count());
        assertEquals(1, unheld.count());
    }

    @Test
    void itemHeldEmptySlotsFireNothing() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:air", TriggerType.ITEM_HELD, rec);

        listener.onItemHeld(new PlayerItemHeldEvent(player, 2, 3));

        assertEquals(0, rec.count());
    }

    @Test
    void handSwapFiresOffhandTriggers() {
        RecordingActionExecutor main = new RecordingActionExecutor();
        RecordingActionExecutor off = new RecordingActionExecutor();
        ActionBindings.add("minecraft:shield", TriggerType.ITEM_HELD_OFFHAND, main);
        ActionBindings.add("minecraft:torch", TriggerType.ITEM_UNHELD_OFFHAND, off);

        listener.onHandSwap(new PlayerSwapHandItemsEvent(player,
                new ItemStack(Material.SHIELD), new ItemStack(Material.TORCH)));

        assertEquals(1, main.count());
        assertEquals(1, off.count());
    }

    @Test
    void itemBreakFiresItemBreak() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:diamond_pickaxe", TriggerType.ITEM_BREAK, rec);

        listener.onItemBreak(new PlayerItemBreakEvent(player, new ItemStack(Material.DIAMOND_PICKAXE)));

        assertEquals(1, rec.count());
    }

    @Test
    void itemAttackFiresWhenHoldingMappedTool() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:diamond_sword", TriggerType.ITEM_ATTACK, rec);

        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        Zombie target = (Zombie) world.spawnEntity(world.getSpawnLocation(), org.bukkit.entity.EntityType.ZOMBIE);

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                player, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5.0);
        listener.onItemAttack(event);

        assertEquals(1, rec.count());
        assertEquals(target, rec.last().target());
    }

    @Test
    void itemAttackByNonPlayerIgnored() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:diamond_sword", TriggerType.ITEM_ATTACK, rec);

        Zombie damager = (Zombie) world.spawnEntity(world.getSpawnLocation(), org.bukkit.entity.EntityType.ZOMBIE);
        Zombie target = (Zombie) world.spawnEntity(world.getSpawnLocation(), org.bukkit.entity.EntityType.ZOMBIE);

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                damager, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5.0);
        listener.onItemAttack(event);

        assertEquals(0, rec.count());
    }

    @Test
    void itemDropFiresItemDrop() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:apple", TriggerType.ITEM_DROP, rec);

        Item drop = world.dropItem(world.getSpawnLocation(), new ItemStack(Material.APPLE));
        listener.onItemDrop(new org.bukkit.event.player.PlayerDropItemEvent(player, drop));

        assertEquals(1, rec.count());
    }
}
