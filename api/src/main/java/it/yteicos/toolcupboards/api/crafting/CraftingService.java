package it.yteicos.toolcupboards.api.crafting;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import org.bukkit.NamespacedKey;

/**
 * Registers the {@code crafting-recipe} of each block definition with the server.
 * <p>
 * V1 maps definitions onto vanilla shaped recipes, which is why {@link RecipeDefinition} is capped
 * at a 3x3 grid. A custom crafting station is planned for V2; this interface is the seam that will
 * absorb it, so third parties should register through it rather than calling Bukkit directly.
 */
public interface CraftingService {

    /**
     * Registers the definition's recipe with the server. No-op if it declares none.
     *
     * @param definition the definition whose recipe to register
     */
    void register(BuildBlockDefinition definition);

    /**
     * Removes every recipe this service registered. Called before a reload so definitions do not
     * accumulate duplicates.
     */
    void unregisterAll();

    /**
     * @param definition the definition to key
     * @return the namespaced key this service uses for that definition's recipe
     */
    NamespacedKey recipeKey(BuildBlockDefinition definition);

    /**
     * @param definition the definition to test
     * @return {@code true} if a recipe for it is currently registered with the server
     */
    boolean isRegistered(BuildBlockDefinition definition);
}
