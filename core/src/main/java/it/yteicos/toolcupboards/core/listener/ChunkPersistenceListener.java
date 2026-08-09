package it.yteicos.toolcupboards.core.listener;

import it.yteicos.toolcupboards.core.block.ChunkBlockStore;
import it.yteicos.toolcupboards.core.block.DefaultBuildBlockService;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Objects;

/**
 * Mirrors the chunk lifecycle into the placed-block index: a loaded chunk restores its blocks, an
 * unloading one writes them back and drops them from memory.
 * <p>
 * Keeping the two symmetric is what stops the index from growing without bound and from ending up
 * with two entries for the same position after a chunk round-trip.
 */
public final class ChunkPersistenceListener implements Listener {

    private final DefaultBuildBlockService blocks;
    private final ChunkBlockStore chunkStore;

    /**
     * @param blocks     the placed-block index
     * @param chunkStore the chunk-backed store
     */
    public ChunkPersistenceListener(final DefaultBuildBlockService blocks, final ChunkBlockStore chunkStore) {
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.chunkStore = Objects.requireNonNull(chunkStore, "chunkStore");
    }

    /**
     * @param event the Bukkit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(final ChunkLoadEvent event) {
        chunkStore.load(event.getChunk());
    }

    /**
     * @param event the Bukkit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(final ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        chunkStore.save(chunk);
        blocks.unloadChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }
}
