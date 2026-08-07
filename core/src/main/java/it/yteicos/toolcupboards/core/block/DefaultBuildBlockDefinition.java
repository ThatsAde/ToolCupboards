package it.yteicos.toolcupboards.core.block;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.block.BuildBlockType;
import it.yteicos.toolcupboards.api.crafting.RecipeDefinition;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.shop.ShopEntry;
import it.yteicos.toolcupboards.api.upkeep.UpkeepRequirement;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable {@link BuildBlockDefinition} produced by the YAML loader.
 */
public record DefaultBuildBlockDefinition(
        String id,
        String blockName,
        BuildBlockType type,
        double maxHealth,
        List<UpkeepRequirement> upkeep,
        ShopEntry shop,
        ItemKey itemModel,
        ItemKey blockTexture,
        RecipeDefinition recipeOrNull
) implements BuildBlockDefinition {

    public DefaultBuildBlockDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blockName, "blockName");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(shop, "shop");
        Objects.requireNonNull(itemModel, "itemModel");
        Objects.requireNonNull(blockTexture, "blockTexture");
        id = id.toLowerCase(Locale.ROOT);
        if (maxHealth <= 0.0D) {
            throw new IllegalArgumentException("block-health must be positive, got " + maxHealth);
        }
        upkeep = List.copyOf(Objects.requireNonNull(upkeep, "upkeep"));
    }

    @Override
    public Optional<RecipeDefinition> recipe() {
        return Optional.ofNullable(recipeOrNull);
    }
}
