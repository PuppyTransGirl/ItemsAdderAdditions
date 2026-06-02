package toutouchien.itemsadderadditions.integration.bridge;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeMachineBridgeTest {
    @BeforeEach
    void resetBridge() throws Exception {
        for (String field : new String[]{
                "handlerInstance",
                "registryField",
                "sessionMapField",
                "behaviorContainerField",
                "lookupCustomItemMethod",
                "behaviorLookupMethod",
                "openMerchantMethod",
                "publicTradeMenuApiAvailable"
        }) {
            setStatic(field, null);
        }
    }

    @Test
    void readyWhenPublicTradeMenuApiExists() {
        assertTrue(TradeMachineBridge.isReady());
    }

    @Test
    void openViaPublicApiRequiresCustomStackAndTradeMachineItem() {
        Player player = mock(Player.class);
        CustomStack customStack = mock(CustomStack.class);
        ItemStack item = ItemStack.of(Material.CHEST);
        when(customStack.getItemStack()).thenReturn(item);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:machine")).thenReturn(customStack);
            customStacks.when(() -> CustomStack.isFurnitureTradeMachine(item)).thenReturn(true);
            customStacks.when(() -> CustomStack.openTradeMenu("pack:machine", player)).thenReturn(true);

            assertTrue(TradeMachineBridge.openTradeMachine(player, "pack:machine"));

            customStacks.verify(() -> CustomStack.openTradeMenu("pack:machine", player));
        }
    }

    @Test
    void openViaPublicApiReturnsFalseForMissingStackOrNonTradeMachine() {
        Player player = mock(Player.class);
        CustomStack customStack = mock(CustomStack.class);
        ItemStack item = ItemStack.of(Material.STONE);
        when(customStack.getItemStack()).thenReturn(item);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:missing")).thenReturn(null);
            assertFalse(TradeMachineBridge.openTradeMachine(player, "pack:missing"));

            customStacks.when(() -> CustomStack.getInstance("pack:not_machine")).thenReturn(customStack);
            customStacks.when(() -> CustomStack.isFurnitureTradeMachine(item)).thenReturn(false);
            assertFalse(TradeMachineBridge.openTradeMachine(player, "pack:not_machine"));
        }
    }

    @Test
    void openRejectsNullPlayerAndBlankId() {
        Player player = mock(Player.class);

        assertThrows(IllegalArgumentException.class, () -> TradeMachineBridge.openTradeMachine(null, "pack:machine"));
        assertFalse(TradeMachineBridge.openTradeMachine(player, ""));
        assertFalse(TradeMachineBridge.openTradeMachine(player, "   "));
        assertFalse(TradeMachineBridge.openTradeMachine(player, null));
    }

    @Test
    void legacyPathRequiresCapturedHandlerWhenPublicApiUnavailable() throws Exception {
        setStatic("publicTradeMenuApiAvailable", false);
        Player player = mock(Player.class);

        assertFalse(TradeMachineBridge.isReady());
        assertThrows(IllegalStateException.class, () -> TradeMachineBridge.openTradeMachine(player, "pack:machine"));
    }

    @Test
    void captureIgnoresNullAndKeepsFirstHandler() throws Exception {
        setStatic("publicTradeMenuApiAvailable", false);
        LegacyHandler first = new LegacyHandler(new LegacyRegistry(null));
        LegacyHandler second = new LegacyHandler(new LegacyRegistry(null));

        TradeMachineBridge.capture(null);
        assertFalse(TradeMachineBridge.isReady());

        TradeMachineBridge.capture(first);
        TradeMachineBridge.capture(second);

        assertTrue(TradeMachineBridge.isReady());
        assertSame(first, staticField("handlerInstance"));
    }

    @Test
    void legacyPathOpensFurnitureTradeMachineAndStoresSession() throws Exception {
        setStatic("publicTradeMenuApiAvailable", false);
        Player player = mock(Player.class);
        ItemStack item = ItemStack.of(Material.CHEST);
        CustomStack customStack = mock(CustomStack.class);
        when(customStack.getItemStack()).thenReturn(item);

        LegacyTradeMachine tradeMachine = new LegacyTradeMachine();
        LegacyBehaviorContainer behaviors = new LegacyBehaviorContainer(tradeMachine, null);
        LegacyRegistry registry = new LegacyRegistry(new LegacyCustomItemData(behaviors));
        LegacyHandler handler = new LegacyHandler(registry);
        TradeMachineBridge.capture(handler);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:machine")).thenReturn(customStack);

            assertTrue(TradeMachineBridge.openTradeMachine(player, "pack:machine"));
        }

        assertSame(item, registry.lookedUpItem);
        assertSame(player, tradeMachine.openedFor);
        assertSame(tradeMachine, handler.vR.get(player));
    }

    @Test
    void legacyPathFallsBackToBlockTradeMachineAndHandlesMissingPieces() throws Exception {
        setStatic("publicTradeMenuApiAvailable", false);
        Player player = mock(Player.class);
        ItemStack item = ItemStack.of(Material.CHEST);
        CustomStack customStack = mock(CustomStack.class);
        when(customStack.getItemStack()).thenReturn(item);

        LegacyTradeMachine blockMachine = new LegacyTradeMachine();
        LegacyHandler handler = new LegacyHandler(new LegacyRegistry(
                new LegacyCustomItemData(new LegacyBehaviorContainer(null, blockMachine))));
        TradeMachineBridge.capture(handler);

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:block_machine")).thenReturn(customStack);
            assertTrue(TradeMachineBridge.openTradeMachine(player, "pack:block_machine"));

            customStacks.when(() -> CustomStack.getInstance("pack:missing")).thenReturn(null);
            assertFalse(TradeMachineBridge.openTradeMachine(player, "pack:missing"));
        }

        assertSame(player, blockMachine.openedFor);

        resetBridge();
        setStatic("publicTradeMenuApiAvailable", false);
        TradeMachineBridge.capture(new LegacyHandler(new LegacyRegistry(new LegacyCustomItemData(
                new LegacyBehaviorContainer(null, null)))));
        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:not_machine")).thenReturn(customStack);
            assertFalse(TradeMachineBridge.openTradeMachine(player, "pack:not_machine"));
        }
    }

    @Test
    void legacyPathWrapsReflectionFailures() throws Exception {
        setStatic("publicTradeMenuApiAvailable", false);
        Player player = mock(Player.class);
        CustomStack customStack = mock(CustomStack.class);
        when(customStack.getItemStack()).thenReturn(ItemStack.of(Material.CHEST));
        TradeMachineBridge.capture(new Object());

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.getInstance("pack:machine")).thenReturn(customStack);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> TradeMachineBridge.openTradeMachine(player, "pack:machine"));
            assertTrue(thrown.getMessage().contains("Failed to open trade machine"));
        }
    }

    private static void setStatic(String fieldName, Object value) throws Exception {
        Field field = TradeMachineBridge.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Object staticField(String fieldName) throws Exception {
        Field field = TradeMachineBridge.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    @SuppressWarnings("unused")
    static final class LegacyHandler {
        private final Object vQ;
        private final Map<Player, Object> vR = new HashMap<>();

        LegacyHandler(Object registry) {
            this.vQ = registry;
        }
    }

    @SuppressWarnings("unused")
    static final class LegacyRegistry {
        private final Object customItemData;
        private ItemStack lookedUpItem;

        LegacyRegistry(Object customItemData) {
            this.customItemData = customItemData;
        }

        Object a(ItemStack item) {
            this.lookedUpItem = item;
            return customItemData;
        }
    }

    @SuppressWarnings("unused")
    static final class LegacyCustomItemData {
        private final Object BN;

        LegacyCustomItemData(Object behaviors) {
            this.BN = behaviors;
        }
    }

    @SuppressWarnings("unused")
    static final class LegacyBehaviorContainer {
        private final Object furniture;
        private final Object block;

        LegacyBehaviorContainer(Object furniture, Object block) {
            this.furniture = furniture;
            this.block = block;
        }

        Object bg(String key) {
            if ("furniture_trade_machine".equals(key)) return furniture;
            if ("block_trade_machine".equals(key)) return block;
            return null;
        }
    }

    @SuppressWarnings("unused")
    static final class LegacyTradeMachine {
        private Player openedFor;

        void am(Player player) {
            openedFor = player;
        }
    }
}
