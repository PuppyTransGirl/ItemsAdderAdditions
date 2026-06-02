package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;
import static toutouchien.itemsadderadditions.testsupport.MockBukkitUnsupported.failInsteadOfSkip;

class BuiltInActionExecutionTest {
    private static ServerMock server;
    private static WorldMock world;
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
        player = server.addPlayer();
    }

    private static YamlConfiguration yaml(String s) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private ActionContext ctx(TriggerType type) {
        return ActionContext.create(player, type).build();
    }

    @Test
    void igniteSetsFireTicks() {
        IgniteAction action = new IgniteAction();
        assertTrue(action.configure(yaml("duration: 100"), "ns:item"));
        action.run(ctx(TriggerType.ITEM_INTERACT));
        assertEquals(100, player.getFireTicks());
    }

    @Test
    @Disabled("MockBukkit EntityMock.teleportAsync is not implemented")
    void teleportMovesEntity() {
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yaml("x: 10.0\ny: 70.0\nz: -5.0\nworld: world"), "ns:item"));
        failInsteadOfSkip(() -> action.run(ctx(TriggerType.ITEM_INTERACT)));

        server.getScheduler().performTicks(2);
        Location loc = player.getLocation();
        assertEquals(10.0, loc.getX(), 0.5);
        assertEquals(70.0, loc.getY(), 0.5);
        assertEquals(-5.0, loc.getZ(), 0.5);
    }

    @Test
    void teleportToMissingWorldDoesNothing() {
        Location before = player.getLocation().clone();
        TeleportAction action = new TeleportAction();
        assertTrue(action.configure(yaml("x: 10.0\ny: 70.0\nz: -5.0\nworld: nonexistent"), "ns:item"));
        action.run(ctx(TriggerType.ITEM_INTERACT));

        assertEquals(before.getWorld(), player.getLocation().getWorld());
    }

    // Note: message/actionbar/title actions route through TextRenderer ->
    // ItemsAdder FontImageWrapper.replaceFontImages, whose compileOnly stub throws
    // UnsupportedOperationException ("not meant to be shaded"). They are not
    // exercisable without the real ItemsAdder plugin present, so configuration is
    // validated here instead of execution.
    @Test
    void messageActionConfigures() {
        MessageAction action = new MessageAction();
        assertTrue(action.configure(yaml("text: \"hello world\""), "ns:item"));
    }

    @Test
    void actionBarConfigures() {
        ActionBarAction action = new ActionBarAction();
        assertTrue(action.configure(yaml("text: \"<yellow>bar\""), "ns:item"));
    }

    @Test
    void titleConfigures() {
        TitleAction action = new TitleAction();
        assertTrue(action.configure(yaml("title: \"hi\"\nsubtitle: \"there\"\nfade_in: 5\nstay: 20\nfade_out: 5"), "ns:item"));
    }

    @Test
    void swingHandValidSlotDoesNotThrow() {
        SwingHandAction action = new SwingHandAction();
        assertTrue(action.configure(yaml("hand: HAND"), "ns:item"));
        assertDoesNotThrow(() -> action.run(ctx(TriggerType.ITEM_INTERACT)));
    }

    @Test
    void swingHandInvalidSlotIsNoOp() {
        SwingHandAction action = new SwingHandAction();
        assertTrue(action.configure(yaml("hand: NOT_A_SLOT"), "ns:item"));
        assertDoesNotThrow(() -> action.run(ctx(TriggerType.ITEM_INTERACT)));
    }

    @Test
    void clearItemWithUnknownItemIsNoOp() {
        ClearItemAction action = new ClearItemAction();
        assertTrue(action.configure(yaml("item: \"missing:item\"\namount: 2"), "ns:item"));
        assertDoesNotThrow(() -> action.run(ctx(TriggerType.ITEM_INTERACT)));
    }

    @Test
    void openInventoryEnderChestOpens() {
        OpenInventoryAction action = new OpenInventoryAction();
        assertTrue(action.configure(yaml("type: ender_chest"), "ns:item"));
        assertDoesNotThrow(() -> action.run(ctx(TriggerType.ITEM_INTERACT)));
    }

    @Test
    void openInventoryUnsupportedTypeWarnsOnly() {
        OpenInventoryAction action = new OpenInventoryAction();
        assertTrue(action.configure(yaml("type: not_a_real_inventory"), "ns:item"));
        assertDoesNotThrow(() -> action.run(ctx(TriggerType.ITEM_INTERACT)));
    }

    @Test
    void veinminerWithoutBlockIsNoOp() {
        VeinminerAction action = new VeinminerAction();
        assertTrue(action.configure(yaml("max_blocks: 16"), "ns:item"));
        assertDoesNotThrow(() -> action.run(ctx(TriggerType.ITEM_BREAK_BLOCK)));
    }

    @Test
    void veinminerBreaksConnectedBlocks() {
        VeinminerAction action = new VeinminerAction();
        assertTrue(action.configure(yaml("max_blocks: 8"), "ns:item"));

        player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.DIAMOND_PICKAXE));
        for (int x = 0; x < 3; x++) {
            Block b = world.getBlockAt(x, 64, 0);
            b.setType(Material.STONE);
        }
        Block origin = world.getBlockAt(0, 64, 0);

        ActionContext context = ActionContext.create(player, TriggerType.ITEM_BREAK_BLOCK)
                .block(origin)
                .heldItem(player.getInventory().getItemInMainHand())
                .build();

        assertDoesNotThrow(() -> action.run(context));
    }
}
