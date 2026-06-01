package toutouchien.itemsadderadditions.integration.valhalla;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValhallaItemApplierSerializerTest {
    @Test
    void serializeEmptyListReturnsEmptyString() {
        assertEquals("", ValhallaItemApplier.serializeStats(List.of()));
    }

    @Test
    void serializeOneStat() {
        List<ValhallaStatEntry> stats = List.of(
                new ValhallaStatEntry("CRIT_DAMAGE", 1000.0, "ADD_NUMBER", true)
        );
        assertEquals("CRIT_DAMAGE:1000.0:ADD_NUMBER:true", ValhallaItemApplier.serializeStats(stats));
    }

    @Test
    void serializeMultipleStatsJoinedBySemicolon() {
        List<ValhallaStatEntry> stats = List.of(
                new ValhallaStatEntry("GENERIC_MOVEMENT_SPEED", 0.3, "ADD_SCALAR", false),
                new ValhallaStatEntry("JUMPS", 3.0, "ADD_NUMBER", false),
                new ValhallaStatEntry("JUMP_HEIGHT", 1.0, "ADD_NUMBER", false)
        );
        assertEquals(
                "GENERIC_MOVEMENT_SPEED:0.3:ADD_SCALAR:false;JUMPS:3.0:ADD_NUMBER:false;JUMP_HEIGHT:1.0:ADD_NUMBER:false",
                ValhallaItemApplier.serializeStats(stats)
        );
    }
}
