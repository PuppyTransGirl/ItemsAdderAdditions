package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StairShapeDeriverTest {
    private static final Location ORIGIN = new Location(null, 0, 0, 0);

    private final Map<Location, CustomFurniture> world = new HashMap<>();
    private final Map<Location, FacingDirection> canonical = new HashMap<>();

    private static CustomFurniture furnitureAt(Location loc) {
        CustomFurniture furniture = mock(CustomFurniture.class);
        Entity entity = mock(Entity.class);
        when(furniture.getEntity()).thenReturn(entity);
        when(entity.getLocation()).thenReturn(loc);
        return furniture;
    }

    private void place(FacingDirection at, FacingDirection facing) {
        Location loc = at.offset(ORIGIN.toBlockLocation());
        world.put(loc, furnitureAt(loc));
        canonical.put(loc, facing);
    }

    private StairShape derive(FacingDirection selfFacing) {
        Function<Location, CustomFurniture> findAt = world::get;
        return StairShapeDeriver.derive(furnitureAt(ORIGIN), selfFacing, findAt, canonical);
    }

    @Test
    void noNeighborsYieldsDefault() {
        assertEquals(StairShape.DEFAULT, derive(FacingDirection.NORTH));
    }

    @Test
    void leftOnlyYieldsRight() {
        place(FacingDirection.WEST, FacingDirection.NORTH); // counterClockWise of NORTH = left
        assertEquals(StairShape.RIGHT, derive(FacingDirection.NORTH));
    }

    @Test
    void rightOnlyYieldsLeft() {
        place(FacingDirection.EAST, FacingDirection.NORTH); // clockWise of NORTH = right
        assertEquals(StairShape.LEFT, derive(FacingDirection.NORTH));
    }

    @Test
    void leftAndRightYieldStraight() {
        place(FacingDirection.WEST, FacingDirection.NORTH);
        place(FacingDirection.EAST, FacingDirection.NORTH);
        assertEquals(StairShape.STRAIGHT, derive(FacingDirection.NORTH));
    }

    @Test
    void frontPerpendicularCounterClockwiseYieldsOuterLeft() {
        // front neighbor (NORTH offset) facing WEST = counterClockWise(NORTH)
        place(FacingDirection.NORTH, FacingDirection.WEST);
        assertEquals(StairShape.OUTER_LEFT, derive(FacingDirection.NORTH));
    }

    @Test
    void frontPerpendicularClockwiseYieldsOuterRight() {
        place(FacingDirection.NORTH, FacingDirection.EAST);
        assertEquals(StairShape.OUTER_RIGHT, derive(FacingDirection.NORTH));
    }

    @Test
    void backPerpendicularCounterClockwiseYieldsInnerLeft() {
        // back neighbor (SOUTH offset) facing WEST
        place(FacingDirection.SOUTH, FacingDirection.WEST);
        assertEquals(StairShape.INNER_LEFT, derive(FacingDirection.NORTH));
    }

    @Test
    void backPerpendicularClockwiseYieldsInnerRight() {
        place(FacingDirection.SOUTH, FacingDirection.EAST);
        assertEquals(StairShape.INNER_RIGHT, derive(FacingDirection.NORTH));
    }
}
