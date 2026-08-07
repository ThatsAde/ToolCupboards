package it.yteicos.toolcupboards.core.item;

import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProvider;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;


import java.util.Optional;

public class NexoItemProvider implements ItemProvider {

    @Override
    public String namespace() {
        return "nexo";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Nexo");
    }

    @Override
    public Optional<ItemStack> createItem(ItemKey key, int amount) {
        
        /*NexoItems.optionalItemFromId(String) → Optional<ItemBuilder>   // mappa 1:1 sul tuo Optional
        NexoItems.itemFromId(String)         → ItemBuilder             // nullable
        NexoItems.exists(String)             → boolean
        NexoItems.idFromItem(ItemStack)      → String                  // nullable
        ItemBuilder.setAmount(int) → ItemBuilder ; ItemBuilder.build() → ItemStack*/
    }

    @Override
    public boolean matches(ItemStack stack, ItemKey key) {
        return false;
    }
}
