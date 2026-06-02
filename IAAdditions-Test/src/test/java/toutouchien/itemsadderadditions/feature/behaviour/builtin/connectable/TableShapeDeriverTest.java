package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableShapeDeriverTest {
    private static final Location ORIGIN = new Location(null, 0, 0, 0);

    private static CustomFurniture furnitureAt(Location loc) {
        CustomFurniture furniture = mock(CustomFurniture.class);
        Entity entity = mock(Entity.class);
        when(furniture.getEntity()).thenReturn(entity);
        when(entity.getLocation()).thenReturn(loc);
        return furniture;
    }

    private static Function<Location, CustomFurniture> presence(FacingDirection... occupied) {
        Set<Location> present = new HashSet<>();
        for (FacingDirection d : occupied) {
            present.add(d.offset(ORIGIN.toBlockLocation()));
        }
        CustomFurniture neighbor = mock(CustomFurniture.class);
        return loc -> present.contains(loc) ? neighbor : null;
    }

    private static PlacementSpec derive(Function<Location, CustomFurniture> findAt) {
        return TableShapeDeriver.derive(
                furnitureAt(ORIGIN),
                "default", "end", "straight", "corner", "border", "middle",
                findAt);
    }

    @Test
    void noNeighborsYieldsDefault() {
        PlacementSpec spec = derive(presence());
        assertEquals("default", spec.variantID());
        assertEquals(0f, spec.yaw());
    }

    @Test
    void singleNeighborYieldsEndFacingThatNeighbor() {
        PlacementSpec spec = derive(presence(FacingDirection.NORTH));
        assertEquals("end", spec.variantID());
        assertEquals(FacingDirection.NORTH.toYaw(), spec.yaw());
    }

    @Test
    void oppositeNeighborsYieldStraight() {
        assertEquals("straight", derive(presence(FacingDirection.NORTH, FacingDirection.SOUTH)).variantID());
        assertEquals(90f, derive(presence(FacingDirection.NORTH, FacingDirection.SOUTH)).yaw());
        assertEquals(0f, derive(presence(FacingDirection.WEST, FacingDirection.EAST)).yaw());
    }

    @Test
    void adjacentNeighborsYieldCorner() {
        PlacementSpec spec = derive(presence(FacingDirection.NORTH, FacingDirection.EAST));
        assertEquals("corner", spec.variantID());
        assertEquals(FacingDirection.NORTH.toYaw(), spec.yaw());
    }

    @Test
    void threeNeighborsYieldBorder() {
        PlacementSpec spec = derive(presence(
                FacingDirection.SOUTH, FacingDirection.WEST, FacingDirection.EAST));
        assertEquals("border", spec.variantID());
        assertEquals(0f, spec.yaw());
    }

    @Test
    void fourNeighborsYieldMiddle() {
        PlacementSpec spec = derive(presence(
                FacingDirection.NORTH, FacingDirection.SOUTH,
                FacingDirection.WEST, FacingDirection.EAST));
        assertEquals("middle", spec.variantID());
    }
}
