package it.yteicos.toolcupboards.core.block;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.block.BuildBlockRegistry;
import it.yteicos.toolcupboards.api.block.BuildBlockType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link BuildBlockRegistry}, rebuilt from scratch on every reload.
 * <p>
 * Backed by a {@link LinkedHashMap} because registration order is part of the contract: it is the
 * file-name order the loader hands definitions over in, and commands and the shop list them that way.
 */
public final class SimpleBuildBlockRegistry implements BuildBlockRegistry {

    private final Map<String, BuildBlockDefinition> definitions = new LinkedHashMap<>();

    @Override
    public void register(final BuildBlockDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        final String key = key(definition.id());
        final BuildBlockDefinition existing = definitions.putIfAbsent(key, definition);
        if (existing != null) {
            throw new IllegalStateException("Duplicate build block id '" + definition.id()
                    + "': keeping the definition that was loaded first.");
        }
    }

    @Override
    public Optional<BuildBlockDefinition> definition(final String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(definitions.get(key(id)));
    }

    @Override
    public Collection<BuildBlockDefinition> definitions() {
        return List.copyOf(definitions.values());
    }

    @Override
    public Collection<BuildBlockDefinition> definitionsOfType(final BuildBlockType type) {
        if (type == null) {
            return List.of();
        }
        return definitions.values().stream()
                .filter(definition -> definition.type() == type)
                .toList();
    }

    @Override
    public int size() {
        return definitions.size();
    }

    /**
     * Drops every definition. Called before a reload so stale ids cannot survive it.
     */
    public void clear() {
        definitions.clear();
    }

    private static String key(final String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
