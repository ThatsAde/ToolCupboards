package it.yteicos.toolcupboards.core.item;

import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProvider;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * {@link ItemProvider} for plain Minecraft items. Keys look like {@code minecraft:stone}, and a key
 * written without a namespace lands here as well (see {@link ItemKey#parse(String)}).
 * <p>
 * Matching compares materials only: this provider cannot tell a plain stone from a Nexo item built
 * on stone. That is acceptable because upkeep and recipes resolve an exact {@link ItemKey} whose
 * namespace already decides which provider answers.
 */
public final class VanillaItemProvider implements ItemProvider {

    @Override
    public String namespace() {
        return ItemKey.VANILLA_NAMESPACE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<ItemStack> createItem(final ItemKey key, final int amount) {
        if (amount <= 0) {
            return Optional.empty();
        }
        return material(key).map(material -> new ItemStack(material, amount));
    }

    @Override
    public boolean matches(final ItemStack stack, final ItemKey key) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        return material(key).filter(material -> material == stack.getType()).isPresent();
    }

    @Override
    public Optional<String> displayName(final ItemKey key) {
        return material(key).map(material -> material.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    /**
     * @param key the key to resolve
     * @return the obtainable material behind the key, or empty if it names no item
     */
    private Optional<Material> material(final ItemKey key) {
        if (key == null) {
            return Optional.empty();
        }
        final Material material = Material.matchMaterial(key.toString());
        return material != null && material.isItem() ? Optional.of(material) : Optional.empty();
    }
}
