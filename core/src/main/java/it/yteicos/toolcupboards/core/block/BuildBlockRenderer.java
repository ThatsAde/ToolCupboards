package it.yteicos.toolcupboards.core.block;

import com.nexomc.nexo.api.NexoBlocks;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.item.ItemProviderRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Puts a build block's appearance into the world, and takes it back out.
 * <p>
 * Hybrid by design: a {@code nexo:} texture is placed through Nexo and keeps its custom look, while
 * anything else falls back to the plain Minecraft block behind the key. Nexo therefore stays
 * optional — a definition that does not use it works on a bare Paper server.
 */
public final class BuildBlockRenderer {

    private static final String NEXO_PLUGIN = "Nexo";
    private static final String NEXO_NAMESPACE = "nexo";

    private final ItemProviderRegistry providers;
    private final Logger logger;

    /**
     * @param providers resolves a texture key into a concrete item, and therefore a material
     * @param logger    where unusable textures are reported
     */
    public BuildBlockRenderer(final ItemProviderRegistry providers, final Logger logger) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Makes the world show this definition at the given position.
     * <p>
     * Idempotent: calling it on a position that already looks right changes nothing, which is why
     * it is safe to run right after a vanilla placement has already put the block there.
     *
     * @param definition what is standing there
     * @param location   the block position, main thread only
     */
    public void apply(final BuildBlockDefinition definition, final Location location) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(location, "location");

        final ItemKey texture = definition.blockTexture();
        if (isNexoTexture(texture)) {
            NexoBlocks.place(texture.id(), location);
            return;
        }

        final Optional<Material> material = blockMaterial(texture);
        if (material.isEmpty()) {
            logger.warning("Build block '" + definition.id() + "' has no placeable block behind '"
                    + texture + "'; leaving the world block as it is.");
            return;
        }
        location.getBlock().setType(material.get(), false);
    }

    /**
     * Clears the world block, whichever plugin owns it.
     *
     * @param location the block position, main thread only
     */
    public void clear(final Location location) {
        Objects.requireNonNull(location, "location");
        final Block block = location.getBlock();
        if (nexoLoaded() && NexoBlocks.isCustomBlock(block)) {
            // Nexo tracks its own block entities: let it tear the block down instead of us.
            NexoBlocks.remove(location);
            return;
        }
        block.setType(Material.AIR, false);
    }

    /** @return the material a non-Nexo texture key resolves to, if it can be placed at all */
    private Optional<Material> blockMaterial(final ItemKey texture) {
        return providers.createItem(texture, 1)
                .map(ItemStack::getType)
                .filter(Material::isBlock);
    }

    private static boolean isNexoTexture(final ItemKey texture) {
        return NEXO_NAMESPACE.equals(texture.namespace()) && nexoLoaded();
    }

    private static boolean nexoLoaded() {
        return Bukkit.getPluginManager().isPluginEnabled(NEXO_PLUGIN);
    }
}
