package it.yteicos.toolcupboards.core.storage;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import it.yteicos.toolcupboards.api.cupboard.ToolCupboard;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Persists placed cupboards and build blocks across restarts.
 * <p>
 * Every method is asynchronous: I/O must never touch the main thread. Results come back as plain
 * data and are turned into live objects by the calling service, on the main thread.
 * <p>
 * Implementations planned: YAML per world (small servers), SQLite (default), MySQL (networks).
 */
public interface StorageBackend {

    /**
     * Opens connections and runs migrations.
     *
     * @return a future completing once the backend is usable
     */
    CompletableFuture<Void> initialize();

    /**
     * Loads everything belonging to a world, typically on world load.
     *
     * @param world the world being loaded
     * @return the stored cupboards and blocks
     */
    CompletableFuture<WorldSnapshot> loadWorld(World world);

    /**
     * Persists a cupboard, inserting or updating as needed.
     *
     * @param cupboard the cupboard to store
     * @return a future completing once written
     */
    CompletableFuture<Void> saveCupboard(ToolCupboard cupboard);

    /**
     * Deletes a cupboard and its authorization list.
     *
     * @param cupboard the cupboard to forget
     * @return a future completing once written
     */
    CompletableFuture<Void> deleteCupboard(ToolCupboard cupboard);

    /**
     * Persists a build block, inserting or updating as needed.
     *
     * @param block the block to store
     * @return a future completing once written
     */
    CompletableFuture<Void> saveBlock(BuildBlock block);

    /**
     * Deletes a build block.
     *
     * @param block the block to forget
     * @return a future completing once written
     */
    CompletableFuture<Void> deleteBlock(BuildBlock block);

    /**
     * Flushes pending writes and closes connections.
     *
     * @return a future completing once shut down
     */
    CompletableFuture<Void> close();

    /**
     * Everything stored for a single world.
     *
     * @param cupboards the cupboards in that world
     * @param blocks    the build blocks in that world
     */
    record WorldSnapshot(List<ToolCupboard> cupboards, List<BuildBlock> blocks) {

        /** An empty snapshot, for worlds with nothing stored. */
        public static final WorldSnapshot EMPTY = new WorldSnapshot(List.of(), List.of());

        public WorldSnapshot {
            cupboards = List.copyOf(cupboards);
            blocks = List.copyOf(blocks);
        }
    }
}
