package it.yteicos.toolcupboards.core.config;

import it.yteicos.toolcupboards.api.claim.ClaimShape;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Typed view of {@code config.yml}, validated once at load time.
 * <p>
 * Every value is checked in {@link #from(ConfigurationSection)}: an invalid file makes the plugin
 * refuse to start rather than run with a silently corrected value. The {@code config-version} is
 * compared, never migrated.
 *
 * @param claimShape            how a cupboard projects its claim, {@code claim.shape}
 * @param claimRadius           claim radius in blocks, {@code claim.radius}, always positive
 * @param maxCupboardsPerPlayer cap per player, {@code claim.max-per-player}, {@link #UNLIMITED} for none
 * @param tickIntervalSeconds   how often the upkeep scheduler wakes up, always positive
 * @param decayPerMissedTick    health removed per unpaid interval, never negative
 * @param shopEnabled           global shop switch, {@code shop.enabled}
 * @param worlds                lower-case world names the plugin operates in; empty means all
 */
public record PluginSettings(
        ClaimShape claimShape,
        int claimRadius,
        int maxCupboardsPerPlayer,
        long tickIntervalSeconds,
        double decayPerMissedTick,
        boolean shopEnabled,
        Set<String> worlds
) {

    /** Sentinel for {@link #maxCupboardsPerPlayer()} meaning "no cap". */
    public static final int UNLIMITED = -1;

    public PluginSettings {
        Objects.requireNonNull(claimShape, "claim.shape");
        Objects.requireNonNull(worlds, "worlds");
        if (claimRadius <= 0) {
            throw new IllegalArgumentException("claim.radius must be positive, got " + claimRadius);
        }
        if (maxCupboardsPerPlayer < UNLIMITED) {
            throw new IllegalArgumentException(
                    "claim.max-per-player must be -1 or greater, got " + maxCupboardsPerPlayer);
        }
        if (tickIntervalSeconds <= 0L) {
            throw new IllegalArgumentException(
                    "upkeep.tick-interval-seconds must be positive, got " + tickIntervalSeconds);
        }
        if (decayPerMissedTick < 0.0D) {
            throw new IllegalArgumentException(
                    "upkeep.decay-per-missed-tick must not be negative, got " + decayPerMissedTick);
        }
        worlds = Set.copyOf(worlds);
    }

    /**
     * Reads and validates the whole file.
     *
     * @param config the loaded {@code config.yml}
     * @return the parsed settings
     * @throws IllegalStateException    if {@code config-version} is missing or unknown
     * @throws IllegalArgumentException if any value is out of range or malformed
     */
    public static PluginSettings from(final ConfigurationSection config) {
        Objects.requireNonNull(config, "config");
        if (!ConfigVersion.isCurrent(config)) {
            throw new IllegalStateException(ConfigVersion.describeMismatch(config));
        }

        final String rawShape = config.getString("claim.shape", ClaimShape.SPHERE.name());
        final ClaimShape shape;
        try {
            shape = ClaimShape.valueOf(rawShape.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    "claim.shape must be SPHERE or CUBE, got '" + rawShape + "'");
        }

        final Set<String> worlds = new LinkedHashSet<>();
        for (final String world : config.getStringList("worlds")) {
            if (world != null && !world.isBlank()) {
                worlds.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }

        return new PluginSettings(
                shape,
                config.getInt("claim.radius", 32),
                config.getInt("claim.max-per-player", UNLIMITED),
                config.getLong("upkeep.tick-interval-seconds", 60L),
                config.getDouble("upkeep.decay-per-missed-tick", 50.0D),
                config.getBoolean("shop.enabled", true),
                worlds);
    }

    /**
     * @param worldName the world to test, as returned by {@code World#getName()}
     * @return {@code true} if the plugin is active there; an empty {@code worlds} list means everywhere
     */
    public boolean enabledIn(final String worldName) {
        return worlds.isEmpty()
                || (worldName != null && worlds.contains(worldName.toLowerCase(Locale.ROOT)));
    }

    /** @return {@code true} if players may own any number of cupboards */
    public boolean cupboardsUnlimited() {
        return maxCupboardsPerPlayer == UNLIMITED;
    }

    /** @return {@link #tickIntervalSeconds()} expressed in server ticks */
    public long tickIntervalTicks() {
        return tickIntervalSeconds * 20L;
    }
}
