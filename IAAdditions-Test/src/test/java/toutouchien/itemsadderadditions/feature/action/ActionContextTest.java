package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class ActionContextTest {
    private static ServerMock server;
    private static PlayerMock player;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        player = server.addPlayer();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void playerIsPreserved() {
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        assertSame(player, ctx.player());
    }

    @Test
    void triggerTypeIsPreserved() {
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_KILL).build();
        assertEquals(TriggerType.ITEM_KILL, ctx.triggerType());
    }

    @Test
    void optionalFieldsDefaultToNull() {
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();
        assertNull(ctx.block());
        assertNull(ctx.target());
        assertNull(ctx.complexFurniture());
        assertNull(ctx.heldItem());
        assertNull(ctx.eventArgument());
    }

    @Test
    void heldItemIsPreserved() {
        ItemStack item = ItemStack.of(Material.DIAMOND_SWORD);
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .heldItem(item)
                .build();
        assertSame(item, ctx.heldItem());
    }

    @Test
    void eventArgumentIsPreserved() {
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .eventArgument("right")
                .build();
        assertEquals("right", ctx.eventArgument());
    }

    @Test
    void targetIsPreserved() {
        PlayerMock target = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_ATTACK)
                .target(target)
                .build();
        assertSame(target, ctx.target());
    }

    @Test
    void complexFurnitureIsPreserved() {
        PlayerMock furnitureEntity = server.addPlayer(); // stands in for any Entity
        ActionContext ctx = ActionContext.create(player, TriggerType.COMPLEX_FURNITURE_INTERACT)
                .complexFurniture(furnitureEntity)
                .build();
        assertSame(furnitureEntity, ctx.complexFurniture());
    }

    @Test
    void builderIsFluentReturnsSameInstance() {
        ActionContext.Builder builder = ActionContext.create(player, TriggerType.ITEM_INTERACT);
        assertSame(builder, builder.heldItem(null));
        assertSame(builder, builder.eventArgument("left"));
        assertSame(builder, builder.target(null));
    }
}
