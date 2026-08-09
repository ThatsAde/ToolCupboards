package it.yteicos.toolcupboards.core.block;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProviderRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the item players hold in their inventory for a definition, and recognises it again later.
 * <p>
 * The definition id is stamped into the item's {@code PersistentDataContainer}: it survives being
 * dropped, stored in a chest and picked back up, and it is what tells a build block apart from the
 * plain item its texture is built on. Without it, placing stone and placing a stone-textured build
 * block would be indistinguishable.
 */
public final class BuildBlockItems {

    /** Key of the stamped definition id, {@code toolcupboards:block-id}. */
    private final NamespacedKey idKey;

    private final ItemProviderRegistry providers;

    /**
     * @param plugin    owner of the namespaced key
     * @param providers resolves {@code item-type} into a real stack
     */
    public BuildBlockItems(final Plugin plugin, final ItemProviderRegistry providers) {
        this.idKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), "block-id");
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    /**
     * Builds the held item for a definition.
     *
     * @param definition the block to hand out
     * @param amount     the stack size, must be positive
     * @return the stamped stack, or empty if no available provider can resolve its item key
     */
    public Optional<ItemStack> create(final BuildBlockDefinition definition, final int amount) {
        Objects.requireNonNull(definition, "definition");
        if (amount <= 0) {
            return Optional.empty();
        }

        final ItemKey key = definition.itemModel();
        return providers.createItem(key, amount).map(stack -> {
            stack.editMeta(meta -> {
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, definition.id());
                // Nexo and MMOItems already name their own items; only plain vanilla stacks would
                // otherwise show up as "Stone".
                if (key.isVanilla()) {
                    meta.displayName(Component.text(definition.blockName())
                            .decoration(TextDecoration.ITALIC, false));
                }
            });
            return stack;
        });
    }

    /**
     * @param stack the stack to inspect, may be {@code null} or empty
     * @return the definition id stamped on it, if it is a build block item
     */
    public Optional<String> idOf(final ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.getItemMeta()
                .getPersistentDataContainer()
                .get(idKey, PersistentDataType.STRING));
    }

    /** @return the key the definition id is stamped under */
    public NamespacedKey idKey() {
        return idKey;
    }
}
