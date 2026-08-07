package it.yteicos.toolcupboards.api;

import it.yteicos.toolcupboards.api.block.BuildBlockRegistry;
import it.yteicos.toolcupboards.api.block.BuildBlockService;
import it.yteicos.toolcupboards.api.claim.ProtectionService;
import it.yteicos.toolcupboards.api.crafting.CraftingService;
import it.yteicos.toolcupboards.api.cupboard.ToolCupboardService;
import it.yteicos.toolcupboards.api.item.ItemProviderRegistry;
import it.yteicos.toolcupboards.api.shop.ShopService;
import it.yteicos.toolcupboards.api.upkeep.UpkeepService;

/**
 * The single entry point third-party plugins use to reach ToolCupboards.
 * <p>
 * Obtained from Bukkit's service manager, never from a static plugin singleton:
 * <pre>{@code
 * ToolCupboardsAPI api = Bukkit.getServicesManager().load(ToolCupboardsAPI.class);
 * }</pre>
 * The instance is registered during the plugin's enable phase, so consumers should look it up on
 * their own enable at the earliest, or lazily.
 */
public interface ToolCupboardsAPI {

    /** @return the running plugin version */
    String version();

    /** @return the definitions loaded from {@code blocks/*.yml} */
    BuildBlockRegistry blockRegistry();

    /** @return the placed build blocks */
    BuildBlockService blocks();

    /** @return the placed tool cupboards */
    ToolCupboardService cupboards();

    /** @return the claim rules */
    ProtectionService protection();

    /** @return the decay loop */
    UpkeepService upkeep();

    /** @return the item abstraction over vanilla, Nexo and MMOItems */
    ItemProviderRegistry itemProviders();

    /** @return the recipe registration seam */
    CraftingService crafting();

    /** @return the block shop */
    ShopService shop();
}
