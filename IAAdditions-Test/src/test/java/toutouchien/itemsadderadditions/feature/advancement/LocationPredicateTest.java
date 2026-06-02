package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocationPredicateTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        NamespaceUtils.initVanillaCache();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void parseReturnsNullForNullRaw() {
        assertNull(LocationPredicate.parse("ns", null));
    }

    @Test
    void nullWorldNeverMatches() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map("dimension", "minecraft:overworld"));
        assertNotNull(predicate);
        assertFalse(predicate.matches(new Location(null, 0, 64, 0)));
    }

    @Test
    void dimensionMatch() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map("dimension", "minecraft:overworld"));
        assertNotNull(predicate);
        assertTrue(predicate.matches(new Location(world, 0, 64, 0)));
    }

    @Test
    void dimensionMismatch() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map("dimension", "minecraft:the_nether"));
        assertNotNull(predicate);
        assertFalse(predicate.matches(new Location(world, 0, 64, 0)));
    }

    @Test
    void worldNameMismatch() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map("world", "other_world"));
        assertNotNull(predicate);
        assertFalse(predicate.matches(new Location(world, 0, 64, 0)));
    }

    @Test
    void worldNameMatch() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map("world", "world"));
        assertNotNull(predicate);
        assertTrue(predicate.matches(new Location(world, 0, 64, 0)));
    }

    @Test
    void positionRangeMatchAndMismatch() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map(
                "position", map("y", map("min", 60, "max", 70))));
        assertNotNull(predicate);
        assertTrue(predicate.matches(new Location(world, 0, 64, 0)));
        assertFalse(predicate.matches(new Location(world, 0, 100, 0)));
    }

    @Test
    void canSeeSkyConstraint() {
        LocationPredicate predicate = LocationPredicate.parse("ns", map("can_see_sky", true));
        assertNotNull(predicate);
        assertTrue(predicate.matches(new Location(world, 0, 100, 0)));
    }

    @Test
    void blockPredicateScalarMatches() {
        Location loc = new Location(world, 1, 64, 1);
        loc.getBlock().setType(Material.STONE);
        LocationPredicate predicate = LocationPredicate.parse("ns", map("block", "minecraft:stone"));
        assertNotNull(predicate);
        assertTrue(predicate.matches(loc));
    }

    @Test
    void blockPredicateScalarMismatch() {
        Location loc = new Location(world, 2, 64, 2);
        loc.getBlock().setType(Material.DIRT);
        LocationPredicate predicate = LocationPredicate.parse("ns", map("block", "minecraft:stone"));
        assertNotNull(predicate);
        assertFalse(predicate.matches(loc));
    }

    @Test
    void blockPredicateSectionWithBlocksList() {
        Location loc = new Location(world, 3, 64, 3);
        loc.getBlock().setType(Material.STONE);
        LocationPredicate predicate = LocationPredicate.parse("ns",
                map("block", map("blocks", List.of("minecraft:stone", "minecraft:cobblestone"))));
        assertNotNull(predicate);
        assertTrue(predicate.matches(loc));
    }

    @Test
    void blockPredicateParseNullForNullRaw() {
        assertNull(BlockPredicate.parse("ns", null));
    }
}
