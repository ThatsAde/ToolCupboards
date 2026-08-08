package it.yteicos.toolcupboards;

import it.yteicos.toolcupboards.api.ToolCupboardsAPI;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.core.ToolCupboardsServices;
import it.yteicos.toolcupboards.core.block.BuildBlockItems;
import it.yteicos.toolcupboards.core.block.BuildBlockRenderer;
import it.yteicos.toolcupboards.core.block.ChunkBlockStore;
import it.yteicos.toolcupboards.core.block.DefaultBuildBlockService;
import it.yteicos.toolcupboards.core.block.SimpleBuildBlockRegistry;
import it.yteicos.toolcupboards.core.config.BlockDefinitionLoader;
import it.yteicos.toolcupboards.core.config.PluginSettings;
import it.yteicos.toolcupboards.core.config.YamlBlockDefinitionLoader;
import it.yteicos.toolcupboards.core.item.MMOItemsItemProvider;
import it.yteicos.toolcupboards.core.item.NexoItemProvider;
import it.yteicos.toolcupboards.core.item.SimpleItemProviderRegistry;
import it.yteicos.toolcupboards.core.item.VanillaItemProvider;
import it.yteicos.toolcupboards.core.listener.BuildBlockListener;
import it.yteicos.toolcupboards.core.listener.BuildProtectionListener;
import it.yteicos.toolcupboards.core.listener.ChunkPersistenceListener;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plugin entry point.
 * <p>
 * Boot order matters and is intentionally explicit: settings, then item providers, then block
 * definitions (which reference items), then the gameplay services, then listeners.
 */
public final class ToolCupboardsPlugin extends JavaPlugin {

    /** Folder, relative to the plugin data folder, holding the block definition files. */
    public static final String BLOCKS_FOLDER = "blocks";

    private ToolCupboardsServices services;
    private SimpleBuildBlockRegistry blockRegistry;
    private DefaultBuildBlockService blocks;
    private BuildBlockItems blockItems;
    private ChunkBlockStore chunkStore;

    @Override
    public void onEnable() {
        services = new ToolCupboardsServices(getPluginMeta().getVersion());

        saveDefaultConfig();
        saveResource(BLOCKS_FOLDER + "/example.yml", false);

        if (!loadSettings()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Every provider is registered unconditionally: each one reports isAvailable() == false
        // when its backing plugin is missing, and the registry refuses to delegate to it.
        final SimpleItemProviderRegistry itemProviders = new SimpleItemProviderRegistry();
        itemProviders.register(new VanillaItemProvider());
        itemProviders.register(new NexoItemProvider());
        itemProviders.register(new MMOItemsItemProvider());
        services.setItemProviders(itemProviders);

        blockRegistry = new SimpleBuildBlockRegistry();
        services.setBlockRegistry(blockRegistry);
        reloadBlockDefinitions();

        blockItems = new BuildBlockItems(this, itemProviders);
        blocks = new DefaultBuildBlockService(new BuildBlockRenderer(itemProviders, getLogger()));
        services.setBlocks(blocks);
        chunkStore = new ChunkBlockStore(this, blockRegistry, blocks, getLogger());

        // TODO: wire ToolCupboardService, ProtectionService, UpkeepService, CraftingService and
        //       ShopService here. Until then gameplayReady() stays false and protection no-ops.

        final PluginManager events = getServer().getPluginManager();
        events.registerEvents(new BuildBlockListener(services, blocks, blockItems, chunkStore), this);
        events.registerEvents(new ChunkPersistenceListener(blocks, chunkStore), this);
        events.registerEvents(new BuildProtectionListener(services), this);
        Bukkit.getServicesManager().register(ToolCupboardsAPI.class, services, this, ServicePriority.Normal);

        restoreLoadedChunks();

        if (!services.gameplayReady()) {
            getLogger().warning("Gameplay services are not wired yet — protection is inactive.");
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        // Shutdown does not reliably fire ChunkUnloadEvent, so flush every loaded chunk by hand.
        if (chunkStore != null && blocks != null) {
            forEachLoadedChunk(chunkStore::save);
            blocks.clear();
        }
        if (blockRegistry != null) {
            blockRegistry.clear();
        }
        services = null;
    }

    /** @return the item factory for build blocks, used by commands and the shop */
    public BuildBlockItems blockItems() {
        return blockItems;
    }

    /**
     * Reads back the chunks that were already loaded when the plugin enabled: they never fire
     * {@link org.bukkit.event.world.ChunkLoadEvent} for us.
     */
    private void restoreLoadedChunks() {
        final int[] restored = {0};
        forEachLoadedChunk(chunk -> restored[0] += chunkStore.load(chunk));
        if (restored[0] > 0) {
            getLogger().info("Restored " + restored[0] + " build block(s) from already-loaded chunks.");
        }
    }

    private void forEachLoadedChunk(final Consumer<Chunk> action) {
        for (final World world : getServer().getWorlds()) {
            for (final Chunk chunk : world.getLoadedChunks()) {
                action.accept(chunk);
            }
        }
    }

    /**
     * Re-reads {@code config.yml} and every block definition. Safe to call at runtime.
     *
     * @return {@code true} if the reload succeeded
     */
    public boolean reloadEverything() {
        reloadConfig();
        if (!loadSettings()) {
            return false;
        }
        reloadBlockDefinitions();
        return true;
    }

    /** @return the service container backing the public API */
    public ToolCupboardsServices services() {
        return services;
    }

    private boolean loadSettings() {
        try {
            services.setSettings(PluginSettings.from(getConfig()));
            return true;
        } catch (final RuntimeException exception) {
            getLogger().severe("config.yml is invalid: " + exception.getMessage());
            return false;
        }
    }

    private void reloadBlockDefinitions() {
        final Path folder = getDataFolder().toPath().resolve(BLOCKS_FOLDER);
        final BlockDefinitionLoader loader = new YamlBlockDefinitionLoader(getLogger());
        final List<BuildBlockDefinition> definitions = loader.loadAll(folder);

        blockRegistry.clear();
        for (final BuildBlockDefinition definition : definitions) {
            try {
                blockRegistry.register(definition);
            } catch (final IllegalStateException exception) {
                getLogger().warning(exception.getMessage());
            }
        }
        getLogger().info("Loaded " + blockRegistry.size() + " build block definition(s).");
    }
}
