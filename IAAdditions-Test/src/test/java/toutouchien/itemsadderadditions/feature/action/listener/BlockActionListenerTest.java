package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

class BlockActionListenerTest {
    private static ServerMock server;
    private static WorldMock world;
    private BlockActionListener listener;
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
        listener = new BlockActionListener(new ActionDispatcher());
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        ActionBindings.clear();
    }

    @Test
    void blockBreakFiresPlacedBlockBreakForBlockId() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stone", TriggerType.PLACED_BLOCK_BREAK, rec);

        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.STONE);

        listener.onBlockBreak(new BlockBreakEvent(block, player));

        assertEquals(1, rec.count());
        assertEquals(block, rec.last().block());
    }

    @Test
    void blockBreakFiresItemBreakBlockForToolId() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:diamond_pickaxe", TriggerType.ITEM_BREAK_BLOCK, rec);

        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
        Block block = world.getBlockAt(1, 64, 0);
        block.setType(Material.STONE);

        listener.onBlockBreak(new BlockBreakEvent(block, player));

        assertEquals(1, rec.count());
        assertEquals(Material.DIAMOND_PICKAXE, rec.last().heldItem().getType());
    }

    @Test
    void blockBreakWithEmptyHandDoesNotFireItemTrigger() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:air", TriggerType.ITEM_BREAK_BLOCK, rec);

        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        Block block = world.getBlockAt(2, 64, 0);
        block.setType(Material.DIRT);

        listener.onBlockBreak(new BlockBreakEvent(block, player));

        assertEquals(0, rec.count());
    }

    @Test
    void rightClickBlockFiresBlockInteractForBlockAndItem() {
        RecordingActionExecutor blockRec = new RecordingActionExecutor();
        RecordingActionExecutor itemRec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:chest", TriggerType.BLOCK_INTERACT, blockRec);
        ActionBindings.add("minecraft:stick", TriggerType.BLOCK_INTERACT, itemRec);

        Block block = world.getBlockAt(3, 64, 0);
        block.setType(Material.CHEST);
        ItemStack held = new ItemStack(Material.STICK);
        player.getInventory().setItemInMainHand(held);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_BLOCK, held, block, org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);

        listener.onBlockInteract(event);

        assertEquals(1, blockRec.count());
        assertEquals(1, itemRec.count());
    }

    @Test
    void leftClickBlockIsIgnored() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:chest", TriggerType.BLOCK_INTERACT, rec);

        Block block = world.getBlockAt(4, 64, 0);
        block.setType(Material.CHEST);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.LEFT_CLICK_BLOCK, null, block, org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);

        listener.onBlockInteract(event);

        assertEquals(0, rec.count());
    }

    @Test
    void rightClickAirDoesNotFireBlockInteract() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stick", TriggerType.BLOCK_INTERACT, rec);

        ItemStack held = new ItemStack(Material.STICK);
        player.getInventory().setItemInMainHand(held);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, held, null, org.bukkit.block.BlockFace.SELF, EquipmentSlot.HAND);

        listener.onBlockInteract(event);

        assertEquals(0, rec.count());
    }
}
