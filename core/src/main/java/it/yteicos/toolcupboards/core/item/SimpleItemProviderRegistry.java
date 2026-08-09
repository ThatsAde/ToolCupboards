package it.yteicos.toolcupboards.core.item;

import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProvider;
import it.yteicos.toolcupboards.api.item.ItemProviderRegistry;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link ItemProviderRegistry}, keyed by namespace.
 * <p>
 * A provider whose backing plugin is missing stays registered but reports
 * {@link ItemProvider#isAvailable()} as {@code false}; the convenience methods here check that
 * before delegating, so a server without Nexo or MMOItems simply resolves nothing for those
 * namespaces instead of blowing up.
 */
public final class SimpleItemProviderRegistry implements ItemProviderRegistry {

    private final Map<String, ItemProvider> providers = new LinkedHashMap<>();

    @Override
    public void register(final ItemProvider provider) {
        Objects.requireNonNull(provider, "provider");
        final String namespace = key(provider.namespace());
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("provider namespace must not be blank");
        }
        final ItemProvider existing = providers.putIfAbsent(namespace, provider);
        if (existing != null) {
            throw new IllegalStateException("Namespace '" + namespace + "' is already claimed by "
                    + existing.getClass().getSimpleName());
        }
    }

    @Override
    public Optional<ItemProvider> provider(final String namespace) {
        return namespace == null ? Optional.empty() : Optional.ofNullable(providers.get(key(namespace)));
    }

    @Override
    public Collection<ItemProvider> providers() {
        return List.copyOf(providers.values());
    }

    @Override
    public Optional<ItemStack> createItem(final ItemKey key, final int amount) {
        if (key == null || amount <= 0) {
            return Optional.empty();
        }
        return available(key).flatMap(provider -> provider.createItem(key, amount));
    }

    @Override
    public boolean matches(final ItemStack stack, final ItemKey key) {
        if (stack == null || key == null) {
            return false;
        }
        return available(key)
                .map(provider -> provider.matches(stack, key))
                .orElse(false);
    }

    /** @return the provider claiming the key's namespace, only if its backing plugin is loaded */
    private Optional<ItemProvider> available(final ItemKey key) {
        return provider(key.namespace()).filter(ItemProvider::isAvailable);
    }

    private static String key(final String namespace) {
        return namespace.trim().toLowerCase(Locale.ROOT);
    }
}
