package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.bed.SlotOffset;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BedBehaviourTest {
    @Test
    void configureRejectsNonConfigurationSections() {
        assertFalse(new BedBehaviour().configure(List.of("0,0,0"), "test:bed"));
    }

    @Test
    void configureDefaultsWhenSlotsAreMissingOrInvalid() throws Exception {
        BedBehaviour missing = new BedBehaviour();
        YamlConfiguration empty = new YamlConfiguration();

        assertTrue(missing.configure(empty, "test:bed"));
        assertEquals(List.of(new SlotOffset(0, 0, 0)), configuredSlots(missing));

        BedBehaviour invalid = new BedBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("slots", Arrays.asList("invalid", "1,not_int,0", null));

        assertTrue(invalid.configure(config, "test:bed"));
        assertEquals(List.of(new SlotOffset(0, 0, 0)), configuredSlots(invalid));
    }

    @Test
    void configureKeepsOnlyValidSlotOffsets() throws Exception {
        BedBehaviour behaviour = new BedBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("slots", List.of("1, 0, 2", "bad", "-3,4,-5"));

        assertTrue(behaviour.configure(config, "test:bed"));

        assertEquals(List.of(
                new SlotOffset(1, 0, 2),
                new SlotOffset(-3, 4, -5)
        ), configuredSlots(behaviour));
    }

    @Test
    void centredSlotRotatesLocalOffsetsByYaw() throws Exception {
        Location base = new Location(null, 10, 64, 20);
        SlotOffset offset = new SlotOffset(1, 2, 3);

        assertBlock(7, 66, 21, centredSlot(base, offset, 0));
        assertBlock(9, 66, 23, centredSlot(base, offset, 90));
        assertBlock(13, 66, 19, centredSlot(base, offset, 180));
        assertBlock(11, 66, 17, centredSlot(base, offset, 270));
        assertBlock(7, 66, 21, centredSlot(base, offset, 360));
        assertBlock(11, 66, 17, centredSlot(base, offset, -90));
    }

    @Test
    void findFreeSlotSkipsAlreadyOccupiedSlot() throws Exception {
        BedBehaviour behaviour = new BedBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("slots", List.of("0,0,0", "1,0,0"));
        assertTrue(behaviour.configure(config, "test:bed"));

        sleepers(behaviour).put(UUID.randomUUID(), new Location(null, 10.5, 64, 20.5));

        Location free = findFreeSlot(behaviour, new Location(null, 10, 64, 20), 0);

        assertNotNull(free);
        assertBlock(10, 64, 21, free);
    }

    @Test
    void slotKeysAreBlockSnappedForAllConfiguredSlots() throws Exception {
        BedBehaviour behaviour = new BedBehaviour();
        YamlConfiguration config = new YamlConfiguration();
        config.set("slots", List.of("0,0,0", "1,0,0", "0,1,1"));
        assertTrue(behaviour.configure(config, "test:bed"));

        Set<Location> keys = slotKeys(behaviour, new Location(null, 10.9, 64, 20.9), 90);

        assertEquals(Set.of(
                new Location(null, 10, 64, 20),
                new Location(null, 9, 64, 20),
                new Location(null, 10, 65, 21)
        ), keys);
    }

    @SuppressWarnings("unchecked")
    private static List<SlotOffset> configuredSlots(BedBehaviour behaviour) throws Exception {
        Field field = BedBehaviour.class.getDeclaredField("configuredSlots");
        field.setAccessible(true);
        return (List<SlotOffset>) field.get(behaviour);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Location> sleepers(BedBehaviour behaviour) throws Exception {
        Field field = BedBehaviour.class.getDeclaredField("sleepers");
        field.setAccessible(true);
        return (Map<UUID, Location>) field.get(behaviour);
    }

    private static Location centredSlot(Location base, SlotOffset offset, float yaw) throws Exception {
        Method method = BedBehaviour.class.getDeclaredMethod("centredSlot", Location.class, SlotOffset.class, float.class);
        method.setAccessible(true);
        return (Location) method.invoke(null, base, offset, yaw);
    }

    private static Location findFreeSlot(BedBehaviour behaviour, Location base, float yaw) throws Exception {
        Method method = BedBehaviour.class.getDeclaredMethod("findFreeSlot", Location.class, float.class);
        method.setAccessible(true);
        return (Location) method.invoke(behaviour, base, yaw);
    }

    @SuppressWarnings("unchecked")
    private static Set<Location> slotKeys(BedBehaviour behaviour, Location base, float yaw) throws Exception {
        Method method = BedBehaviour.class.getDeclaredMethod("slotKeys", Location.class, float.class);
        method.setAccessible(true);
        return (Set<Location>) method.invoke(behaviour, base, yaw);
    }

    private static void assertBlock(int x, int y, int z, Location location) {
        assertEquals(x, location.getBlockX());
        assertEquals(y, location.getBlockY());
        assertEquals(z, location.getBlockZ());
    }
}
