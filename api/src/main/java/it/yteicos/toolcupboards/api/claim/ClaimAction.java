package it.yteicos.toolcupboards.api.claim;

/**
 * Something a player may attempt inside a claim, asked of
 * {@link ProtectionService#allows(org.bukkit.entity.Player, org.bukkit.Location, ClaimAction)}.
 * <p>
 * Note what is <em>absent</em>: opening chests, breaking vanilla blocks, hurting entities. A claim
 * protects build blocks and the cupboard itself, nothing more — raiders are meant to break through
 * a wall and then loot freely.
 */
public enum ClaimAction {

    /** Breaking a build block that belongs to the claim. */
    BREAK_BUILD_BLOCK,

    /** Placing a build block inside the claimed volume. */
    PLACE_BUILD_BLOCK,

    /** Breaking the tool cupboard itself. */
    BREAK_CUPBOARD,

    /** Opening the cupboard's upkeep inventory. */
    OPEN_CUPBOARD,

    /** Editing the cupboard's authorization list. */
    MANAGE_AUTHORIZATIONS
}
