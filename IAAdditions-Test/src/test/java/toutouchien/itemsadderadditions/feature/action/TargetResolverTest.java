package toutouchien.itemsadderadditions.feature.action;

import org.bukkit.entity.Entity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TargetResolverTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void selfMode_containsOnlyPlayer() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "self", 0, 0);

        assertEquals(1, result.size());
        assertTrue(result.contains(player));
    }

    @Test
    void selfMode_caseInsensitive() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "SELF", 0, 0);

        assertEquals(1, result.size());
        assertTrue(result.contains(player));
    }

    @Test
    void otherMode_noTarget_returnsEmpty() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "other", 0, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void otherMode_withTarget_returnsTarget() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .target(target)
                .build();

        Set<Entity> result = TargetResolver.resolve(ctx, "other", 0, 0);

        assertEquals(1, result.size());
        assertTrue(result.contains(target));
        assertFalse(result.contains(player));
    }

    @Test
    void allMode_withTarget_returnsBoth() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .target(target)
                .build();

        Set<Entity> result = TargetResolver.resolve(ctx, "all", 0, 0);

        assertEquals(2, result.size());
        assertTrue(result.contains(player));
        assertTrue(result.contains(target));
    }

    @Test
    void allMode_noTarget_returnsOnlyPlayer() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "all", 0, 0);

        assertEquals(1, result.size());
        assertTrue(result.contains(player));
    }

    @Test
    void radiusMode_zeroRadius_returnsEmpty() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "radius", 0, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void inSightMode_zeroDistance_returnsEmpty() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "in_sight", 0, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void unknownMode_returnsEmpty() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT).build();

        Set<Entity> result = TargetResolver.resolve(ctx, "invalid_mode", 0, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void otherMode_playerAndTargetAreSame_returnsOne() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .target(player)
                .build();

        Set<Entity> result = TargetResolver.resolve(ctx, "other", 0, 0);

        assertEquals(1, result.size());
        assertTrue(result.contains(player));
    }

    @Test
    void allMode_playerAndTargetAreSame_deduplicates() {
        PlayerMock player = server.addPlayer();
        ActionContext ctx = ActionContext.create(player, TriggerType.ITEM_INTERACT)
                .target(player)
                .build();

        // HashSet deduplicates the same entity reference
        Set<Entity> result = TargetResolver.resolve(ctx, "all", 0, 0);

        assertEquals(1, result.size());
    }
}
