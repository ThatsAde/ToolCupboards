package it.yteicos.toolcupboards.api.item;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Bridges a single item source (vanilla, Nexo, MMOItems, ...) so the rest of the plugin can work
 * with {@link ItemKey} values without knowing which plugin backs them.
 * <p>
 * Implementations are registered in {@link ItemProviderRegistry} and must be resilient to their
 * backing plugin being absent: {@link #isAvailable()} then returns {@code false} and every lookup
 * returns {@link Optional#empty()} instead of throwing.
 * <p>
 * Third-party plugins may implement this interface to expose their own items to ToolCupboards.
 */
public interface ItemProvider {

    /**
     * @return the namespace this provider claims, lowercase (e.g. {@code nexo});
     *         must be unique across registered providers
     */
    String namespace();

    /**
     * @return {@code true} if the backing plugin is present and initialised
     */
    boolean isAvailable();

    /**
     * Builds a fresh stack for the given key.
     *
     * @param key    the item to create; its {@link ItemKey#namespace()} equals {@link #namespace()}
     * @param amount the stack size, must be positive
     * @return the created stack, or empty if the key is unknown to this provider
     */
    Optional<ItemStack> createItem(ItemKey key, int amount);

    /**
     * Tests whether an existing stack corresponds to the given key. Implementations must compare
     * by provider identity (persistent data, custom model id, ...) rather than by material alone
     * whenever the backing plugin supports it.
     *
     * @param stack the stack to test, may be {@code null} or empty
     * @param key   the expected item
     * @return {@code true} if {@code stack} is an instance of {@code key}
     */
    boolean matches(ItemStack stack, ItemKey key);

    /**
     * @param key the item to resolve
     * @return a human-readable display name for the key, if this provider knows it
     */
    default Optional<String> displayName(ItemKey key) {
        return Optional.empty();
    }
}
