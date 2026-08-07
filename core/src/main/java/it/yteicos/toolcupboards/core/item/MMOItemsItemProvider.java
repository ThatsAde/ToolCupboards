package it.yteicos.toolcupboards.core.item;

import io.lumine.mythic.lib.api.item.NBTItem;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProvider;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class MMOItemsItemProvider implements ItemProvider {

    @Override
    public String namespace() {
        return "mmoitems";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    @Override
    public Optional<ItemStack> createItem(ItemKey key, int amount) {
        if (!isAvailable()) return Optional.empty();

        final String[] values = key.id().split("\\.");
        if (values.length < 2) return Optional.empty();

        final String typeName = values[0];
        final String itemId = values[1];

        final Type type = MMOItems.plugin.getTypes().get(typeName.toUpperCase());
        if (type == null) return Optional.empty();

        final ItemStack item = MMOItems.plugin.getItem(type, itemId);
        if (item == null) return Optional.empty();

        item.setAmount(amount);
        return Optional.of(item);
    }

    @Override
    public boolean matches(ItemStack stack, ItemKey key) {
        if (!isAvailable() || stack == null || stack.getType().isAir()) {
            return false;
        }

        final String[] values = key.id().split("\\.");
        if (values.length < 2) return false;

        final String expectedType = values[0];
        final String expectedId = values[1];

        final NBTItem nbt = NBTItem.get(stack);

        final Type itemType = MMOItems.getType(nbt);
        if (itemType == null) return false;

        final String itemId = nbt.getString("MMOITEMS_ITEM_ID");

        return itemType.getId().equalsIgnoreCase(expectedType) && expectedId.equalsIgnoreCase(itemId);
    }
}