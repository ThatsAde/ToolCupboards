package it.yteicos.toolcupboards.api.cupboard;

import it.yteicos.toolcupboards.api.claim.ClaimArea;
import it.yteicos.toolcupboards.api.upkeep.UpkeepStatus;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A placed tool cupboard: the anchor of a claim.
 * <p>
 * A cupboard must stand on a foundation block. It projects a {@link ClaimArea} that protects the
 * build blocks inside it, holds the list of players allowed to build there, and stores the
 * materials that pay for the base's upkeep.
 */
public interface ToolCupboard {

    /** @return the stable id of this cupboard */
    UUID id();

    /** @return the player who placed it and owns the authorization list */
    UUID owner();

    /** @return the cupboard's block position; the returned location is a copy */
    Location location();

    /** @return the volume this cupboard protects */
    ClaimArea area();

    /** @return the epoch millis at which the cupboard was placed */
    long placedAt();

    // --- authorization -------------------------------------------------------------------------

    /**
     * @return an unmodifiable view of the authorized players; never contains {@link #owner()},
     *         which is always implicitly authorized
     */
    Set<UUID> authorizedPlayers();

    /**
     * @param playerId the player to test
     * @return {@code true} for the owner and for anyone on the authorization list
     */
    boolean isAuthorized(UUID playerId);

    /**
     * Adds a player to the authorization list. No-op if already present or if {@code playerId} is
     * the owner.
     *
     * @param playerId the player to authorize
     */
    void authorize(UUID playerId);

    /**
     * Removes a player from the authorization list. No-op if absent.
     *
     * @param playerId the player to revoke
     */
    void unauthorize(UUID playerId);

    /** Clears the authorization list, leaving only the owner. */
    default void clearAuthorizations() {
        Set.copyOf(authorizedPlayers()).forEach(this::unauthorize);
    }

    // --- upkeep --------------------------------------------------------------------------------

    /**
     * @return the inventory holding the upkeep materials; mutating it is how players refuel a base
     */
    Inventory upkeepInventory();

    /** @return the current supply state of the base anchored to this cupboard */
    UpkeepStatus upkeepStatus();

    /**
     * @return the epoch millis of the next scheduled upkeep charge, or empty if nothing covered by
     *         this cupboard requires upkeep
     */
    Optional<Long> nextUpkeepAt();
}
