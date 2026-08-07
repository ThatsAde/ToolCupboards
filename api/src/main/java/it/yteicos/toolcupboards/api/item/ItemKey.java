package it.yteicos.toolcupboards.api.item;

import java.util.Locale;
import java.util.Objects;

/**
 * A namespaced reference to an item, used everywhere the configuration needs to point at
 * something that may not be a vanilla Minecraft item.
 * <p>
 * The textual form is {@code namespace:id}. A missing namespace defaults to
 * {@value #VANILLA_NAMESPACE}.
 * <p>
 * Examples: {@code minecraft:stone}, {@code nexo:reinforced_plate}, {@code mmoitems:MATERIAL.STEEL}.
 *
 * @param namespace the provider namespace, lowercase, never blank
 * @param id        the provider-specific identifier, never blank
 */
public record ItemKey(String namespace, String id) {

    /** Namespace handled by the built-in vanilla provider. */
    public static final String VANILLA_NAMESPACE = "minecraft";

    public ItemKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(id, "id");
        namespace = namespace.trim().toLowerCase(Locale.ROOT);
        id = id.trim();
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    /**
     * Parses a configuration string into a key.
     *
     * @param raw the raw value, e.g. {@code nexo:foo} or {@code stone}
     * @return the parsed key
     * @throws IllegalArgumentException if {@code raw} is blank or malformed
     */
    public static ItemKey parse(final String raw) {
        Objects.requireNonNull(raw, "raw");
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("item key must not be blank");
        }
        final int separator = trimmed.indexOf(':');
        if (separator < 0) {
            return new ItemKey(VANILLA_NAMESPACE, trimmed);
        }
        return new ItemKey(trimmed.substring(0, separator), trimmed.substring(separator + 1));
    }

    /** @return {@code true} if this key is handled by the vanilla provider */
    public boolean isVanilla() {
        return VANILLA_NAMESPACE.equals(namespace);
    }

    @Override
    public String toString() {
        return namespace + ':' + id;
    }
}
