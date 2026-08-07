package it.yteicos.toolcupboards.api.upkeep;

import it.yteicos.toolcupboards.api.item.ItemKey;

import java.time.Duration;
import java.util.Objects;

/**
 * One maintenance cost of a build block: {@code amount} of {@code item} must be present in the
 * covering tool cupboard every {@code interval}, otherwise blocks of that definition start to decay.
 * <p>
 * Mapped from the {@code settings.required-materials} section of a block definition file:
 * {@code time-unit} + {@code frequency-check} collapse into {@link #interval()}.
 *
 * @param item     the consumed item
 * @param amount   how many units are consumed per interval, must be positive
 * @param interval how often the cost is charged, must be positive
 */
public record UpkeepRequirement(ItemKey item, int amount, Duration interval) {

    public UpkeepRequirement {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(interval, "interval");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + amount);
        }
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive, got " + interval);
        }
    }

    /** @return the cost expressed per hour, for display and comparison purposes */
    public double amountPerHour() {
        return amount * (Duration.ofHours(1).toMillis() / (double) interval.toMillis());
    }
}
