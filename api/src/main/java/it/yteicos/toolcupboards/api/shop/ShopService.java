package it.yteicos.toolcupboards.api.shop;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Sells build blocks for the server's currency.
 * <p>
 * Two switches gate every purchase: the global {@code shop.enabled} flag in {@code config.yml} and
 * the per-definition {@code shop-settings.enabled}. The global one always wins, which is how a
 * server turns the shop off without touching dozens of block files.
 */
public interface ShopService {

    /** @return {@code true} if the global {@code shop.enabled} flag is on */
    boolean enabled();

    /**
     * @param definition the definition to price
     * @return its shop entry, or empty if the global switch is off
     */
    Optional<ShopEntry> entryOf(BuildBlockDefinition definition);

    /** @return every definition currently purchasable, in registration order */
    Collection<BuildBlockDefinition> catalogue();

    /**
     * Charges the buyer and hands over the blocks. Atomic: on any non-{@link Result#SUCCESS}
     * result, no money moves and no item is given.
     *
     * @param buyer      the purchasing player
     * @param definition what to buy
     * @param amount     how many units, must be positive
     * @return the outcome
     */
    Result purchase(Player buyer, BuildBlockDefinition definition, int amount);

    /**
     * Outcome of a purchase attempt.
     */
    enum Result {

        /** Money taken, items delivered. */
        SUCCESS,

        /** The global {@code shop.enabled} flag is off. */
        SHOP_DISABLED,

        /** The definition's own {@code shop-settings.enabled} is off. */
        NOT_FOR_SALE,

        /** {@code amount-available} is exhausted or lower than the requested amount. */
        OUT_OF_STOCK,

        /** The buyer cannot afford it. */
        INSUFFICIENT_FUNDS,

        /** The buyer's inventory has no room for the blocks. */
        NO_INVENTORY_SPACE,

        /** No economy plugin is installed, so nothing can be priced. */
        NO_ECONOMY
    }
}
