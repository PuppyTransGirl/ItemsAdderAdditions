package toutouchien.itemsadderadditions.behaviours.executors.connectable;

import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@NullMarked
public final class TableShapeDeriver {
    private TableShapeDeriver() {
        throw new IllegalStateException("Utility class");
    }

    public static PlacementSpec derive(
            CustomFurniture self,
            String defaultFurniture,
            String endFurniture,
            String straightFurniture,
            String cornerFurniture,
            String borderFurniture,
            String middleFurniture,
            Function<Location, @Nullable CustomFurniture> findAt
    ) {
        Location loc = self.getEntity().getLocation().toBlockLocation();

        boolean north = findAt.apply(FacingDirection.NORTH.offset(loc)) != null;
        boolean south = findAt.apply(FacingDirection.SOUTH.offset(loc)) != null;
        boolean west = findAt.apply(FacingDirection.WEST.offset(loc)) != null;
        boolean east = findAt.apply(FacingDirection.EAST.offset(loc)) != null;

        int count = b(north) + b(south) + b(west) + b(east);

        return switch (count) {
            case 0 -> new PlacementSpec(defaultFurniture, 0f);

            case 1 -> {
                float yaw = north ? FacingDirection.NORTH.toYaw()
                        : south ? FacingDirection.SOUTH.toYaw()
                          : west ? FacingDirection.WEST.toYaw()
                            : FacingDirection.EAST.toYaw();
                yield new PlacementSpec(endFurniture, yaw);
            }

            case 2 -> {
                if (north && south) yield new PlacementSpec(straightFurniture, 90f);
                if (west && east) yield new PlacementSpec(straightFurniture, 0f);

                float yaw = (north && east) ? FacingDirection.NORTH.toYaw()
                        : (north && west) ? FacingDirection.WEST.toYaw()
                          : (south && west) ? FacingDirection.SOUTH.toYaw()
                            : FacingDirection.EAST.toYaw();
                yield new PlacementSpec(cornerFurniture, yaw);
            }

            case 3 -> {
                float yaw = !north ? 0f : !south ? 180f : !west ? -90f : 90f;
                yield new PlacementSpec(borderFurniture, yaw);
            }

            default -> new PlacementSpec(middleFurniture, 0f);
        };
    }

    private static int b(boolean v) {
        return v ? 1 : 0;
    }
}
