package it.yteicos.toolcupboards.api.upkeep;

/**
 * Result of evaluating a tool cupboard's stock against the upkeep it owes.
 */
public enum UpkeepStatus {

    /** Every requirement is covered for at least one further interval. */
    SUSTAINED,

    /** Requirements are met for now, but the remaining stock is below the warning threshold. */
    RUNNING_LOW,

    /** At least one requirement could not be paid; covered blocks are decaying. */
    DECAYING,

    /** The cupboard is gone or holds nothing at all. */
    UNSUPPLIED
}
