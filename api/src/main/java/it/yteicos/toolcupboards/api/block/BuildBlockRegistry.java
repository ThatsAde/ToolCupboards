package it.yteicos.toolcupboards.api.block;

import java.util.Collection;
import java.util.Optional;

/**
 * Every {@link BuildBlockDefinition} loaded from {@code blocks/*.yml}, keyed by id.
 * <p>
 * Rebuilt wholesale on reload. Consumers must therefore look definitions up by id rather than
 * caching instances across a reload.
 */
public interface BuildBlockRegistry {

    /**
     * Adds a definition.
     *
     * @param definition the definition to register
     * @throws IllegalStateException if another definition already uses the same id
     */
    void register(BuildBlockDefinition definition);

    /**
     * @param id the identifier to look up, case-insensitive
     * @return the matching definition, if any
     */
    Optional<BuildBlockDefinition> definition(String id);

    /** @return every registered definition, in registration order */
    Collection<BuildBlockDefinition> definitions();

    /**
     * @param type the role to filter by
     * @return every definition of that type, in registration order
     */
    Collection<BuildBlockDefinition> definitionsOfType(BuildBlockType type);

    /** @return how many definitions are registered */
    int size();
}
