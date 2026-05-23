package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;

class ClearItemActionTest {
    private static ServerMock server;
    private PlayerMock player;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        NamespaceUtils.initVanillaCache();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static int countMaterial(PlayerMock p, Material mat) {
        int count = 0;
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat) count += stack.getAmount();
        }
        return count;
    }

    @BeforeEach
    void freshPlayer() {
        player = server.addPlayer();
        player.getInventory().clear();
    }

    @Test
    void key_returnsClearItem() {
        assertEquals("clear_item", new ClearItemAction().key());
    }

    @Test
    void configure_missingItem_returnsFalse() {
        assertFalse(new ClearItemAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_withItem_returnsTrue() {
        assertTrue(new ClearItemAction().configure(yamlOf("item: minecraft:stone"), "test:item"));
    }

    @Test
    void configure_withItemAndAmount_returnsTrue() {
        assertTrue(new ClearItemAction().configure(yamlOf("item: minecraft:stone\namount: 3"), "test:item"));
    }

    @Test
    void run_removesVanillaItemFromInventory() {
        player.getInventory().addItem(new ItemStack(Material.STONE, 5));

        ClearItemAction action = new ClearItemAction();
        action.configure(yamlOf("item: minecraft:stone\namount: 3"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(2, countMaterial(player, Material.STONE));
    }

    @Test
    void run_removesEntireStack_whenAmountExceedsInventory() {
        player.getInventory().addItem(new ItemStack(Material.STONE, 2));

        ClearItemAction action = new ClearItemAction();
        action.configure(yamlOf("item: minecraft:stone\namount: 10"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(0, countMaterial(player, Material.STONE));
    }

    @Test
    void run_defaultAmountIsOne() {
        player.getInventory().addItem(new ItemStack(Material.STONE, 5));

        ClearItemAction action = new ClearItemAction();
        action.configure(yamlOf("item: minecraft:stone"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(4, countMaterial(player, Material.STONE));
    }

    @Test
    void run_doesNotTouchOtherMaterials() {
        player.getInventory().addItem(new ItemStack(Material.STONE, 5));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        ClearItemAction action = new ClearItemAction();
        action.configure(yamlOf("item: minecraft:stone\namount: 5"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(0, countMaterial(player, Material.STONE));
        assertEquals(5, countMaterial(player, Material.DIAMOND));
    }

    @Test
    void run_emptyInventory_doesNotThrow() {
        ClearItemAction action = new ClearItemAction();
        action.configure(yamlOf("item: minecraft:stone"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        assertDoesNotThrow(() -> action.run(ctx));
    }

    @Test
    void newInstance_returnsDistinctInstance() {
        ClearItemAction prototype = new ClearItemAction();
        ActionExecutor copy = prototype.newInstance();
        assertNotSame(prototype, copy);
        assertInstanceOf(ClearItemAction.class, copy);
    }
}
