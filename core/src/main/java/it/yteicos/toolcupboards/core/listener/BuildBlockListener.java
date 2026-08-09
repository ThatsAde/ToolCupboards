package it.yteicos.toolcupboards.core.listener;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.block.BuildBlockRegistry;
import it.yteicos.toolcupboards.core.ToolCupboardsServices;
import it.yteicos.toolcupboards.core.block.BuildBlockItems;
import it.yteicos.toolcupboards.core.block.ChunkBlockStore;
import it.yteicos.toolcupboards.core.block.DefaultBuildBlockService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Keeps the placed-block index in step with the world.
 * <p>
 * Separate from {@link BuildProtectionListener} on purpose: that one answers "is this player
 * allowed to?", this one answers "is this one of ours, and where does it go in the index?". Both
 * run after protection has had its say, so a cancelled event never reaches the index.
 * <p>
 * Rendering is left to whoever owns the item: a Nexo-backed block is already placed by Nexo when
 * this runs, and a vanilla-backed one by Bukkit itself. The service only re-asserts the appearance.
 */
public final class BuildBlockListener implements Listener {

    private final ToolCupboardsServices services;
    private final DefaultBuildBlockService blocks;
    private final BuildBlockItems items;
    private final ChunkBlockStore chunkStore;

    /**
     * @param services   the service container, for settings and the definition registry
     * @param blocks     the placed-block index
     * @param items      recognises build block items in hand
     * @param chunkStore stopgap persistence, rewritten after every mutation
     */
    public BuildBlockListener(final ToolCupboardsServices services,
                              final DefaultBuildBlockService blocks,
                              final BuildBlockItems items,
                              final ChunkBlockStore chunkStore) {
        this.services = Objects.requireNonNull(services, "services");
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.items = Objects.requireNonNull(items, "items");
        this.chunkStore = Objects.requireNonNull(chunkStore, "chunkStore");
    }

    /**
     * Turns the placement of a stamped item into a tracked build block.
     *
     * @param event the Bukkit event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlace(final BlockPlaceEvent event) {
        final Optional<String> stamped = items.idOf(event.getItemInHand());
        if (stamped.isEmpty()) {
            return;
        }

        final Location location = event.getBlock().getLocation();
        if (!services.settings().enabledIn(location.getWorld().getName())) {
            // Outside the configured worlds the item behaves like the plain block it is built on.
            return;
        }

        final BuildBlockRegistry registry = services.blockRegistry();
        final Optional<BuildBlockDefinition> definition = registry.definition(stamped.get());
        if (definition.isEmpty()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component
                    .text("This build block no longer exists: " + stamped.get(), NamedTextColor.RED));
            return;
        }

        if (!blocks.canPlace(definition.get(), location)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component
                    .text("Structure blocks must rest on a foundation.", NamedTextColor.RED));
            return;
        }

        blocks.place(definition.get(), location, event.getPlayer().getUniqueId());
        chunkStore.saveAround(location);
    }

    /**
     * Drops a broken build block from the index.
     * <p>
     * A plain break destroys outright: that is the "no cupboard, anyone can break it" rule. Once
     * {@code ProtectionService} exists, an unauthorized break inside a claim goes through the damage
     * pipeline instead and never reaches this handler.
     *
     * @param event the Bukkit event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(final BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final Optional<BuildBlock> block = blocks.blockAt(location);
        if (block.isEmpty()) {
            return;
        }

        // The vanilla drop would be the texture block, not the build block: suppress it. What a
        // broken build block gives back, if anything, is a raiding rule and belongs to Fase 2.
        event.setDropItems(false);
        blocks.destroy(block.get());
        chunkStore.saveAround(location);
    }
}
