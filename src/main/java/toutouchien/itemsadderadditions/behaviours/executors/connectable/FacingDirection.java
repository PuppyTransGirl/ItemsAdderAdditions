package toutouchien.itemsadderadditions.behaviours.executors.connectable;

import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;

@NullMarked
public enum FacingDirection {
    NORTH, SOUTH, WEST, EAST;

    public FacingDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST  -> EAST;
            case EAST  -> WEST;
        };
    }

    public FacingDirection counterClockWise() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST  -> SOUTH;
            case SOUTH -> EAST;
            case EAST  -> NORTH;
        };
    }

    public FacingDirection clockWise() {
        return counterClockWise().opposite();
    }

    public Location offset(Location origin) {
        Location loc = origin.clone();
        return switch (this) {
            case NORTH -> loc.add(0, 0, -1);
            case SOUTH -> loc.add(0, 0,  1);
            case WEST  -> loc.add(-1, 0, 0);
            case EAST  -> loc.add(1,  0, 0);
        };
    }

    public static FacingDirection fromYaw(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw >= 315 || yaw < 45)  return SOUTH;
        if (yaw < 135)               return WEST;
        if (yaw < 225)               return NORTH;
        return EAST;
    }

    public float toYaw() {
        return switch (this) {
            case SOUTH -> 0f;
            case WEST  -> 90f;
            case NORTH -> 180f;
            case EAST  -> 270f;
        };
    }

    public boolean isPerpendicular(FacingDirection other) {
        boolean thisNS  = this  == NORTH || this  == SOUTH;
        boolean otherNS = other == NORTH || other == SOUTH;
        return thisNS != otherNS;
    }
}
