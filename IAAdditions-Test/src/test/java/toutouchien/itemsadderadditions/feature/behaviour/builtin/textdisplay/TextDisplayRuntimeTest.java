package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.nms.api.INmsHandler;
import toutouchien.itemsadderadditions.nms.api.INmsTextDisplayHandler;
import toutouchien.itemsadderadditions.nms.api.textdisplay.*;
import toutouchien.itemsadderadditions.testsupport.FakeNms;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static toutouchien.itemsadderadditions.testsupport.MockBukkitUnsupported.failInsteadOfSkip;

class TextDisplayRuntimeTest {
    private ServerMock server;
    private WorldMock world;
    private JavaPlugin plugin;
    private INmsTextDisplayHandler textDisplays;
    private MockedStatic<FontImageWrapper> fontImages;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("TextDisplayRuntimeTest");

        INmsHandler handler = FakeNms.install();
        textDisplays = mock(INmsTextDisplayHandler.class);
        when(handler.textDisplays()).thenReturn(textDisplays);
        when(textDisplays.spawn(any(), any())).thenAnswer(invocation ->
                new PacketTextDisplayHandle(100 + invocation.hashCode(), UUID.randomUUID()));

        fontImages = mockStatic(FontImageWrapper.class);
        fontImages.when(() -> FontImageWrapper.replaceFontImages(any(Component.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        fontImages.close();
        FakeNms.uninstall();
        MockBukkit.unmock();
    }

    private static PacketTextDisplayVisual visual() {
        return new PacketTextDisplayVisual(
                PacketTextDisplayBillboard.FIXED,
                PacketTextDisplayAlignment.CENTER,
                false,
                false,
                200,
                null,
                (byte) -1,
                1f,
                1f,
                1f,
                null,
                null,
                0f,
                0f
        );
    }

    private static TextDisplaySpec spec(String id, double range, int refreshInterval) {
        return new TextDisplaySpec(
                id,
                List.of("<yellow>Hello"),
                new Vector(1, 2, 0),
                15f,
                5f,
                visual(),
                range,
                refreshInterval
        );
    }

    private TextDisplayRuntime runtime(TextDisplaySpec... specs) {
        return new TextDisplayRuntime(
                plugin,
                "test:sign",
                ItemCategory.BLOCK,
                specs.length == 0 ? List.of(spec("main", 16, 0)) : List.of(specs)
        );
    }

    private TextDisplayOwner ownerAt(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.STONE);
        return TextDisplayOwner.block("test:sign", block, 90f);
    }

    @Test
    void matchesIdAllowsRotationSuffixesOnlyForBlocks() {
        TextDisplayRuntime blockRuntime = runtime();
        TextDisplayRuntime itemRuntime = new TextDisplayRuntime(plugin, "test:sign", ItemCategory.ITEM, List.of(spec("main", 16, 0)));

        assertTrue(blockRuntime.matchesId("test:sign_north"));
        assertTrue(blockRuntime.matchesId("test:sign"));
        assertFalse(blockRuntime.matchesId("minecraft:sign_north"));
        assertFalse(itemRuntime.matchesId("test:sign_north"));
        assertTrue(itemRuntime.matchesId("test:sign"));
    }

    @Test
    void trackBeforeStartDoesNotSpawn() {
        TextDisplayRuntime runtime = runtime();
        server.addPlayer();

        runtime.track(ownerAt(0, 64, 0));

        verifyNoInteractions(textDisplays);
    }

    @Test
    void trackWhileRunningSpawnsDisplayForViewerInRange() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        runtime.start();

        runtime.track(ownerAt(0, 64, 0));

        ArgumentCaptor<PacketTextDisplay> display = ArgumentCaptor.forClass(PacketTextDisplay.class);
        verify(textDisplays).spawn(eq(viewer), display.capture());
        assertEquals(0.5, display.getValue().location().getX(), 0.0001);
        assertEquals(66.0, display.getValue().location().getY(), 0.0001);
        assertEquals(105f, display.getValue().location().getYaw(), 0.0001f);
        assertEquals(5f, display.getValue().location().getPitch(), 0.0001f);
    }

    @Test
    void outOfRangeViewerDoesNotSpawnUntilResyncedInRange() {
        TextDisplayRuntime runtime = runtime(spec("main", 4, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 100, 64, 100));
        runtime.start();
        runtime.track(ownerAt(0, 64, 0));
        verifyNoInteractions(textDisplays);

        viewer.teleport(new Location(world, 0, 64, 0));
        runtime.resync(viewer);

        verify(textDisplays).spawn(eq(viewer), any());
    }

    @Test
    void movingViewerOutOfRangeDestroysVisibleDisplay() {
        TextDisplayRuntime runtime = runtime(spec("main", 4, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        PacketTextDisplayHandle handle = new PacketTextDisplayHandle(7, UUID.randomUUID());
        when(textDisplays.spawn(any(), any())).thenReturn(handle);
        runtime.start();
        runtime.track(ownerAt(0, 64, 0));

        viewer.teleport(new Location(world, 100, 64, 100));
        runtime.resync(viewer);

        verify(textDisplays).destroy(viewer, handle);
    }

    @Test
    void untrackDestroysDisplaysForOwner() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        PacketTextDisplayHandle handle = new PacketTextDisplayHandle(8, UUID.randomUUID());
        when(textDisplays.spawn(any(), any())).thenReturn(handle);
        runtime.start();
        TextDisplayOwner owner = ownerAt(0, 64, 0);
        runtime.track(owner);

        runtime.untrack(owner.ownerId());

        verify(textDisplays).destroy(viewer, handle);
    }

    @Test
    void refreshIntervalUpdatesVisibleDisplayOnTick() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 1));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        PacketTextDisplayHandle handle = new PacketTextDisplayHandle(9, UUID.randomUUID());
        when(textDisplays.spawn(any(), any())).thenReturn(handle);
        runtime.start();
        runtime.track(ownerAt(0, 64, 0));

        server.getScheduler().performTicks(1);

        verify(textDisplays).updateMetadata(eq(viewer), eq(handle), any(), eq(visual()));
    }

    @Test
    void spawnFailureIsRetriedAfterCooldown() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        when(textDisplays.spawn(any(), any()))
                .thenThrow(new IllegalStateException("first"))
                .thenReturn(new PacketTextDisplayHandle(10, UUID.randomUUID()));
        runtime.start();

        runtime.track(ownerAt(0, 64, 0));
        runtime.resync(viewer);
        verify(textDisplays, times(1)).spawn(eq(viewer), any());

        server.getScheduler().performTicks(31);

        verify(textDisplays, atLeast(2)).spawn(eq(viewer), any());
    }

    @Test
    void destroyAllClearsOwnersAndViewerState() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        PacketTextDisplayHandle handle = new PacketTextDisplayHandle(11, UUID.randomUUID());
        when(textDisplays.spawn(any(), any())).thenReturn(handle);
        runtime.start();
        runtime.track(ownerAt(0, 64, 0));

        runtime.destroyAll();
        runtime.resync(viewer);

        verify(textDisplays).destroy(viewer, handle);
        verify(textDisplays, times(1)).spawn(eq(viewer), any());
    }

    @Test
    void forgetViewerDropsStateWithoutDestroyingPackets() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        runtime.start();
        runtime.track(ownerAt(0, 64, 0));
        clearInvocations(textDisplays);

        runtime.forgetViewer(viewer.getUniqueId());
        runtime.resync(viewer);

        verify(textDisplays).spawn(eq(viewer), any());
        verify(textDisplays, never()).destroy(eq(viewer), any());
    }

    @Test
    void untrackChunkRemovesOwnersInThatChunkOnly() {
        TextDisplayRuntime runtime = runtime(spec("main", 128, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        runtime.start();
        TextDisplayOwner first = ownerAt(1, 64, 1);
        TextDisplayOwner second = ownerAt(32, 64, 32);
        runtime.track(first);
        runtime.track(second);
        clearInvocations(textDisplays);

        runtime.untrackChunk(world.getChunkAt(0, 0));
        runtime.resync(viewer);

        verify(textDisplays, never()).spawn(eq(viewer), argThat(display ->
                display.location().getBlockX() == first.baseLocation().getBlockX()));
    }

    @Test
    @Disabled("MockBukkit PaperScheduledTask.cancel is not implemented")
    void stopPreventsFurtherSyncs() {
        TextDisplayRuntime runtime = runtime(spec("main", 16, 0));
        PlayerMock viewer = server.addPlayer();
        viewer.teleport(new Location(world, 0, 64, 0));
        runtime.start();
        failInsteadOfSkip(runtime::stop);

        runtime.track(ownerAt(0, 64, 0));
        runtime.resync(viewer);

        verifyNoInteractions(textDisplays);
    }
}
