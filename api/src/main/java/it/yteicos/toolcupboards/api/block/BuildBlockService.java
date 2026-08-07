package it.yteicos.toolcupboards.api.block;

import it.yteicos.toolcupboards.api.claim.ClaimArea;
import it.yteicos.toolcupboards.api.event.BuildBlockDamageEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The live set of placed build blocks, and the only supported way to change it.
 * <p>
 * Every method here must be called from the main thread: they touch the world and fire events.
 */
public interface BuildBlockService {

    /**
     * @param location the block position to look up
     * @return the build block standing there, or empty if the position holds a vanilla block
     */
    Optional<BuildBlock> blockAt(Location location);

    /**
     * Tests the placement rules without applying them: a {@link BuildBlockType#STRUCTURE} block
     * needs a foundation underneath it, directly or transitively.
     *
     * @param definition what is being placed
     * @param location   where it would go
     * @return {@code true} if the placement is legal
     */
    boolean canPlace(BuildBlockDefinition definition, Location location);

    /**
     * Registers a build block at a position and updates the world accordingly.
     *
     * @param definition the template to place
     * @param location   the target block position
     * @param placedBy   the responsible player, or {@code null} for plugin placement
     * @return the newly placed block
     * @throws IllegalStateException if {@link #canPlace} would have returned {@code false}
     */
    BuildBlock place(BuildBlockDefinition definition, Location location, @Nullable UUID placedBy);

    /**
     * Runs the full damage pipeline: fires {@link BuildBlockDamageEvent}, applies the (possibly
     * modified) amount, and destroys the block if its health hits zero.
     *
     * @param block   the block to hit
     * @param amount  the health to remove, must not be negative
     * @param cause   what inflicted the damage
     * @param damager the responsible player, or {@code null} for decay and plugin damage
     * @return the health left, or 0 if the block was destroyed; negative if the event was cancelled
     */
    double damage(BuildBlock block, double amount, BuildBlockDamageEvent.Cause cause, @Nullable Player damager);

    /**
     * Removes a block from the world and from persistence, regardless of its health.
     *
     * @param block the block to remove
     */
    void destroy(BuildBlock block);

    /**
     * @param area the volume to query
     * @return every build block inside {@code area}
     */
    Collection<BuildBlock> blocksIn(ClaimArea area);

    /** @return how many build blocks are currently loaded */
    int count();
}
