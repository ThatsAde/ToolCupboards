package it.yteicos.toolcupboards.api.upkeep;

import it.yteicos.toolcupboards.api.cupboard.ToolCupboard;

import java.util.List;
import java.util.Optional;

/**
 * The decay loop: periodically charges each cupboard for the blocks it covers, and eats away at
 * those blocks when it cannot pay.
 * <p>
 * This is what forces players to log in and farm, so treat it as load-bearing rather than as a
 * side feature.
 */
public interface UpkeepService {

    /**
     * @param cupboard the cupboard to inspect
     * @return the aggregated cost of everything that cupboard covers, one entry per item
     */
    List<UpkeepRequirement> requirementsOf(ToolCupboard cupboard);

    /**
     * @param cupboard the cupboard to inspect
     * @return whether its stock covers what it owes
     */
    UpkeepStatus statusOf(ToolCupboard cupboard);

    /**
     * @param cupboard the cupboard to inspect
     * @return the epoch millis of its next charge, or empty if it owes nothing
     */
    Optional<Long> nextChargeAt(ToolCupboard cupboard);

    /**
     * Charges one interval immediately: consumes what it can from the cupboard's inventory and
     * applies decay damage to the covered blocks for whatever it could not pay.
     * <p>
     * Called by the scheduler; exposed so commands and tests can force a tick.
     *
     * @param cupboard the cupboard to charge
     * @return the resulting status
     */
    UpkeepStatus charge(ToolCupboard cupboard);

    /**
     * @param cupboard the cupboard to inspect
     * @return how long the current stock will last, in milliseconds, or empty if it owes nothing;
     *         {@link Long#MAX_VALUE} when nothing it covers decays
     */
    Optional<Long> remainingMillis(ToolCupboard cupboard);
}
