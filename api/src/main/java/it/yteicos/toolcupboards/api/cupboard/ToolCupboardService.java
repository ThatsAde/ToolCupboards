package it.yteicos.toolcupboards.api.cupboard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The live set of placed tool cupboards, and the only supported way to change it.
 * <p>
 * Main thread only: placing and removing a cupboard changes what is protected, which the listeners
 * read synchronously.
 */
public interface ToolCupboardService {

    /**
     * @param id the cupboard id
     * @return the matching cupboard, if it still exists
     */
    Optional<ToolCupboard> cupboard(UUID id);

    /**
     * @param location the exact block position of the cupboard
     * @return the cupboard standing there, if any
     */
    Optional<ToolCupboard> cupboardAt(Location location);

    /**
     * @param ownerId the owning player
     * @return every cupboard that player placed
     */
    Collection<ToolCupboard> cupboardsOf(UUID ownerId);

    /** @return every loaded cupboard */
    Collection<ToolCupboard> cupboards();

    /**
     * Tests the placement rules without applying them: the position must sit on a foundation
     * block, must not overlap another player's claim, and the owner must be under the
     * {@code claim.max-per-player} cap.
     *
     * @param owner    the player attempting the placement
     * @param location the target block position
     * @return the reason it would fail, or empty if the placement is legal
     */
    Optional<PlacementRefusal> refusePlacement(Player owner, Location location);

    /**
     * Places a cupboard and starts protecting its area.
     *
     * @param owner    the placing player
     * @param location the target block position
     * @return the new cupboard
     * @throws IllegalStateException if {@link #refusePlacement} would have returned a reason
     */
    ToolCupboard place(Player owner, Location location);

    /**
     * Removes a cupboard; everything it covered becomes breakable by anyone again.
     *
     * @param cupboard the cupboard to remove
     */
    void remove(ToolCupboard cupboard);

    /**
     * Why a cupboard could not be placed.
     */
    enum PlacementRefusal {

        /** The block below is not a foundation build block. */
        NOT_ON_FOUNDATION,

        /** The projected area would overlap a claim the player is not authorized in. */
        OVERLAPS_FOREIGN_CLAIM,

        /** The owner already reached {@code claim.max-per-player}. */
        LIMIT_REACHED,

        /** The world is not in the plugin's {@code worlds} list. */
        WORLD_DISABLED
    }
}
