package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
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

class MiscItemActionListenerTest {
    private static ServerMock server;
    private static WorldMock world;
    private MiscItemActionListener listener;
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
        listener = new MiscItemActionListener(new ActionDispatcher());
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        ActionBindings.clear();
    }

    @Test
    void bookReadFiresOnWrittenBook() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:written_book", TriggerType.ITEM_BOOK_READ, rec);

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, book, null, BlockFace.SELF, EquipmentSlot.HAND);

        listener.onBookRead(event);

        assertEquals(1, rec.count());
    }

    @Test
    void bookReadIgnoresOffHand() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:written_book", TriggerType.ITEM_BOOK_READ, rec);

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, book, null, BlockFace.SELF, EquipmentSlot.OFF_HAND);

        listener.onBookRead(event);

        assertEquals(0, rec.count());
    }

    @Test
    void bookReadIgnoresNonBookItem() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:stick", TriggerType.ITEM_BOOK_READ, rec);

        ItemStack stick = new ItemStack(Material.STICK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_AIR, stick, null, BlockFace.SELF, EquipmentSlot.HAND);

        listener.onBookRead(event);

        assertEquals(0, rec.count());
    }

    @Test
    void bookWriteFiresFromMainHandSlot() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:writable_book", TriggerType.ITEM_BOOK_WRITE, rec);

        ItemStack writable = new ItemStack(Material.WRITABLE_BOOK);
        player.getInventory().setItem(0, writable);
        BookMeta meta = (BookMeta) new ItemStack(Material.WRITABLE_BOOK).getItemMeta();

        PlayerEditBookEvent event = new PlayerEditBookEvent(player, 0, meta, meta, false);
        listener.onBookWrite(event);

        assertEquals(1, rec.count());
    }

    @Test
    void bucketEmptyFires() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:water_bucket", TriggerType.ITEM_BUCKET_EMPTY, rec);

        Block block = world.getBlockAt(0, 64, 0);
        ItemStack bucket = new ItemStack(Material.WATER_BUCKET);
        PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(
                player, block, BlockFace.UP, Material.WATER_BUCKET, bucket);

        listener.onBucketEmpty(event);

        assertEquals(1, rec.count());
    }

    @Test
    void bucketFillFires() {
        RecordingActionExecutor rec = new RecordingActionExecutor();
        ActionBindings.add("minecraft:bucket", TriggerType.ITEM_BUCKET_FILL, rec);

        Block block = world.getBlockAt(0, 64, 0);
        ItemStack bucket = new ItemStack(Material.BUCKET);
        PlayerBucketFillEvent event = new PlayerBucketFillEvent(
                player, block, block, BlockFace.UP, Material.BUCKET, bucket);

        listener.onBucketFill(event);

        assertEquals(1, rec.count());
    }
}
