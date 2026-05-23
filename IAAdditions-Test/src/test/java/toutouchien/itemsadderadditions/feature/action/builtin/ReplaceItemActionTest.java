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

class ReplaceItemActionTest {
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

    @BeforeEach
    void freshPlayer() {
        player = server.addPlayer();
        player.getInventory().clear();
    }

    @Test
    void key_returnsReplaceItem() {
        assertEquals("replace_item", new ReplaceItemAction().key());
    }

    @Test
    void configure_missingItem_returnsFalse() {
        assertFalse(new ReplaceItemAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_withItem_returnsTrue() {
        assertTrue(new ReplaceItemAction().configure(yamlOf("item: minecraft:diamond"), "test:item"));
    }

    @Test
    void configure_withCopyFlags_returnsTrue() {
        assertTrue(new ReplaceItemAction().configure(
                yamlOf("item: minecraft:diamond\ncopy_durability: true\ncopy_enchantments: true\ncopy_pdc: true"),
                "test:item"
        ));
    }

    @Test
    void run_replacesMainHandItem() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));

        ReplaceItemAction action = new ReplaceItemAction();
        action.configure(yamlOf("item: minecraft:diamond"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(Material.DIAMOND, player.getInventory().getItemInMainHand().getType());
    }

    @Test
    void run_replacesOffHandItem() {
        player.getInventory().setItemInOffHand(new ItemStack(Material.STONE));

        ReplaceItemAction action = new ReplaceItemAction();
        action.configure(yamlOf("item: minecraft:diamond"), "test:item");

        // ITEM_INTERACT_OFFHAND triggers an offhand replacement
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT_OFFHAND).build();
        action.run(ctx);

        assertEquals(Material.DIAMOND, player.getInventory().getItemInOffHand().getType());
    }

    @Test
    void run_unknownItem_doesNotChangeMainHand() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));

        ReplaceItemAction action = new ReplaceItemAction();
        // Custom IA item - not in vanilla cache → itemByID returns null → no-op
        action.configure(yamlOf("item: myns:custom_sword"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        action.run(ctx);

        assertEquals(Material.STONE, player.getInventory().getItemInMainHand().getType());
    }

    @Test
    void run_mainhandNotAffectedByOffhandTrigger() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));
        player.getInventory().setItemInOffHand(new ItemStack(Material.GRAVEL));

        ReplaceItemAction action = new ReplaceItemAction();
        action.configure(yamlOf("item: minecraft:diamond"), "test:item");

        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT_OFFHAND).build();
        action.run(ctx);

        // Only offhand should be replaced
        assertEquals(Material.STONE, player.getInventory().getItemInMainHand().getType());
        assertEquals(Material.DIAMOND, player.getInventory().getItemInOffHand().getType());
    }

    @Test
    void newInstance_returnsDistinctInstance() {
        ReplaceItemAction prototype = new ReplaceItemAction();
        ActionExecutor copy = prototype.newInstance();
        assertNotSame(prototype, copy);
        assertInstanceOf(ReplaceItemAction.class, copy);
    }
}
