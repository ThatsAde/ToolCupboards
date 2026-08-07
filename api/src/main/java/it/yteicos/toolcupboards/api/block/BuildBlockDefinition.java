package it.yteicos.toolcupboards.api.block;

import it.yteicos.toolcupboards.api.crafting.RecipeDefinition;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.shop.ShopEntry;
import it.yteicos.toolcupboards.api.upkeep.UpkeepRequirement;

import java.util.List;
import java.util.Optional;

/**
 * The immutable template of a build block, parsed from one {@code blocks/*.yml} file.
 * <p>
 * A definition is a blueprint, not a placed block: it says how much health a fresh block starts
 * with, what it costs to keep standing, how it is crafted and bought. The placed counterpart is
 * {@link BuildBlock}.
 */
public interface BuildBlockDefinition {

    /**
     * @return the stable identifier, derived from the file name without extension, lowercase;
     *         this is what persistence and commands refer to
     */
    String id();

    /** @return the {@code settings.block-name} value, used for display and item naming */
    String blockName();

    /** @return whether this is a foundation or a structure block */
    BuildBlockType type();

    /** @return the health a freshly placed block starts with, always positive */
    double maxHealth();

    /**
     * @return the maintenance costs charged to the covering cupboard; empty means this block never
     *         decays
     */
    List<UpkeepRequirement> upkeep();

    /** @return the shop availability; {@link ShopEntry#DISABLED} when the file omits the section */
    ShopEntry shop();

    /** @return the item players hold in the inventory, from {@code item-settings.item-modeldata} */
    ItemKey itemModel();

    /** @return the block appearance once placed, from {@code item-settings.block-texture} */
    ItemKey blockTexture();

    /** @return the shaped recipe, or empty if the file declares no {@code crafting-recipe} */
    Optional<RecipeDefinition> recipe();

    /** @return {@code true} if this definition costs nothing to maintain */
    default boolean isMaintenanceFree() {
        return upkeep().isEmpty();
    }
}
