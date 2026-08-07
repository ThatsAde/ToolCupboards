package it.yteicos.toolcupboards.api.event;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Fired before a build block loses health, whether from a raider, an explosion or upkeep decay.
 * <p>
 * Cancelling leaves the block untouched. Changing {@link #setAmount(double)} scales the hit.
 */
public class BuildBlockDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BuildBlock block;
    private final Player damager;
    private final Cause cause;
    private double amount;
    private boolean cancelled;

    /**
     * @param block   the block being damaged
     * @param damager the player responsible, or {@code null} for decay and plugin damage
     * @param cause   what inflicted the damage
     * @param amount  the health about to be removed
     */
    public BuildBlockDamageEvent(@NotNull final BuildBlock block, @Nullable final Player damager,
                                 @NotNull final Cause cause, final double amount) {
        this.block = Objects.requireNonNull(block, "block");
        this.damager = damager;
        this.cause = Objects.requireNonNull(cause, "cause");
        this.amount = amount;
    }

    /** @return the block being damaged */
    public @NotNull BuildBlock block() {
        return block;
    }

    /** @return the player responsible, if the damage came from one */
    public @NotNull Optional<Player> damager() {
        return Optional.ofNullable(damager);
    }

    /** @return what inflicted the damage */
    public @NotNull Cause cause() {
        return cause;
    }

    /** @return the health about to be removed */
    public double amount() {
        return amount;
    }

    /**
     * @param amount the health to remove instead, must not be negative
     */
    public void setAmount(final double amount) {
        if (amount < 0.0D) {
            throw new IllegalArgumentException("amount must not be negative, got " + amount);
        }
        this.amount = amount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handler list, required by Bukkit */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * What inflicted damage on a build block.
     */
    public enum Cause {
        /** A player hitting the block. */
        PLAYER,
        /** An explosion, from any source. */
        EXPLOSION,
        /** Unpaid upkeep eating away at the base. */
        DECAY,
        /** Another plugin acting through the API. */
        PLUGIN
    }
}
