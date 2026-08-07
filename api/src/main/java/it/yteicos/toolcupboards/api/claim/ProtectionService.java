package it.yteicos.toolcupboards.api.claim;

import it.yteicos.toolcupboards.api.cupboard.ToolCupboard;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Answers the only question the listeners ever need: may this player do this here?
 * <p>
 * All claim rules live behind this interface so they stay testable without a running server. The
 * baseline rule is deliberately permissive: a position covered by no cupboard allows everything,
 * which is what makes unprotected build blocks breakable by anyone.
 */
public interface ProtectionService {

    /**
     * @param player   the acting player
     * @param location where the action happens
     * @param action   what is being attempted
     * @return {@code true} if nothing forbids the action
     */
    boolean allows(Player player, Location location, ClaimAction action);

    /**
     * @param location the position to test
     * @return the cupboard whose area covers {@code location}, if any; when several overlap, the
     *         nearest one wins
     */
    Optional<ToolCupboard> cupboardCovering(Location location);

    /**
     * @param location the position to test
     * @return every cupboard whose area covers {@code location}, nearest first
     */
    Collection<ToolCupboard> cupboardsCovering(Location location);

    /**
     * @param location the position to test
     * @return {@code true} if at least one cupboard covers {@code location}
     */
    default boolean isProtected(Location location) {
        return cupboardCovering(location).isPresent();
    }
}
