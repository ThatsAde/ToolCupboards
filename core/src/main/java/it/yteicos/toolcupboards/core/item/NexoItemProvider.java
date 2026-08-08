package it.yteicos.toolcupboards.core.item;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProvider;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * {@link ItemProvider} backed by Nexo. Keys look like {@code nexo:reinforced_concrete}.
 * <p>
 * Every Nexo class is touched behind {@link #isAvailable()}, which short-circuits on the plugin
 * manager first: on a server without Nexo the classes are never resolved and nothing throws.
 */
public final class NexoItemProvider implements ItemProvider {

    @Override
    public String namespace() {
        return "nexo";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Nexo");
    }

    @Override
    public Optional<ItemStack> createItem(final ItemKey key, final int amount) {
        if (!isAvailable() || key == null || amount <= 0) {
            return Optional.empty();
        }

        final ItemBuilder builder = NexoItems.itemFromId(key.id());
        if (builder == null) {
            return Optional.empty();
        }

        // build() may hand back the builder's cached stack: clone before touching the amount.
        final ItemStack item = builder.build().clone();
        item.setAmount(amount);
        return Optional.of(item);
    }

    @Override
    public boolean matches(final ItemStack stack, final ItemKey key) {
        if (!isAvailable() || stack == null || key == null || stack.getType().isAir()) {
            return false;
        }
        return key.id().equalsIgnoreCase(NexoItems.idFromItem(stack));
    }
}
