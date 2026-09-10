package toutouchien.itemsadderadditions.feature.action.builtin;

import com.jeff_media.customblockdata.CustomBlockData;
import dev.lone.itemsadder.api.CustomBlock;
import net.momirealms.antigrieflib.AntiGriefLib;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.common.namespace.*;
import toutouchien.itemsadderadditions.common.utils.BlocksShape;
import toutouchien.itemsadderadditions.feature.action.ActionsManager;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.loading.BehaviourBindings;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReplaceNearBlocksActionTest {
    ServerMock server;
    Plugin plugin;
    Player player;
    World world;
    AntiGriefLib protection;
    MockedStatic<CustomBlock> custom;
    MockedStatic<CustomBlockData> metadata;
    final Map<String, Block> blocks = new HashMap<>();
    final List<String> writes = new ArrayList<>();
    final List<String> reads = new ArrayList<>();

    @BeforeEach void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        player = server.addPlayer();
        world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call -> {
            int x=call.getArgument(0), y=call.getArgument(1), z=call.getArgument(2);
            reads.add(key(x,y,z));
            return block(x,y,z);
        });
        protection = mock(AntiGriefLib.class);
        when(protection.test(eq(player), any(), any())).thenReturn(true);
        custom = mockStatic(CustomBlock.class);
        metadata = mockStatic(CustomBlockData.class);
    }

    @AfterEach void cleanup() {
        ReplaceNearBlocksAction.cancelPending();
        BehaviourBindings.clear();
        NamespaceUtils.clearCustomTagRegistry();
        metadata.close(); custom.close();
        MockBukkit.unmock();
    }

    String key(int x, int y, int z) { return x+","+y+","+z; }

    Block block(int x, int y, int z) {
        return blocks.computeIfAbsent(key(x,y,z), id -> {
            Block block = mock(Block.class);
            AtomicReference<BlockData> data = new AtomicReference<>(Material.STONE.createBlockData());
            when(block.getBlockData()).thenAnswer(call -> data.get());
            when(block.getType()).thenAnswer(call -> data.get().getMaterial());
            when(block.getLocation()).thenReturn(new Location(world,x,y,z));
            when(block.getState()).thenReturn(mock(BlockState.class));
            doAnswer(call -> {
                assertTrue((boolean) call.getArgument(1), "normal physics must remain enabled");
                data.set(call.getArgument(0));
                writes.add(id);
                return null;
            }).when(block).setBlockData(any(BlockData.class), anyBoolean());
            return block;
        });
    }

    YamlConfiguration yaml(String extra) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("from: [minecraft:stone]\nto: minecraft:dirt\nradius:\n  blocks_from_center: 0\n" + extra);
        return config;
    }

    ReplaceNearBlocksAction action(String extra) throws Exception {
        ReplaceNearBlocksAction action = new ReplaceNearBlocksAction();
        assertTrue(action.configure(yaml(extra), "pack:item"));
        return action;
    }

    boolean submit(ReplaceNearBlocksAction action, Location center) {
        return BlockReplacementQueue.submit(plugin, protection, player, center, action.options);
    }

    void tick() { BlockReplacementQueue.tick(() -> 0L); }

    @Test void vanillaReplacementAndNormalPhysics() throws Exception {
        assertTrue(submit(action(""), new Location(world,0,64,0)));
        tick();
        assertEquals(List.of("0,64,0"), writes);
        assertEquals(Material.DIRT, block(0,64,0).getType());
        verify(protection).test(player, Flag.BREAK, new Location(world,0,64,0));
        verify(protection).test(player, Flag.PLACE, new Location(world,0,64,0));
    }

    @Test void schedulerRunsWork() throws Exception {
        submit(action(""), new Location(world,0,64,0));
        server.getScheduler().performTicks(3);
        assertEquals(1, writes.size());
    }

    @ParameterizedTest @ValueSource(strings={"CUBOID", "RHOMBUS", "SPHERE", "CYLINDER"})
    void cursorMatchesExistingShapeAndVisitsEachCoordinateOnce(String shape) throws Exception {
        YamlConfiguration config=yaml("shape: "+shape);
        config.set("radius.blocks_from_center", null);
        config.set("radius.x", 2); config.set("radius.y", 1); config.set("radius.z", 3);
        ReplaceNearBlocksAction action=new ReplaceNearBlocksAction();
        assertTrue(action.configure(config,"pack:item"));
        Location center=new Location(world,-0.5,63.5,-16.5);
        submit(action,center);
        for(int i=0;i<10;i++) tick();
        Set<String> expected=new HashSet<>();
        for(Location l:BlocksShape.valueOf(shape).collect(center,2,1,3))
            expected.add(key(l.getBlockX(),l.getBlockY(),l.getBlockZ()));
        assertEquals(expected,new HashSet<>(writes));
        assertEquals(writes.size(),new HashSet<>(writes).size());
        assertEquals(reads.size(),new HashSet<>(reads).size());
    }

    @Test void predicatesMatchCollectorIncludingZeroAxes() {
        for(BlocksShape shape:List.of(BlocksShape.CUBOID,BlocksShape.RHOMBUS,BlocksShape.SPHERE,BlocksShape.CYLINDER))
            for(int rx=0;rx<=3;rx++) for(int ry=0;ry<=3;ry++) for(int rz=0;rz<=3;rz++) {
                int count=0;
                for(int x=-rx;x<=rx;x++) for(int y=-ry;y<=ry;y++) for(int z=-rz;z<=rz;z++)
                    if(shape.containsBlock(x,y,z,rx,ry,rz)) count++;
                assertEquals(shape.collect(new Location(null,0,0,0),rx,ry,rz).size(),count);
            }
        assertTrue(BlocksShape.CONE.isDirectional());
        assertTrue(BlocksShape.BEAM.isDirectional());
        assertTrue(BlocksShape.PYRAMID.isDirectional());
    }

    @Test void uniformRadiusWinsAndAxesDefaultToFive() throws Exception {
        YamlConfiguration config=yaml("");
        config.set("radius.x", 25);
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction();
        assertTrue(a.configure(config,"pack:item"));
        assertEquals(0,a.options.radiusX());
        config.set("radius.blocks_from_center",null);
        config.set("radius.x",0);
        assertTrue(a.configure(config,"pack:item"));
        assertEquals(5,a.options.radiusY());
        assertEquals(5,a.options.radiusZ());
    }

    @Test void goldenLargeShapeCountsPreserveFloatingPointBoundaries() {
        BlocksShape[] shapes={BlocksShape.CUBOID,BlocksShape.RHOMBUS,BlocksShape.SPHERE,BlocksShape.CYLINDER};
        int[] radii={5,10,25,50};
        int[][] counts={{1331,231,515,891},{9261,1561,4169,6657},
                {132651,22151,65267,100011},{1030301,171785,523305,792345}};
        for(int i=0;i<radii.length;i++) for(int j=0;j<shapes.length;j++) {
            int r=radii[i], count=0;
            for(int x=-r;x<=r;x++) for(int y=-r;y<=r;y++) for(int z=-r;z<=r;z++)
                if(shapes[j].containsBlock(x,y,z,r,r,r)) count++;
            assertEquals(counts[i][j],count,shapes[j]+" radius "+r);
        }
    }

    @Test void blockMatcherNeverFallsBackToItemTags() {
        assertTrue(NamespaceUtils.matchesContentIDOrTag("minecraft:arrow","#minecraft:arrows",CustomTagType.ITEM));
        assertFalse(NamespaceUtils.matchesContentIDOrTag("minecraft:arrow","#minecraft:arrows",CustomTagType.BLOCK));
    }

    @ParameterizedTest @ValueSource(strings={"-1", "26", "2147483647", "1.5", "'5'", "true"})
    void rejectsInvalidRadii(String value) throws Exception {
        YamlConfiguration config=yaml("");
        YamlConfiguration number=new YamlConfiguration(); number.loadFromString("value: "+value);
        config.set("radius.blocks_from_center",number.get("value"));
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
    }

    @Test void rejectsOversizedBoxWithoutTruncation() throws Exception {
        YamlConfiguration config=yaml(""); config.set("radius.blocks_from_center",16);
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
    }

    @ParameterizedTest @ValueSource(strings={"from: []", "from: minecraft:stone", "from: [minecraft:stick]", "from: ['#minecraft:not_a_tag']", "from: ['#minecraft:arrows']", "from: ['pack:missing']", "to: [minecraft:stone]", "to: '#minecraft:dirt'", "to: 'bad:id:extra'", "to: minecraft:missing", "to: minecraft:chest", "shape: CONE"})
    void rejectsBadConfiguration(String override) throws Exception {
        YamlConfiguration config=yaml("");
        YamlConfiguration replacement=new YamlConfiguration(); replacement.loadFromString(override);
        for(String key:replacement.getKeys(false)) config.set(key,replacement.get(key));
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
    }

    @Test void customTargetsRejectedEvenWhenRegistered() throws Exception {
        custom.when(() -> CustomBlock.getInstance("pack:target")).thenReturn(mock(CustomBlock.class));
        YamlConfiguration config=yaml(""); config.set("to","pack:target");
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
    }

    @ParameterizedTest @ValueSource(strings={"ITEM", "FURNITURE", "RECIPE"})
    void rejectsEveryOtherCustomTagType(String type) throws Exception {
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(
                new CustomTagDefinition("pack","wrong_type",CustomTagType.valueOf(type),List.of("minecraft:stone"),"test"))));
        YamlConfiguration config=yaml(""); config.set("from",List.of("#pack:wrong_type"));
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
    }

    @Test void rejectsTooManyRulesAndAllowsAirTarget() throws Exception {
        YamlConfiguration config=yaml(""); config.set("from",Collections.nCopies(33,"minecraft:stone"));
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
        config.set("from",List.of("minecraft:stone")); config.set("to","minecraft:air");
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick();
        assertEquals(Material.AIR,block(0,64,0).getType());
    }

    @Test void blockTagsUseActualIdentityAndRejectOtherCustomTagTypes() throws Exception {
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(
                new CustomTagDefinition("pack","blocks",CustomTagType.BLOCK,List.of("pack:ore","minecraft:stone"),"test"),
                new CustomTagDefinition("pack","items",CustomTagType.ITEM,List.of("minecraft:stone"),"test"))));
        YamlConfiguration config=yaml(""); config.set("from",List.of("#pack:items"));
        assertFalse(new ReplaceNearBlocksAction().configure(config,"pack:item"));
        CustomBlock ore=mock(CustomBlock.class);
        when(ore.getNamespacedID()).thenReturn("pack:ore_north"); when(ore.remove()).thenReturn(true);
        custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(ore,ore,null);
        config.set("from",List.of("minecraft:gravel","#minecraft:dirt","#pack:blocks"));
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick();
        verify(ore).remove(); assertEquals(1,writes.size());
    }

    @Test void exactCustomIdIsRemovedThroughItemsAdder() throws Exception {
        CustomBlock ore=mock(CustomBlock.class);
        when(ore.getNamespacedID()).thenReturn("pack:ore"); when(ore.remove()).thenReturn(true);
        custom.when(() -> CustomBlock.getInstance("pack:ore")).thenReturn(ore);
        custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(ore,ore,null);
        YamlConfiguration config=yaml(""); config.set("from",List.of("pack:ore"));
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick();
        verify(ore).remove(); assertEquals(1,writes.size());
    }

    @Test void failedLookupNeverFallsBackToBackingVanillaMaterial() throws Exception {
        custom.when(() -> CustomBlock.byAlreadyPlaced(any(Block.class))).thenThrow(new IllegalStateException("IA reloading"));
        submit(action(""),new Location(world,0,64,0)); tick();
        assertTrue(writes.isEmpty());
    }

    @Test void customRemovalFailureNeverOverwritesBackingBlock() throws Exception {
        CustomBlock ore=mock(CustomBlock.class); when(ore.getNamespacedID()).thenReturn("minecraft:stone");
        custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(ore);
        submit(action(""),new Location(world,0,64,0)); tick();
        verify(ore).remove(); assertTrue(writes.isEmpty());
    }

    @Test void customStateRemainingAfterSuccessfulRemovalIsNotOverwritten() throws Exception {
        CustomBlock ore=mock(CustomBlock.class);
        when(ore.getNamespacedID()).thenReturn("pack:ore"); when(ore.remove()).thenReturn(true);
        custom.when(() -> CustomBlock.getInstance("pack:ore")).thenReturn(ore);
        custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(ore);
        YamlConfiguration config=yaml(""); config.set("from",List.of("pack:ore"));
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick();
        verify(ore).remove(); assertTrue(writes.isEmpty());
    }

    @Test void nonmatchingAndProtectedBlocksStayUnchanged() throws Exception {
        when(protection.test(eq(player),eq(Flag.PLACE),any())).thenReturn(false);
        submit(action(""),new Location(world,0,64,0)); tick(); assertTrue(writes.isEmpty());
        reset(protection);
        YamlConfiguration config=yaml(""); config.set("from",List.of("minecraft:gravel"));
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick();
        verifyNoInteractions(protection); assertTrue(writes.isEmpty());
    }

    @Test void breakDenialDoesNotRemoveCustomSource() throws Exception {
        CustomBlock source=mock(CustomBlock.class); when(source.getNamespacedID()).thenReturn("minecraft:stone");
        custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(source);
        when(protection.test(eq(player),eq(Flag.BREAK),any())).thenReturn(false);
        submit(action(""),new Location(world,0,64,0)); tick();
        verify(source,never()).remove(); verify(protection,never()).test(eq(player),eq(Flag.PLACE),any());
        assertTrue(writes.isEmpty());
    }

    @Test void protectionCallbacksCannotChangeBlockIdentityUnderTheWrite() throws Exception {
        when(protection.test(eq(player),eq(Flag.PLACE),any())).thenAnswer(call -> {
            CustomBlock source=mock(CustomBlock.class); when(source.getNamespacedID()).thenReturn("pack:new_block");
            custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(source);
            return true;
        });
        submit(action(""),new Location(world,0,64,0)); tick(); assertTrue(writes.isEmpty());
    }

    @Test void customBackingMaterialDoesNotMatchVanillaRule() throws Exception {
        CustomBlock source=mock(CustomBlock.class); when(source.getNamespacedID()).thenReturn("pack:ore");
        custom.when(() -> CustomBlock.byAlreadyPlaced(block(0,64,0))).thenReturn(source);
        submit(action(""),new Location(world,0,64,0)); tick();
        verify(source,never()).remove(); assertTrue(writes.isEmpty());
    }

    @Test void minecraftBlockTagMatchesVanillaBlock() throws Exception {
        YamlConfiguration config=yaml(""); config.set("from",List.of("#minecraft:base_stone_overworld"));
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick(); assertEquals(1,writes.size());
    }

    @Test void unloadedChunksAreNotReadAndMissingHaloPreventsMutation() throws Exception {
        when(world.isChunkLoaded(0,0)).thenReturn(false);
        submit(action(""),new Location(world,0,64,0)); tick();
        assertTrue(reads.isEmpty()); assertTrue(writes.isEmpty());
        when(world.isChunkLoaded(0,0)).thenReturn(true);
        when(world.isChunkLoaded(-1,0)).thenReturn(false);
        submit(action(""),new Location(world,0,64,0)); tick();
        assertTrue(writes.isEmpty());
        verify(world,never()).getChunkAt(anyInt(),anyInt());
        verify(world,never()).loadChunk(anyInt(),anyInt());
    }

    @Test void storageAndBlockEntitiesAreSkipped() throws Exception {
        when(block(0,64,0).getState()).thenReturn(mock(TileState.class));
        submit(action(""),new Location(world,0,64,0)); tick();
        metadata.when(() -> CustomBlockData.hasCustomBlockData(block(1,64,0),plugin)).thenReturn(true);
        submit(action(""),new Location(world,1,64,0)); tick();
        BehaviourBindings.add("minecraft:stone",mock(BehaviourExecutor.class));
        submit(action(""),new Location(world,2,64,0)); tick();
        assertTrue(writes.isEmpty());
    }

    @Test void mutationsShareOneTickCapAcrossJobs() throws Exception {
        YamlConfiguration config=yaml("shape: CUBOID"); config.set("radius.blocks_from_center",3);
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); submit(a,new Location(world,100,64,0));
        tick(); assertEquals(BlockReplacementQueue.WRITES_PER_TICK,writes.size());
        assertTrue(writes.stream().anyMatch(k -> k.startsWith("-3,")));
        assertTrue(writes.stream().anyMatch(k -> k.startsWith("97,")));
    }

    @Test void emptyScansAndDeadlineAreBounded() throws Exception {
        YamlConfiguration config=yaml("shape: CUBOID"); config.set("radius.blocks_from_center",5); config.set("from",List.of("minecraft:gravel"));
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,64,0)); tick();
        assertEquals(BlockReplacementQueue.SCANS_PER_TICK,reads.size());
        AtomicLong clock=new AtomicLong();
        BlockReplacementQueue.tick(() -> clock.getAndAdd(BlockReplacementQueue.TICK_NANOS));
        assertEquals(BlockReplacementQueue.SCANS_PER_TICK,reads.size());
    }

    @Test void chunksAreRecheckedAfterYieldAndWorldHeightIsClipped() throws Exception {
        YamlConfiguration config=yaml("shape: CUBOID"); config.set("radius.blocks_from_center",5);
        ReplaceNearBlocksAction a=new ReplaceNearBlocksAction(); assertTrue(a.configure(config,"pack:item"));
        submit(a,new Location(world,0,-64,0)); tick();
        assertEquals(32,writes.size());
        assertTrue(reads.stream().allMatch(k -> Integer.parseInt(k.split(",")[1]) >= -64));
        when(world.isChunkLoaded(anyInt(),anyInt())).thenReturn(false);
        int readCount=reads.size(); tick();
        assertEquals(readCount,reads.size()); assertEquals(32,writes.size());
    }

    @Test void reentrantCancellationPreventsMutationAndDisabledPluginRejectsNewJobs() throws Exception {
        when(protection.test(eq(player),eq(Flag.PLACE),any())).thenAnswer(call -> {
            ReplaceNearBlocksAction.cancelPending(); return true;
        });
        ReplaceNearBlocksAction a=action(""); submit(a,new Location(world,0,64,0)); tick();
        assertTrue(writes.isEmpty()); assertFalse(submit(a,new Location(world,0,64,0)));
        server.getPluginManager().disablePlugin(plugin);
        assertFalse(submit(action(""),new Location(world,0,64,0)));
    }

    @Test void reloadAndShutdownCancelPendingAndOldDelayedConfigurations() throws Exception {
        ActionsManager manager=new ActionsManager(PluginSettings.load(new YamlConfiguration()));
        ReplaceNearBlocksAction a=action(""); submit(a,new Location(world,0,64,0));
        manager.reload(List.of()); tick(); assertTrue(writes.isEmpty());
        assertFalse(submit(a,new Location(world,0,64,0)));
        submit(action(""),new Location(world,0,64,0)); manager.shutdown(); tick();
        assertTrue(writes.isEmpty());
    }

    @Test void concurrencyLimitAndRegistrationCloning() throws Exception {
        ActionsManager manager=new ActionsManager(PluginSettings.load(new YamlConfiguration()));
        var prototype=manager.registry().getPrototype("iaa_replace_near_blocks");
        assertNotNull(prototype); assertNull(manager.registry().getPrototype("replace_near_blocks"));
        var first=(ReplaceNearBlocksAction)prototype.newInstance();
        var second=(ReplaceNearBlocksAction)prototype.newInstance();
        assertTrue(first.configure(yaml(""),"pack:item")); assertNull(second.options);
        for(int i=0;i<BlockReplacementQueue.MAX_JOBS;i++) assertTrue(submit(first,new Location(world,i,64,0)));
        assertFalse(submit(first,new Location(world,9,64,0)));
        tick(); assertTrue(submit(first,new Location(world,9,64,0)));
    }
}
