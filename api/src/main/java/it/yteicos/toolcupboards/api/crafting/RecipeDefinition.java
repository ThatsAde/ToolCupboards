package it.yteicos.toolcupboards.api.crafting;

import it.yteicos.toolcupboards.api.item.ItemKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A shaped crafting recipe declared in a block definition file under {@code crafting-recipe}.
 * <p>
 * The pattern uses the vanilla convention: up to three rows of up to three characters, where a
 * space means "empty" and every other character must have an entry in {@link #ingredients()}.
 *
 * @param pattern      the rows of the crafting grid, 1..3 entries of equal length 1..3
 * @param ingredients  character to item mapping; keys must cover every non-space pattern character
 * @param resultAmount how many blocks one craft yields, must be positive
 */
public record RecipeDefinition(List<String> pattern, Map<Character, ItemKey> ingredients, int resultAmount) {

    public RecipeDefinition {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(ingredients, "ingredients");
        if (pattern.isEmpty() || pattern.size() > 3) {
            throw new IllegalArgumentException("pattern must have 1..3 rows, got " + pattern.size());
        }
        final int width = pattern.getFirst().length();
        if (width == 0 || width > 3) {
            throw new IllegalArgumentException("pattern rows must be 1..3 characters wide, got " + width);
        }
        for (final String row : pattern) {
            if (row.length() != width) {
                throw new IllegalArgumentException("all pattern rows must share the same width");
            }
            for (final char slot : row.toCharArray()) {
                if (slot != ' ' && !ingredients.containsKey(slot)) {
                    throw new IllegalArgumentException("pattern character '" + slot + "' has no ingredient");
                }
            }
        }
        if (resultAmount <= 0) {
            throw new IllegalArgumentException("resultAmount must be positive, got " + resultAmount);
        }
        pattern = List.copyOf(pattern);
        ingredients = Map.copyOf(ingredients);
    }

    /** @return the grid width in slots */
    public int width() {
        return pattern.getFirst().length();
    }

    /** @return the grid height in slots */
    public int height() {
        return pattern.size();
    }
}
