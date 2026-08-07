package it.yteicos.toolcupboards.api.block;

import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

/**
 * A build block that exists in the world: a {@link BuildBlockDefinition} plus the mutable state
 * that placement gives it — where it stands, who put it there, how much health is left.
 * <p>
 * Health is the whole raiding loop. Blocks are not broken instantly: they absorb damage from
 * players and explosions, and lose health passively when the covering cupboard cannot pay upkeep.
 */
public interface BuildBlock {

    /** @return the stable id of this placed block */
    UUID id();

    /** @return the template this block was placed from */
    BuildBlockDefinition definition();

    /** @return the block position; the returned location is a copy */
    Location location();

    /** @return the player who placed it, or empty if it was placed by a plugin or is unknown */
    Optional<UUID> placedBy();

    /** @return the epoch millis at which the block was placed */
    long placedAt();

    /** @return the remaining health, between 0 and {@link #maxHealth()} */
    double health();

    /** @return the health of a fresh block of this definition */
    default double maxHealth() {
        return definition().maxHealth();
    }

    /** @return remaining health as a 0..1 fraction, for damage indicators */
    default double healthRatio() {
        return health() / maxHealth();
    }

    /** @return {@code true} once health has reached zero */
    default boolean destroyed() {
        return health() <= 0.0D;
    }

    /**
     * Removes health, clamped at zero. Does <em>not</em> fire events or remove the world block:
     * go through {@link BuildBlockService#damage} for that.
     *
     * @param amount the health to remove, must not be negative
     * @return the health left afterwards
     */
    double damage(double amount);

    /**
     * Restores health, clamped at {@link #maxHealth()}.
     *
     * @param amount the health to restore, must not be negative
     * @return the health afterwards
     */
    double repair(double amount);
}
