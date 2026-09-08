package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable.ConnectableType;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConnectableBehaviourTest {
    private static ServerMock server;
    private static WorldMock world;
    private static JavaPlugin plugin;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin("ConnectableBehaviourTest");
        server.getPluginManager().enablePlugin(plugin);
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void configureRejectsNonConfigurationSections() {
        assertFalse(new ConnectableBehaviour().configure(Map.of("type", "table"), "pack:table"));
    }

    @Test
    void configureDefaultsStairVariantIdsFromBaseNamespace() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "stair");

        assertTrue(behaviour.configure(config, "pack:oak_stair"));

        assertEquals(ConnectableType.STAIR, field(behaviour, "type"));
        assertEquals("pack:oak_stair", field(behaviour, "defaultVariant"));
        assertEquals("pack:oak_stair_straight", field(behaviour, "straightVariant"));
        assertEquals("pack:oak_stair_left", field(behaviour, "leftVariant"));
        assertEquals("pack:oak_stair_right", field(behaviour, "rightVariant"));
        assertEquals("pack:oak_stair_outer", field(behaviour, "outerVariant"));
        assertEquals("pack:oak_stair_inner", field(behaviour, "innerVariant"));
    }

    @Test
    void configureResolvesBareAndNamespacedTableVariants() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "TABLE");
        config.set("default", "isolated");
        config.set("straight", "other:line");
        config.set("middle", "center");
        config.set("border", "edge");
        config.set("corner", "turn");
        config.set("end", "cap");

        assertTrue(behaviour.configure(config, "pack:table"));

        assertEquals(ConnectableType.TABLE, field(behaviour, "type"));
        assertEquals("pack:isolated", field(behaviour, "defaultVariant"));
        assertEquals("other:line", field(behaviour, "straightVariant"));
        assertEquals("pack:center", field(behaviour, "middleVariant"));
        assertEquals("pack:edge", field(behaviour, "borderVariant"));
        assertEquals("pack:turn", field(behaviour, "cornerVariant"));
        assertEquals("pack:cap", field(behaviour, "endVariant"));
    }

    @Test
    void configureTreatsUnknownTypeAsStair() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "unknown");

        assertTrue(behaviour.configure(config, "pack:connectable"));

        assertEquals(ConnectableType.STAIR, field(behaviour, "type"));
        assertEquals("pack:connectable_straight", field(behaviour, "straightVariant"));
    }

    @Test
    void breakingVariantDropsDefaultFurnitureItem() throws Exception {
        ConnectableBehaviour behaviour = new ConnectableBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", "table");
        assertTrue(behaviour.configure(config, "pack:table"));
        setField(behaviour, "host", new BehaviourHost("pack:table", ItemCategory.FURNITURE, plugin));

        Location location = new Location(world, 4, 64, 4);
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        CustomFurniture furniture = mock(CustomFurniture.class);
        when(furniture.getNamespacedID()).thenReturn("pack:table_middle");
        when(furniture.getEntity()).thenReturn(entity);
        FurnitureBreakEvent breakEvent = mock(FurnitureBreakEvent.class);
        when(breakEvent.getFurniture()).thenReturn(furniture);

        behaviour.onFurnitureRemoved(breakEvent);
        Item dropped = world.dropItem(location.clone().add(1, 0, 0), ItemStack.of(Material.CHEST));
        CustomStack variant = mock(CustomStack.class);
        when(variant.getNamespacedID()).thenReturn("pack:table_middle");
        CustomStack original = mock(CustomStack.class);
        when(original.getItemStack()).thenReturn(ItemStack.of(Material.BARREL));

        try (MockedStatic<CustomStack> customStacks = mockStatic(CustomStack.class)) {
            customStacks.when(() -> CustomStack.byItemStack(any(ItemStack.class))).thenReturn(variant);
            customStacks.when(() -> CustomStack.getInstance("pack:table")).thenReturn(original);

            behaviour.onItemSpawn(new ItemSpawnEvent(dropped));
        }

        assertEquals(Material.BARREL, dropped.getItemStack().getType());
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(ConnectableBehaviour behaviour, String name) throws Exception {
        Field field = ConnectableBehaviour.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(behaviour);
    }

    private static void setField(ConnectableBehaviour behaviour, String name, Object value) throws Exception {
        Field field = ConnectableBehaviour.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(behaviour, value);
    }
}
