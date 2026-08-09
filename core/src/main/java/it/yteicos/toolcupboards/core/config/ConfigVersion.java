package it.yteicos.toolcupboards.core.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Reads and compares the {@code config-version} key that every configuration file carries.
 * <p>
 * YAML types the value on its own: {@code 1.0} arrives as a {@link Double}, {@code 1} as an
 * {@link Integer} and {@code "1.0"} as a {@link String}. Normalising in one place keeps the two
 * call sites — {@code config.yml} and every {@code blocks/*.yml} — from drifting apart.
 */
public final class ConfigVersion {

    /** The key holding the version, in every file. */
    public static final String KEY = "config-version";

    /** The version this build understands. */
    public static final String CURRENT = "1.0";

    private ConfigVersion() {
    }

    /**
     * @param section the file to inspect
     * @return the declared version in normalised form, or an empty string if the key is absent
     */
    public static String read(final ConfigurationSection section) {
        final Object raw = section.get(KEY);
        if (raw == null) {
            return "";
        }
        if (raw instanceof final Number number) {
            // 1 and 1.0 must compare equal: both normalise to "1.0".
            return Double.toString(number.doubleValue());
        }
        final String text = raw.toString().trim();
        try {
            return Double.toString(Double.parseDouble(text));
        } catch (final NumberFormatException notANumber) {
            return text;
        }
    }

    /**
     * @param section the file to inspect
     * @return {@code true} if the file declares exactly {@link #CURRENT}
     */
    public static boolean isCurrent(final ConfigurationSection section) {
        return CURRENT.equals(read(section));
    }

    /**
     * @param section the file to inspect
     * @return a human-readable complaint about the declared version, for error messages
     */
    public static String describeMismatch(final ConfigurationSection section) {
        final String found = read(section);
        if (found.isEmpty()) {
            return KEY + " is missing (expected " + CURRENT + ")";
        }
        return KEY + " is " + found + ", this build understands " + CURRENT;
    }
}
