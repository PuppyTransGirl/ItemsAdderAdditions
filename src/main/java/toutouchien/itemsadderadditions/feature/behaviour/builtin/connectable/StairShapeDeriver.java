package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

@NullMarked
public final class StairShapeDeriver {
    private StairShapeDeriver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param findAt lookup function: (location) → CustomFurniture or null
     * @param facing canonical logical facing of {@code self}
     */
    public static StairShape derive(
            CustomFurniture self,
            FacingDirection facing,
            Function<Location, @Nullable CustomFurniture> findAt,
            Map<Location, FacingDirection> canonicalFacing
    ) {
        Location selfLoc = self.getEntity().getLocation().toBlockLocation();

        CustomFurniture front = findAt.apply(facing.offset(selfLoc));
        if (front != null) {
            FacingDirection frontFacing = resolve(front, canonicalFacing);
            if (facing.isPerpendicular(frontFacing)
                    && canTakeShape(facing, selfLoc, frontFacing.opposite(), findAt, canonicalFacing)) {
                return frontFacing == facing.counterClockWise()
                        ? StairShape.OUTER_LEFT
                        : StairShape.OUTER_RIGHT;
            }
        }

        CustomFurniture back = findAt.apply(facing.opposite().offset(selfLoc));
        if (back != null) {
            FacingDirection backFacing = resolve(back, canonicalFacing);
            if (facing.isPerpendicular(backFacing)
                    && canTakeShape(facing, selfLoc, backFacing, findAt, canonicalFacing)) {
                return backFacing == facing.counterClockWise()
                        ? StairShape.INNER_LEFT
                        : StairShape.INNER_RIGHT;
            }
        }

        boolean hasLeft = findAt.apply(facing.counterClockWise().offset(selfLoc)) != null;
        boolean hasRight = findAt.apply(facing.clockWise().offset(selfLoc)) != null;

        if (hasLeft && hasRight) return StairShape.STRAIGHT;
        if (hasLeft) return StairShape.RIGHT;
        if (hasRight) return StairShape.LEFT;
        return StairShape.DEFAULT;
    }

    private static boolean canTakeShape(
            FacingDirection selfFacing,
            Location selfLoc,
            FacingDirection face,
            Function<Location, @Nullable CustomFurniture> findAt,
            Map<Location, FacingDirection> canonicalFacing
    ) {
        CustomFurniture n = findAt.apply(face.offset(selfLoc));
        return n == null || resolve(n, canonicalFacing) != selfFacing;
    }

    private static FacingDirection resolve(CustomFurniture f, Map<Location, FacingDirection> canonicalFacing) {
        Location loc = f.getEntity().getLocation().toBlockLocation();
        FacingDirection c = canonicalFacing.get(loc);
        return c != null ? c : FacingDirection.fromYaw(f.getEntity().getYaw());
    }
}
