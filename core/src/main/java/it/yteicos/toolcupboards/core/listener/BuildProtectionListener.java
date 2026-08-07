package it.yteicos.toolcupboards.core.listener;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import it.yteicos.toolcupboards.api.claim.ClaimAction;
import it.yteicos.toolcupboards.core.ToolCupboardsServices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Gates vanilla block breaking and placing through the claim rules.
 * <p>
 * Deliberately thin: it translates Bukkit events into
 * {@link it.yteicos.toolcupboards.api.claim.ProtectionService} questions and nothing more. All the
 * actual rules live in the service, so they stay testable without a server.
 * <p>
 * The listener no-ops while {@link ToolCupboardsServices#gameplayReady()} is false, so a partially
 * wired plugin cannot half-apply protection.
 */
public final class BuildProtectionListener implements Listener {

    private final ToolCupboardsServices services;

    /**
     * @param services the service container
     */
    public BuildProtectionListener(final ToolCupboardsServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    /**
     * Cancels breaking a build block a player is not authorized for.
     * <p>
     * TODO: raiding — instead of a plain cancel, an unauthorized break should route into the damage
     * pipeline (fire {@code BuildBlockDamageEvent}, apply {@link BuildBlock#damage(double)}, remove
     * and fire {@code BuildBlockDestroyEvent} at zero health).
     *
     * @param event the Bukkit event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(final BlockBreakEvent event) {
        if (!services.gameplayReady()) {
            return;
        }
        final Optional<BuildBlock> buildBlock = services.blocks().blockAt(event.getBlock().getLocation());
        if (buildBlock.isEmpty()) {
            // Not one of our blocks: claims never protect vanilla blocks or containers.
            return;
        }
        final boolean allowed = services.protection()
                .allows(event.getPlayer(), event.getBlock().getLocation(), ClaimAction.BREAK_BUILD_BLOCK);
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels placing inside a claim the player is not authorized in.
     *
     * @param event the Bukkit event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlace(final BlockPlaceEvent event) {
        if (!services.gameplayReady()) {
            return;
        }
        final boolean allowed = services.protection()
                .allows(event.getPlayer(), event.getBlock().getLocation(), ClaimAction.PLACE_BUILD_BLOCK);
        if (!allowed) {
            event.setCancelled(true);
        }
    }
}
