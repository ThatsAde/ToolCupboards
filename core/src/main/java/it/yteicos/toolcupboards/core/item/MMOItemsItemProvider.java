package it.yteicos.toolcupboards.core.item;

import io.lumine.mythic.lib.api.item.NBTItem;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProvider;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * {@link ItemProvider} backed by MMOItems. Keys look like {@code mmoitems:TYPE.ID}.
 * <p>
 * MMOItems indexes both types and templates by their upper-case id, with no normalisation on
 * lookup, so both halves of the key are upper-cased here before they are handed over.
 */
public final class MMOItemsItemProvider implements ItemProvider {

    @Override
    public String namespace() {
        return "mmoitems";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOItems") && MMOItems.plugin != null;
    }

    @Override
    public Optional<ItemStack> createItem(final ItemKey key, final int amount) {
        if (!isAvailable() || amount <= 0) {
            return Optional.empty();
        }

        final Reference reference = Reference.parse(key);
        if (reference == null) {
            return Optional.empty();
        }

        final Type type = MMOItems.plugin.getTypes().get(reference.type());
        if (type == null) {
            return Optional.empty();
        }

        final ItemStack item = MMOItems.plugin.getItem(type, reference.id());
        if (item == null) {
            return Optional.empty();
        }

        item.setAmount(amount);
        return Optional.of(item);
    }

    @Override
    public boolean matches(final ItemStack stack, final ItemKey key) {
        if (!isAvailable() || stack == null || stack.getType().isAir()) {
            return false;
        }

        final Reference reference = Reference.parse(key);
        if (reference == null) {
            return false;
        }

        final NBTItem nbt = NBTItem.get(stack);
        final Type type = MMOItems.getType(nbt);
        if (type == null) {
            return false;
        }

        final String id = MMOItems.getID(nbt);
        return type.getId().equals(reference.type()) && reference.id().equals(id);
    }

    /**
     * The two halves of an {@code mmoitems:TYPE.ID} key, upper-cased the way MMOItems stores them.
     */
    private record Reference(String type, String id) {

        /**
         * @param key the key to split
         * @return the parsed reference, or {@code null} if the id is not {@code TYPE.ID}
         */
        static Reference parse(final ItemKey key) {
            if (key == null) {
                return null;
            }
            final String raw = key.id();
            final int separator = raw.indexOf('.');
            if (separator <= 0 || separator == raw.length() - 1) {
                return null;
            }
            final String type = raw.substring(0, separator);
            final String id = raw.substring(separator + 1);
            if (id.indexOf('.') >= 0) {
                return null;
            }
            return new Reference(type.toUpperCase(Locale.ROOT), id.toUpperCase(Locale.ROOT));
        }
    }
}
