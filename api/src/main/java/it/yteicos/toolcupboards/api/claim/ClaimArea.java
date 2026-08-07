package it.yteicos.toolcupboards.api.claim;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

/**
 * The volume a tool cupboard protects, as a shape plus a radius around a centre.
 * <p>
 * Purely geometric: it answers "is this position inside" and nothing else. Whether a player may
 * act at that position is {@link ProtectionService}'s business.
 *
 * @param center the cupboard's position; copied in and out, so callers cannot mutate the area
 * @param shape  how {@code radius} is interpreted
 * @param radius the protected radius in blocks, must be positive
 */
public record ClaimArea(Location center, ClaimShape shape, double radius) {

    public ClaimArea {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(center.getWorld(), "center world");
        if (radius <= 0.0D) {
            throw new IllegalArgumentException("radius must be positive, got " + radius);
        }
        center = center.clone();
    }

    /** @return a copy of the centre; {@link Location} is mutable, so the stored one never escapes */
    @Override
    public Location center() {
        return center.clone();
    }

    /** @return the world this area lives in */
    public World world() {
        return center.getWorld();
    }

    /**
     * @param location the position to test, may be {@code null}
     * @return {@code true} if {@code location} is in the same world and inside the volume
     */
    public boolean contains(final Location location) {
        if (location == null || !center.getWorld().equals(location.getWorld())) {
            return false;
        }
        final double dx = Math.abs(location.getX() - center.getX());
        final double dy = Math.abs(location.getY() - center.getY());
        final double dz = Math.abs(location.getZ() - center.getZ());
        return switch (shape) {
            case SPHERE -> dx * dx + dy * dy + dz * dz <= radius * radius;
            case CUBE -> dx <= radius && dy <= radius && dz <= radius;
        };
    }

    /**
     * Cheap pre-filter for spatial indexing: the axis-aligned box enclosing this area is identical
     * for both shapes, so a chunk/region lookup can use it before the exact
     * {@link #contains(Location)} test.
     *
     * @return the half-extent of the bounding box, in blocks
     */
    public double boundingRadius() {
        return radius;
    }
}
