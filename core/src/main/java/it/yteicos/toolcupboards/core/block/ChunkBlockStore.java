package it.yteicos.toolcupboards.core.block;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.block.BuildBlockRegistry;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Keeps placed build blocks in the chunk's own {@code PersistentDataContainer}, so a restart does
 * not silently swallow them.
 * <p>
 * This is a stopgap, not the persistence layer: it rides along with the region files, it cannot be
 * queried while a chunk is unloaded, and it says nothing about cupboards. {@code core.storage} will
 * replace it in Fase 4. What it buys today is that a base survives {@code /stop}.
 * <p>
 * One entry per line, {@code x;y;z;definition-id;health;placed-by;placed-at}, with an empty
 * {@code placed-by} for plugin placements.
 */
public final class ChunkBlockStore {

    private static final String ENTRY_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = ";";
    private static final int FIELD_COUNT = 7;

    private final NamespacedKey key;
    private final BuildBlockRegistry registry;
    private final DefaultBuildBlockService blocks;
    private final Logger logger;

    /**
     * @param plugin   owner of the namespaced key
     * @param registry resolves stored definition ids
     * @param blocks   the index restored blocks are handed to
     * @param logger   where unreadable entries are reported
     */
    public ChunkBlockStore(final Plugin plugin,
                           final BuildBlockRegistry registry,
                           final DefaultBuildBlockService blocks,
                           final Logger logger) {
        this.key = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), "blocks");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Writes the chunk's current build blocks into its container, replacing whatever was there.
     *
     * @param chunk the chunk to persist
     */
    public void save(final Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        final Collection<BuildBlock> inChunk = blocks.blocksInChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
        final PersistentDataContainer container = chunk.getPersistentDataContainer();

        if (inChunk.isEmpty()) {
            container.remove(key);
            return;
        }

        final StringJoiner joiner = new StringJoiner(ENTRY_SEPARATOR);
        for (final BuildBlock block : inChunk) {
            joiner.add(encode(block));
        }
        container.set(key, PersistentDataType.STRING, joiner.toString());
    }

    /**
     * Convenience for the mutation sites, which know a position rather than a chunk.
     *
     * @param location the block position whose chunk should be rewritten
     */
    public void saveAround(final Location location) {
        if (location != null && location.getWorld() != null && location.isWorldLoaded()) {
            save(location.getChunk());
        }
    }

    /**
     * Restores the chunk's build blocks into the index. Entries whose definition no longer exists
     * are dropped, which is what makes deleting a {@code blocks/*.yml} file a real removal.
     *
     * @param chunk the chunk being loaded
     * @return how many blocks were restored
     */
    public int load(final Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        final String stored = chunk.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return 0;
        }

        int restored = 0;
        boolean dropped = false;
        for (final String entry : stored.split(ENTRY_SEPARATOR)) {
            if (entry.isBlank()) {
                continue;
            }
            final BuildBlock block = decode(chunk, entry);
            if (block == null) {
                dropped = true;
                continue;
            }
            if (blocks.blockAt(block.location()).isPresent()) {
                // Already indexed: a chunk can be re-loaded without ever having been unloaded.
                continue;
            }
            blocks.load(block);
            restored++;
        }

        if (dropped) {
            // Rewrite so the unreadable entries do not come back on the next load.
            save(chunk);
        }
        return restored;
    }

    private static String encode(final BuildBlock block) {
        final Location location = block.location();
        return location.getBlockX() + FIELD_SEPARATOR
                + location.getBlockY() + FIELD_SEPARATOR
                + location.getBlockZ() + FIELD_SEPARATOR
                + block.definition().id() + FIELD_SEPARATOR
                + block.health() + FIELD_SEPARATOR
                + block.placedBy().map(UUID::toString).orElse("") + FIELD_SEPARATOR
                + block.placedAt();
    }

    /** @return the decoded block, or {@code null} if the entry is unusable */
    private BuildBlock decode(final Chunk chunk, final String entry) {
        final String[] fields = entry.split(FIELD_SEPARATOR, -1);
        if (fields.length != FIELD_COUNT) {
            logger.warning("Dropping malformed build block entry in chunk "
                    + chunk.getX() + "," + chunk.getZ() + ": " + entry);
            return null;
        }

        final BuildBlockDefinition definition = registry.definition(fields[3]).orElse(null);
        if (definition == null) {
            logger.warning("Dropping build block '" + fields[3] + "' in chunk "
                    + chunk.getX() + "," + chunk.getZ() + ": no such definition is loaded.");
            return null;
        }

        try {
            final Location location = new Location(chunk.getWorld(),
                    Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            final UUID placedBy = fields[5].isEmpty() ? null : UUID.fromString(fields[5]);
            return new DefaultBuildBlock(UUID.randomUUID(), definition, location, placedBy,
                    Long.parseLong(fields[6]), Double.parseDouble(fields[4]));
        } catch (final IllegalArgumentException malformed) {
            logger.warning("Dropping unreadable build block entry in chunk "
                    + chunk.getX() + "," + chunk.getZ() + ": " + malformed.getMessage());
            return null;
        }
    }
}
