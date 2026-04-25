package toutouchien.itemsadderadditions.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Each constant implements {@link #collect} to enumerate every block location
 * that falls inside the shape given a center location and per-axis radii.
 *
 * <p>Shapes are split into two categories:
 * <ul>
 *   <li><b>Symmetric</b> - {@link #CUBOID}, {@link #RHOMBUS}, {@link #SPHERE},
 *       {@link #CYLINDER}, {@link #TORUS}, {@link #SHELL} - centered on the
 *       player, independent of look direction.</li>
 *   <li><b>Directional</b> - {@link #CONE}, {@link #BEAM}, {@link #PYRAMID} -
 *       oriented along the player's look vector. Use
 *       {@link #collectBiomeQuanta(Location, int, int, int, Vector)} and pass
 *       {@code entity.getLocation().getDirection().normalize()}.</li>
 * </ul>
 *
 * <p>Radius semantics for directional shapes:
 * <ul>
 *   <li>{@code rx} - cross-sectional half-width (X/Z perpendicular to aim)</li>
 *   <li>{@code ry} - cross-sectional half-height (Y perpendicular to aim)</li>
 *   <li>{@code rz} - depth / range along the look axis</li>
 * </ul>
 */
@NullMarked
public enum BlocksShape {

    /**
     * Axis-aligned box: all blocks {@code |dx| <= rx, |dy| <= ry, |dz| <= rz}.
     */
    CUBOID {
        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            return Math.abs(dx) <= rx && Math.abs(dy) <= ry && Math.abs(dz) <= rz;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            List<Location> locations = new ArrayList<>();
            for (int dx = -rx; dx <= rx; dx++)
                for (int dy = -ry; dy <= ry; dy++)
                    for (int dz = -rz; dz <= rz; dz++)
                        locations.add(center.clone().add(dx, dy, dz));
            return locations;
        }
    },

    /**
     * Diamond / rhombus shape: blocks satisfying
     * {@code |dx|/rx + |dy|/ry + |dz|/rz <= 1} (L¹ ellipsoid).
     *
     * <p>When all radii are equal this is the classic diamond (octahedron).
     */
    RHOMBUS {
        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            return (rx > 0 ? Math.abs(dx) / rx : 0)
                    + (ry > 0 ? Math.abs(dy) / ry : 0)
                    + (rz > 0 ? Math.abs(dz) / rz : 0) <= 1.0;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            List<Location> locations = new ArrayList<>();
            for (int dx = -rx; dx <= rx; dx++)
                for (int dy = -ry; dy <= ry; dy++)
                    for (int dz = -rz; dz <= rz; dz++) {
                        double norm = (rx > 0 ? Math.abs(dx) / (double) rx : 0)
                                + (ry > 0 ? Math.abs(dy) / (double) ry : 0)
                                + (rz > 0 ? Math.abs(dz) / (double) rz : 0);
                        if (norm <= 1.0)
                            locations.add(center.clone().add(dx, dy, dz));
                    }
            return locations;
        }
    },

    /**
     * Ellipsoid / sphere: blocks satisfying
     * {@code (dx/rx)² + (dy/ry)² + (dz/rz)² <= 1}.
     *
     * <p>When all radii are equal this is a true sphere.
     */
    SPHERE {
        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            return (rx > 0 ? (dx * dx) / (rx * rx) : 0)
                    + (ry > 0 ? (dy * dy) / (ry * ry) : 0)
                    + (rz > 0 ? (dz * dz) / (rz * rz) : 0) <= 1.0;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            List<Location> locations = new ArrayList<>();
            for (int dx = -rx; dx <= rx; dx++)
                for (int dy = -ry; dy <= ry; dy++)
                    for (int dz = -rz; dz <= rz; dz++) {
                        double norm = (rx > 0 ? Math.pow(dx / (double) rx, 2) : 0)
                                + (ry > 0 ? Math.pow(dy / (double) ry, 2) : 0)
                                + (rz > 0 ? Math.pow(dz / (double) rz, 2) : 0);
                        if (norm <= 1.0)
                            locations.add(center.clone().add(dx, dy, dz));
                    }
            return locations;
        }
    },

    /**
     * Circular disc / cylinder on the XZ plane.
     * {@code ry} controls height, {@code rx}/{@code rz} control the ellipse radii.
     * Set {@code ry = 0} for a single-layer flat disc.
     */
    CYLINDER {
        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            double xzNorm = (rx > 0 ? (dx * dx) / (rx * rx) : 0)
                    + (rz > 0 ? (dz * dz) / (rz * rz) : 0);
            return xzNorm <= 1.0 && Math.abs(dy) <= ry;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            List<Location> locations = new ArrayList<>();
            for (int dx = -rx; dx <= rx; dx++)
                for (int dz = -rz; dz <= rz; dz++) {
                    double xzNorm = (rx > 0 ? Math.pow(dx / (double) rx, 2) : 0)
                            + (rz > 0 ? Math.pow(dz / (double) rz, 2) : 0);
                    if (xzNorm <= 1.0)
                        for (int dy = -ry; dy <= ry; dy++)
                            locations.add(center.clone().add(dx, dy, dz));
                }
            return locations;
        }
    },

    /**
     * Hollow spherical shell: only blocks whose distance from center falls in
     * {@code [max(rx,ry,rz) - 1, max(rx,ry,rz)]}.
     *
     * <p>Uses {@code rx}/{@code ry}/{@code rz} as the outer ellipsoid radii.
     * The shell thickness is always 1 block (one quantum layer for biome ops).
     */
    SHELL {
        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            double outer = (rx > 0 ? (dx * dx) / (rx * rx) : 0)
                    + (ry > 0 ? (dy * dy) / (ry * ry) : 0)
                    + (rz > 0 ? (dz * dz) / (rz * rz) : 0);
            // Inner radius is 1 quantum (~4 blocks) smaller per axis
            int irx = Math.max(0, rx - 4);
            int iry = Math.max(0, ry - 4);
            int irz = Math.max(0, rz - 4);
            double inner = (irx > 0 ? (dx * dx) / (irx * irx) : Double.MAX_VALUE)
                    + (iry > 0 ? (dy * dy) / (iry * iry) : Double.MAX_VALUE)
                    + (irz > 0 ? (dz * dz) / (irz * irz) : Double.MAX_VALUE);
            return outer <= 1.0 && inner > 1.0;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            List<Location> locations = new ArrayList<>();
            int irx = Math.max(0, rx - 1);
            int iry = Math.max(0, ry - 1);
            int irz = Math.max(0, rz - 1);
            for (int dx = -rx; dx <= rx; dx++)
                for (int dy = -ry; dy <= ry; dy++)
                    for (int dz = -rz; dz <= rz; dz++) {
                        double outer = (rx > 0 ? Math.pow(dx / (double) rx, 2) : 0)
                                + (ry > 0 ? Math.pow(dy / (double) ry, 2) : 0)
                                + (rz > 0 ? Math.pow(dz / (double) rz, 2) : 0);
                        double inner = (irx > 0 ? Math.pow(dx / (double) irx, 2) : Double.MAX_VALUE)
                                + (iry > 0 ? Math.pow(dy / (double) iry, 2) : Double.MAX_VALUE)
                                + (irz > 0 ? Math.pow(dz / (double) irz, 2) : Double.MAX_VALUE);
                        if (outer <= 1.0 && inner > 1.0)
                            locations.add(center.clone().add(dx, dy, dz));
                    }
            return locations;
        }
    },

    /**
     * Donut / torus lying flat on the XZ plane.
     *
     * <p>Radius semantics:
     * <ul>
     *   <li>{@code rx}/{@code rz} - distance from center to the middle of the tube ring</li>
     *   <li>{@code ry} - tube radius (thickness of the ring)</li>
     * </ul>
     */
    TORUS {
        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            // Major radius: average of rx and rz (ring center distance)
            double majorR = (rx + rz) / 2.0;
            // Distance from the point to the ring circle in XZ plane
            double distXZ = Math.sqrt(dx * dx + dz * dz);
            double toTube = Math.sqrt(Math.pow(distXZ - majorR, 2) + dy * dy);
            return toTube <= ry;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            List<Location> locations = new ArrayList<>();
            int bound = rx + ry;
            double majorR = (rx + rz) / 2.0;
            for (int dx = -bound; dx <= bound; dx++)
                for (int dy = -ry; dy <= ry; dy++)
                    for (int dz = -bound; dz <= bound; dz++) {
                        double distXZ = Math.sqrt(dx * dx + dz * dz);
                        double toTube = Math.sqrt(Math.pow(distXZ - majorR, 2) + dy * dy);
                        if (toTube <= ry)
                            locations.add(center.clone().add(dx, dy, dz));
                    }
            return locations;
        }
    },

    /**
     * Cone pointing in the player's look direction.
     *
     * <p>Radius semantics:
     * <ul>
     *   <li>{@code rx} - base radius of the cone (at maximum depth)</li>
     *   <li>{@code ry} - unused (kept for API consistency)</li>
     *   <li>{@code rz} - depth / length of the cone along look axis</li>
     * </ul>
     */
    CONE {
        @Override
        public boolean isDirectional() {
            return true;
        }

        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            if (dir == null) return false;
            // Depth along look axis
            double depth = dx * dir.getX() + dy * dir.getY() + dz * dir.getZ();
            if (depth < 0 || depth > rz) return false;
            // Perpendicular distance from look axis (tapers linearly)
            double px = dx - depth * dir.getX();
            double py = dy - depth * dir.getY();
            double pz = dz - depth * dir.getZ();
            double perp = Math.sqrt(px * px + py * py + pz * pz);
            double maxPerp = rx * (depth / rz);
            return perp <= maxPerp;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            // collect() without direction falls back to a sphere of radius rz
            return SPHERE.collect(center, rz, rz, rz);
        }
    },

    /**
     * Thin beam / ray in the player's look direction - a "biome gun".
     *
     * <p>Radius semantics:
     * <ul>
     *   <li>{@code rx} - cross-sectional ellipse half-width</li>
     *   <li>{@code ry} - cross-sectional ellipse half-height</li>
     *   <li>{@code rz} - length / range of the beam</li>
     * </ul>
     */
    BEAM {
        @Override
        public boolean isDirectional() {
            return true;
        }

        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            if (dir == null) return false;
            // Depth along look axis - must be within [0, rz]
            double depth = dx * dir.getX() + dy * dir.getY() + dz * dir.getZ();
            if (depth < 0 || depth > rz) return false;
            // Perpendicular component
            double px = dx - depth * dir.getX();
            double py = dy - depth * dir.getY();
            double pz = dz - depth * dir.getZ();
            // Check cross-section ellipse using rx/ry as half-axes
            // We project perp onto two arbitrary axes perpendicular to dir.
            // For simplicity use world-up cross dir to get a right vector,
            // then dir cross right for up vector.
            Vector up = new Vector(0, 1, 0);
            Vector right = dir.clone().crossProduct(up).normalize();
            // Handle look straight up/down: use world Z instead
            if (right.lengthSquared() < 0.001)
                right = dir.clone().crossProduct(new Vector(0, 0, 1)).normalize();
            Vector beamUp = dir.clone().crossProduct(right).normalize();
            double perpRight = px * right.getX() + py * right.getY() + pz * right.getZ();
            double perpUp = px * beamUp.getX() + py * beamUp.getY() + pz * beamUp.getZ();
            return (rx > 0 ? (perpRight * perpRight) / (rx * rx) : 0)
                    + (ry > 0 ? (perpUp * perpUp) / (ry * ry) : 0) <= 1.0;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            return CYLINDER.collect(center, rx, ry, rz);
        }
    },

    /**
     * Square-base pyramid pointing in the player's look direction.
     *
     * <p>Like {@link #CONE} but with a rectangular cross-section that tapers
     * linearly to a point at maximum depth.
     *
     * <p>Radius semantics:
     * <ul>
     *   <li>{@code rx} - half-width of the base</li>
     *   <li>{@code ry} - half-height of the base</li>
     *   <li>{@code rz} - depth / length along look axis</li>
     * </ul>
     */
    PYRAMID {
        @Override
        public boolean isDirectional() {
            return true;
        }

        @Override
        protected boolean isInsideDouble(double dx, double dy, double dz,
                                         int rx, int ry, int rz,
                                         @Nullable Vector dir) {
            if (dir == null) return false;
            double depth = dx * dir.getX() + dy * dir.getY() + dz * dir.getZ();
            if (depth < 0 || depth > rz) return false;
            double taper = depth / rz; // 0 at tip, 1 at base

            // Perpendicular component
            double px = dx - depth * dir.getX();
            double py = dy - depth * dir.getY();
            double pz = dz - depth * dir.getZ();
            Vector up = new Vector(0, 1, 0);
            Vector right = dir.clone().crossProduct(up).normalize();
            if (right.lengthSquared() < 0.001)
                right = dir.clone().crossProduct(new Vector(0, 0, 1)).normalize();

            Vector beamUp = dir.clone().crossProduct(right).normalize();
            double perpRight = Math.abs(px * right.getX() + py * right.getY() + pz * right.getZ());
            double perpUp = Math.abs(px * beamUp.getX() + py * beamUp.getY() + pz * beamUp.getZ());
            return perpRight <= rx * taper && perpUp <= ry * taper;
        }

        @Override
        public List<Location> collect(Location center, int rx, int ry, int rz) {
            return CUBOID.collect(center, rx, ry, rz);
        }
    };

    /**
     * Returns every block location that belongs to this shape centered on
     * {@code center} with the given per-axis half-extents.
     * Directional shapes fall back to a sensible symmetric approximation.
     *
     * @param center the origin (inclusive)
     * @param rx     half-extent on the X axis (≥ 0)
     * @param ry     half-extent on the Y axis (≥ 0)
     * @param rz     half-extent on the Z axis (≥ 0)
     * @return mutable list of block locations inside the shape
     */
    public abstract List<Location> collect(Location center, int rx, int ry, int rz);

    /**
     * Whether this shape requires a look direction vector.
     * Directional shapes ({@link #CONE}, {@link #BEAM}, {@link #PYRAMID}) return
     * {@code true}; all others return {@code false}.
     */
    public boolean isDirectional() {
        return false;
    }

    /**
     * Collects biome quanta for symmetric shapes.
     * Equivalent to {@link #collectBiomeQuanta(Location, int, int, int, Vector)}
     * with a {@code null} direction.
     */
    public List<Location> collectBiomeQuanta(Location center, int rx, int ry, int rz) {
        return collectBiomeQuanta(center, rx, ry, rz, null);
    }

    /**
     * Returns one representative location per biome quantum (4×4×4 block region)
     * that belongs to this shape. This is the correct method to use for biome
     * operations because Minecraft stores biomes at quantum - not block -
     * resolution.
     *
     * <p>A quantum is included when its center point falls inside the shape,
     * which gives the most faithful discretisation possible at the resolution
     * Minecraft allows.
     *
     * @param center    the origin block position
     * @param rx        half-extent / base-radius on X (≥ 0)
     * @param ry        half-extent / cross-height on Y (≥ 0)
     * @param rz        half-extent / depth on Z (≥ 0)
     * @param direction normalised look vector - required for directional shapes,
     *                  ignored (may be {@code null}) for symmetric ones
     * @return mutable list; one location per included quantum (at quantum origin)
     */
    public List<Location> collectBiomeQuanta(Location center, int rx, int ry, int rz,
                                             @Nullable Vector direction) {
        // Snap center's quantum origin (for iteration)
        int cx = (center.getBlockX() >> 2) << 2;
        int cy = (center.getBlockY() >> 2) << 2;
        int cz = (center.getBlockZ() >> 2) << 2;

        // Actual player block center (for shape tests)
        double bx = center.getBlockX() + 0.5;
        double by = center.getBlockY() + 0.5;
        double bz = center.getBlockZ() + 0.5;

        // For directional shapes the bounding box is along the look axis;
        // use rz as the depth bound and rx as the lateral bound.
        // We add +1 margin to avoid clipping edge quanta.
        int qrx, qry, qrz;
        if (isDirectional() && direction != null) {
            int maxR = Math.max(Math.max(rx, ry), rz);
            qrx = (maxR + 3) / 4 + 1;
            qry = (maxR + 3) / 4 + 1;
            qrz = (maxR + 3) / 4 + 1;
        } else {
            qrx = (rx + 3) / 4 + 1;
            qry = (ry + 3) / 4 + 1;
            qrz = (rz + 3) / 4 + 1;
        }

        List<Location> quanta = new ArrayList<>();
        for (int dqx = -qrx; dqx <= qrx; dqx++) {
            for (int dqy = -qry; dqy <= qry; dqy++) {
                for (int dqz = -qrz; dqz <= qrz; dqz++) {
                    int qox = cx + dqx * 4;
                    int qoy = cy + dqy * 4;
                    int qoz = cz + dqz * 4;

                    // Delta from player center to quantum center
                    double dx = (qox + 1.5) - bx;
                    double dy = (qoy + 1.5) - by;
                    double dz = (qoz + 1.5) - bz;

                    if (isInsideDouble(dx, dy, dz, rx, ry, rz, direction)) {
                        quanta.add(new Location(center.getWorld(), qox, qoy, qoz));
                    }
                }
            }
        }
        return quanta;
    }

    /**
     * Returns {@code true} if the point {@code (dx, dy, dz)} relative to the
     * player falls inside this shape.
     * Directional shapes use {@code dir} for orientation; symmetric shapes ignore it.
     */
    protected boolean isInsideDouble(double dx, double dy, double dz,
                                     int rx, int ry, int rz,
                                     @Nullable Vector dir) {
        return false;
    }
}
