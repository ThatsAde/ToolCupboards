package it.yteicos.toolcupboards.core.block;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.block.BuildBlockService;
import it.yteicos.toolcupboards.api.block.BuildBlockType;
import it.yteicos.toolcupboards.api.claim.ClaimArea;
import it.yteicos.toolcupboards.api.event.BuildBlockDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory {@link BuildBlockService}: the live set of placed blocks, indexed by position and by id.
 * <p>
 * Main-thread only, like the interface says the maps are plain {@link HashMap}s on purpose.
 * Everything here is O(1) except {@link #blocksIn(ClaimArea)}, which scans; the spatial index that
 * makes that cheap belongs to {@code core.claim} and is not written yet.
 * <p>
 * Persistence is not wired: {@code core.storage} has no backend, so placements live only until the
 * server stops. {@link #load(BuildBlock)} and {@link #unloadWorld(World)} are the hooks a backend
 * will call once it exists.
 */
public final class DefaultBuildBlockService implements BuildBlockService {

    private final Map<BlockPos, BuildBlock> byPosition = new LinkedHashMap<>();
    private final Map<UUID, BuildBlock> byId = new HashMap<>();
    /** Secondary index so chunk-shaped questions do not scan every placed block. */
    private final Map<ChunkPos, Set<BuildBlock>> byChunk = new HashMap<>();
    private final BuildBlockRenderer renderer;

    /**
     * @param renderer applies and clears the world appearance of a placed block
     */
    public DefaultBuildBlockService(final BuildBlockRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public Optional<BuildBlock> blockAt(final Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byPosition.get(BlockPos.of(location)));
    }

    @Override
    public boolean canPlace(final BuildBlockDefinition definition, final Location location) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(location.getWorld(), "location world");

        if (byPosition.containsKey(BlockPos.of(location))) {
            return false;
        }
        // Foundations carry the base, so they may go anywhere; structures need one underneath them.
        return definition.type() != BuildBlockType.STRUCTURE || restsOnFoundation(location);
    }

    @Override
    public BuildBlock place(final BuildBlockDefinition definition, final Location location,
                            @Nullable final UUID placedBy) {
        if (!canPlace(definition, location)) {
            throw new IllegalStateException("cannot place " + definition.id() + " at "
                    + describe(location) + ": position taken or no foundation below");
        }

        final Location blockLocation = blockCorner(location);
        final BuildBlock block = new DefaultBuildBlock(definition, blockLocation, placedBy);
        index(block);
        renderer.apply(definition, blockLocation);
        // TODO: StorageBackend#saveBlock once a backend is wired; chunk persistence is handled by
        //       ChunkBlockStore in the meantime.
        return block;
    }

    @Override
    public double damage(final BuildBlock block, final double amount,
                         final BuildBlockDamageEvent.Cause cause, @Nullable final Player damager) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(cause, "cause");
        if (amount < 0.0D) {
            throw new IllegalArgumentException("amount must not be negative, got " + amount);
        }

        final BuildBlockDamageEvent event = new BuildBlockDamageEvent(block, damager, cause, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return -1.0D;
        }

        final double remaining = block.damage(event.amount());
        if (remaining <= 0.0D) {
            destroy(block);
            return 0.0D;
        }
        return remaining;
    }

    @Override
    public void destroy(final BuildBlock block) {
        Objects.requireNonNull(block, "block");

        final Location location = block.location();
        final BlockPos position = BlockPos.of(location);
        // Only drop the mapping if it still points at this block: a replacement may already own it.
        if (byPosition.get(position) == block) {
            byPosition.remove(position);
        }
        byId.remove(block.id());
        unindexChunk(block, position);

        if (location.getWorld() != null && location.isWorldLoaded()) {
            renderer.clear(location);
        }
        // TODO: StorageBackend#deleteBlock once a backend is wired.
    }

    @Override
    public Collection<BuildBlock> blocksIn(final ClaimArea area) {
        Objects.requireNonNull(area, "area");
        final List<BuildBlock> inside = new ArrayList<>();
        for (final BuildBlock block : byPosition.values()) {
            if (area.contains(center(block.location()))) {
                inside.add(block);
            }
        }
        return List.copyOf(inside);
    }

    @Override
    public int count() {
        return byId.size();
    }

    // --- lifecycle -------------------------------------------------------------------------------

    /**
     * Adds an already-existing block to the index, bypassing the placement rules. For storage
     * restore only.
     *
     * @param block the block to track
     */
    public void load(final BuildBlock block) {
        index(Objects.requireNonNull(block, "block"));
    }

    /**
     * Forgets every block of a world without touching it, for world unload.
     *
     * @param world the world being unloaded
     * @return how many blocks were dropped from the index
     */
    public int unloadWorld(final World world) {
        Objects.requireNonNull(world, "world");
        final UUID id = world.getUID();
        final List<BuildBlock> dropped = new ArrayList<>();
        for (final Map.Entry<BlockPos, BuildBlock> entry : byPosition.entrySet()) {
            if (entry.getKey().world().equals(id)) {
                dropped.add(entry.getValue());
            }
        }
        for (final BuildBlock block : dropped) {
            final BlockPos position = BlockPos.of(block.location());
            byPosition.remove(position);
            byId.remove(block.id());
            unindexChunk(block, position);
        }
        return dropped.size();
    }

    /**
     * Forgets the blocks of one chunk without touching the world, for chunk unload. They come back
     * through {@link #load(BuildBlock)} when the chunk is read again.
     *
     * @param world  the world the chunk belongs to
     * @param chunkX the chunk x coordinate
     * @param chunkZ the chunk z coordinate
     * @return how many blocks were dropped from the index
     */
    public int unloadChunk(final World world, final int chunkX, final int chunkZ) {
        Objects.requireNonNull(world, "world");
        final Set<BuildBlock> dropped = byChunk.remove(new ChunkPos(world.getUID(), chunkX, chunkZ));
        if (dropped == null) {
            return 0;
        }
        for (final BuildBlock block : dropped) {
            byPosition.remove(BlockPos.of(block.location()));
            byId.remove(block.id());
        }
        return dropped.size();
    }

    /** Drops every tracked block, for plugin disable. Does not touch the world. */
    public void clear() {
        byPosition.clear();
        byId.clear();
        byChunk.clear();
    }

    /**
     * @param world  the world the chunk belongs to
     * @param chunkX the chunk x coordinate
     * @param chunkZ the chunk z coordinate
     * @return every build block standing in that chunk
     */
    public Collection<BuildBlock> blocksInChunk(final World world, final int chunkX, final int chunkZ) {
        Objects.requireNonNull(world, "world");
        final Set<BuildBlock> blocks = byChunk.get(new ChunkPos(world.getUID(), chunkX, chunkZ));
        return blocks == null ? List.of() : List.copyOf(blocks);
    }

    /**
     * @param id the placed-block id
     * @return the block with that id, if it is loaded
     */
    public Optional<BuildBlock> byId(final UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    // --- internals -------------------------------------------------------------------------------

    private void index(final BuildBlock block) {
        final BlockPos position = BlockPos.of(block.location());
        byPosition.put(position, block);
        byId.put(block.id(), block);
        byChunk.computeIfAbsent(ChunkPos.of(position), key -> new LinkedHashSet<>()).add(block);
    }

    private void unindexChunk(final BuildBlock block, final BlockPos position) {
        final ChunkPos chunk = ChunkPos.of(position);
        final Set<BuildBlock> blocks = byChunk.get(chunk);
        if (blocks != null && blocks.remove(block) && blocks.isEmpty()) {
            byChunk.remove(chunk);
        }
    }

    /**
     * Walks straight down looking for a foundation: a structure may rest on other structures as
     * long as the column ends on one. Bounded by the world floor.
     */
    private boolean restsOnFoundation(final Location location) {
        final World world = location.getWorld();
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        for (int y = location.getBlockY() - 1; y >= world.getMinHeight(); y--) {
            final BuildBlock below = byPosition.get(new BlockPos(world.getUID(), x, y, z));
            if (below == null) {
                return false;
            }
            if (below.definition().type() == BuildBlockType.FOUNDATION) {
                return true;
            }
        }
        return false;
    }

    private static Location blockCorner(final Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static Location center(final Location location) {
        return location.clone().add(0.5D, 0.5D, 0.5D);
    }

    private static String describe(final Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY()
                + "," + location.getBlockZ();
    }

    /**
     * Block coordinates plus world, so two positions compare by value instead of by
     * {@link Location}, which is mutable and carries yaw/pitch.
     */
    private record BlockPos(UUID world, int x, int y, int z) {

        static BlockPos of(final Location location) {
            return new BlockPos(Objects.requireNonNull(location.getWorld(), "location world").getUID(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    /** Chunk coordinates plus world, the key of the secondary index. */
    private record ChunkPos(UUID world, int x, int z) {

        static ChunkPos of(final BlockPos position) {
            return new ChunkPos(position.world(), position.x() >> 4, position.z() >> 4);
        }
    }
}
