package it.yteicos.toolcupboards.api.item;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry of every {@link ItemProvider} known to the plugin, keyed by namespace.
 * <p>
 * This is the single entry point for turning configuration strings into real {@link ItemStack}s.
 * Nothing outside {@code core.item} should talk to Nexo or MMOItems directly.
 */
public interface ItemProviderRegistry {

    /**
     * Registers a provider.
     *
     * @param provider the provider to add
     * @throws IllegalStateException if another provider already claims the same namespace
     */
    void register(ItemProvider provider);

    /**
     * @param namespace the namespace to look up, case-insensitive
     * @return the provider claiming {@code namespace}, if any
     */
    Optional<ItemProvider> provider(String namespace);

    /** @return every registered provider, in registration order */
    Collection<ItemProvider> providers();

    /**
     * Convenience: resolves a key through its provider.
     *
     * @param key    the item to create
     * @param amount the stack size, must be positive
     * @return the created stack, or empty if no available provider can resolve the key
     */
    Optional<ItemStack> createItem(ItemKey key, int amount);

    /**
     * Convenience: tests a stack against a key through its provider.
     *
     * @param stack the stack to test, may be {@code null} or empty
     * @param key   the expected item
     * @return {@code true} if the owning provider is available and considers the stack a match
     */
    boolean matches(ItemStack stack, ItemKey key);
}
