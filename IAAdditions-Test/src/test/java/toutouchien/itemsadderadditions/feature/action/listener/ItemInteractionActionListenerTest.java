package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;
import toutouchien.itemsadderadditions.feature.action.loading.ActionBindings;
import toutouchien.itemsadderadditions.testsupport.RecordingActionExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemInteractionActionListenerTest {
    private static ServerMock server;
    private static WorldMock world;
    private ItemInteractionActionListener listener;
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
        listener = new ItemInteractionActionListener(new ActionDispatcher());
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        ActionBindings.clear();
    }

    @Test
    void rightClickAirFiresGenericAndMainHand() {
        RecordingActionExecutor generic = new RecordingActionExecutor();
        RecordingActionExecutor mainHand = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stick", TriggerType.ITEM_INTERACT, generic);
        ActionBindings.add("minecraft:stick", TriggerType.ITEM_INTERACT_MAINHAND, mainHand);

        ItemStack item = new ItemStack(Material.STICK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.SELF, EquipmentSlot.HAND);

        listener.onItemInteract(event);

        assertEquals(1, generic.count());
        assertEquals(1, mainHand.count());
        assertEquals("right", generic.last().eventArgument());
    }

    @Test
    void sneakingProducesShiftArgument() {
        RecordingActionExecutor specific = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stick",
                toutouchien.itemsadderadditions.feature.action.TriggerKey.of(TriggerType.ITEM_INTERACT, "right_shift"),
                specific);

        player.setSneaking(true);
        ItemStack item = new ItemStack(Material.STICK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.SELF, EquipmentSlot.HAND);

        listener.onItemInteract(event);

        assertEquals(1, specific.count());
        assertEquals("right_shift", specific.last().eventArgument());
    }

    @Test
    void nonInteractActionProducesNoArgument() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stick", TriggerType.ITEM_INTERACT, rec);

        ItemStack item = new ItemStack(Material.STICK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.PHYSICAL, item, null, BlockFace.SELF, EquipmentSlot.HAND);

        listener.onItemInteract(event);

        assertEquals(0, rec.count());
    }

    @Test
    void emptyItemFiresNothing() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:air", TriggerType.ITEM_INTERACT, rec);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, null, null, BlockFace.SELF, EquipmentSlot.HAND);

        listener.onItemInteract(event);

        assertEquals(0, rec.count());
    }

    @Test
    void interactEntityFiresEntityArgument() {
        RecordingActionExecutor generic = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stick", TriggerType.ITEM_INTERACT, generic);

        player.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        Zombie target = (Zombie) world.spawnEntity(world.getSpawnLocation(), org.bukkit.entity.EntityType.ZOMBIE);

        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, target, EquipmentSlot.HAND);
        listener.onItemInteractEntity(event);

        assertEquals(1, generic.count());
        ActionContext ctx = generic.last();
        assertEquals("entity", ctx.eventArgument());
        assertEquals(target, ctx.target());
    }
}
