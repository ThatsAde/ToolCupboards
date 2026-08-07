package it.yteicos.toolcupboards.core;

import it.yteicos.toolcupboards.api.ToolCupboardsAPI;
import it.yteicos.toolcupboards.api.block.BuildBlockRegistry;
import it.yteicos.toolcupboards.api.block.BuildBlockService;
import it.yteicos.toolcupboards.api.claim.ProtectionService;
import it.yteicos.toolcupboards.api.crafting.CraftingService;
import it.yteicos.toolcupboards.api.cupboard.ToolCupboardService;
import it.yteicos.toolcupboards.api.item.ItemProviderRegistry;
import it.yteicos.toolcupboards.api.shop.ShopService;
import it.yteicos.toolcupboards.api.upkeep.UpkeepService;
import it.yteicos.toolcupboards.core.config.PluginSettings;

import java.util.Objects;

/**
 * Service container and {@link ToolCupboardsAPI} implementation.
 * <p>
 * Services are wired in during {@code ToolCupboardsPlugin#onEnable()}, in dependency order. A
 * service that has not been wired yet fails loudly on access instead of returning {@code null} —
 * while the plugin is being built out, that error message is the fastest way to see what is still
 * missing.
 */
public final class ToolCupboardsServices implements ToolCupboardsAPI {

    private final String version;

    private PluginSettings settings;
    private BuildBlockRegistry blockRegistry;
    private BuildBlockService blocks;
    private ToolCupboardService cupboards;
    private ProtectionService protection;
    private UpkeepService upkeep;
    private ItemProviderRegistry itemProviders;
    private CraftingService crafting;
    private ShopService shop;

    /**
     * @param version the running plugin version
     */
    public ToolCupboardsServices(final String version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public String version() {
        return version;
    }

    /** @return the loaded {@code config.yml} view */
    public PluginSettings settings() {
        return require(settings, "PluginSettings");
    }

    @Override
    public BuildBlockRegistry blockRegistry() {
        return require(blockRegistry, "BuildBlockRegistry");
    }

    @Override
    public BuildBlockService blocks() {
        return require(blocks, "BuildBlockService");
    }

    @Override
    public ToolCupboardService cupboards() {
        return require(cupboards, "ToolCupboardService");
    }

    @Override
    public ProtectionService protection() {
        return require(protection, "ProtectionService");
    }

    @Override
    public UpkeepService upkeep() {
        return require(upkeep, "UpkeepService");
    }

    @Override
    public ItemProviderRegistry itemProviders() {
        return require(itemProviders, "ItemProviderRegistry");
    }

    @Override
    public CraftingService crafting() {
        return require(crafting, "CraftingService");
    }

    @Override
    public ShopService shop() {
        return require(shop, "ShopService");
    }

    // --- wiring --------------------------------------------------------------------------------

    /** @param settings the loaded settings */
    public void setSettings(final PluginSettings settings) {
        this.settings = settings;
    }

    /** @param blockRegistry the definition registry */
    public void setBlockRegistry(final BuildBlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
    }

    /** @param blocks the placed-block service */
    public void setBlocks(final BuildBlockService blocks) {
        this.blocks = blocks;
    }

    /** @param cupboards the cupboard service */
    public void setCupboards(final ToolCupboardService cupboards) {
        this.cupboards = cupboards;
    }

    /** @param protection the protection service */
    public void setProtection(final ProtectionService protection) {
        this.protection = protection;
    }

    /** @param upkeep the upkeep service */
    public void setUpkeep(final UpkeepService upkeep) {
        this.upkeep = upkeep;
    }

    /** @param itemProviders the item provider registry */
    public void setItemProviders(final ItemProviderRegistry itemProviders) {
        this.itemProviders = itemProviders;
    }

    /** @param crafting the crafting service */
    public void setCrafting(final CraftingService crafting) {
        this.crafting = crafting;
    }

    /** @param shop the shop service */
    public void setShop(final ShopService shop) {
        this.shop = shop;
    }

    /**
     * @return {@code true} once every gameplay service needed by the listeners is wired; the
     *         listeners no-op until then so a half-built plugin cannot corrupt worlds
     */
    public boolean gameplayReady() {
        return blocks != null && cupboards != null && protection != null;
    }

    private static <T> T require(final T service, final String name) {
        if (service == null) {
            throw new IllegalStateException(name + " is not wired yet — implement it under core/ and "
                    + "register it in ToolCupboardsPlugin#onEnable");
        }
        return service;
    }
}
