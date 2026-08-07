package it.yteicos.toolcupboards.api.shop;

/**
 * Shop availability of a build block, mapped from the {@code shop-settings} section of a block
 * definition file.
 *
 * @param enabled         whether this block may be bought at all; the global shop toggle wins over it
 * @param price           price per unit, must not be negative
 * @param amountAvailable remaining global stock; {@link #UNLIMITED} means no cap
 */
public record ShopEntry(boolean enabled, double price, int amountAvailable) {

    /** Sentinel for {@link #amountAvailable()} meaning "never runs out". */
    public static final int UNLIMITED = -1;

    /** A disabled entry, used when a definition omits {@code shop-settings}. */
    public static final ShopEntry DISABLED = new ShopEntry(false, 0.0D, 0);

    public ShopEntry {
        if (price < 0.0D) {
            throw new IllegalArgumentException("price must not be negative, got " + price);
        }
        if (amountAvailable < UNLIMITED) {
            throw new IllegalArgumentException("amount-available must be >= -1, got " + amountAvailable);
        }
    }

    /** @return {@code true} if stock is not capped */
    public boolean unlimited() {
        return amountAvailable == UNLIMITED;
    }

    /** @return {@code true} if the entry is enabled and something is left to sell */
    public boolean purchasable() {
        return enabled && (unlimited() || amountAvailable > 0);
    }
}
