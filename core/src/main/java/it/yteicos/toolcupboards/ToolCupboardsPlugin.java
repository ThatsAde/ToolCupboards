package it.yteicos.toolcupboards;

import it.yteicos.toolcupboards.api.ToolCupboardsAPI;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.core.ToolCupboardsServices;
import it.yteicos.toolcupboards.core.block.SimpleBuildBlockRegistry;
import it.yteicos.toolcupboards.core.config.BlockDefinitionLoader;
import it.yteicos.toolcupboards.core.config.PluginSettings;
import it.yteicos.toolcupboards.core.config.YamlBlockDefinitionLoader;
import it.yteicos.toolcupboards.core.item.SimpleItemProviderRegistry;
import it.yteicos.toolcupboards.core.item.VanillaItemProvider;
import it.yteicos.toolcupboards.core.listener.BuildProtectionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;

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

    @Override
    public void onEnable() {
        services = new ToolCupboardsServices(getPluginMeta().getVersion());

        saveDefaultConfig();
        saveResource(BLOCKS_FOLDER + "/example.yml", false);

        if (!loadSettings()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final SimpleItemProviderRegistry itemProviders = new SimpleItemProviderRegistry();
        itemProviders.register(new VanillaItemProvider());
        // TODO: register NexoItemProvider / MMOItemsItemProvider when those plugins are present.
        services.setItemProviders(itemProviders);

        blockRegistry = new SimpleBuildBlockRegistry();
        services.setBlockRegistry(blockRegistry);
        reloadBlockDefinitions();

        // TODO: wire BuildBlockService, ToolCupboardService, ProtectionService, UpkeepService,
        //       CraftingService and ShopService here. Until then gameplayReady() stays false and
        //       the listeners no-op.

        getServer().getPluginManager().registerEvents(new BuildProtectionListener(services), this);
        Bukkit.getServicesManager().register(ToolCupboardsAPI.class, services, this, ServicePriority.Normal);

        if (!services.gameplayReady()) {
            getLogger().warning("Gameplay services are not wired yet — protection is inactive.");
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        if (blockRegistry != null) {
            blockRegistry.clear();
        }
        services = null;
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
