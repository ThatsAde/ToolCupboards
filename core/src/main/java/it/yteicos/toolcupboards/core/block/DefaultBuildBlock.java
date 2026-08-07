package it.yteicos.toolcupboards.core.block;

import it.yteicos.toolcupboards.api.block.BuildBlock;
import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import org.bukkit.Location;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DefaultBuildBlock implements BuildBlock {

    private final UUID id;
    private final BuildBlockDefinition definition;
    private final Location location;
    private final UUID placedBy;
    private final long placedAt;
    private double health;

    public DefaultBuildBlock(UUID id, BuildBlockDefinition definition, Location location, UUID placedBy, long placedAt, double health) {
        this.id = Objects.requireNonNull(id, "L'ID non può essere null");
        this.definition = Objects.requireNonNull(definition, "La definizione non può essere null");
        this.location = Objects.requireNonNull(location, "La location non può essere null");
        this.placedBy = placedBy;
        this.placedAt = placedAt;
        this.health = Math.max(0, Math.min(health, definition.maxHealth()));
    }

    public DefaultBuildBlock(BuildBlockDefinition definition, Location location, UUID placedBy) {
        this(UUID.randomUUID(), definition, location, placedBy, System.currentTimeMillis(), definition.maxHealth());
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public BuildBlockDefinition definition() {
        return definition;
    }

    @Override
    public Location location() {
        return location.clone();
    }

    @Override
    public Optional<UUID> placedBy() {
        return Optional.ofNullable(placedBy);
    }

    @Override
    public long placedAt() {
        return placedAt;
    }

    @Override
    public double health() {
        return health;
    }

    @Override
    public double damage(double amount) {
        if (amount <= 0) return health;
        this.health = Math.max(0, this.health - amount);
        return this.health;
    }

    @Override
    public double repair(double amount) {
        if (amount <= 0) return health;
        final double maxHealth = definition.maxHealth();
        this.health = Math.min(maxHealth, this.health + amount);
        return this.health;
    }
}