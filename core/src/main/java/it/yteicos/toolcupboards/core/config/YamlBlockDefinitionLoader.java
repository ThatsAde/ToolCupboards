package it.yteicos.toolcupboards.core.config;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;
import it.yteicos.toolcupboards.api.block.BuildBlockType;
import it.yteicos.toolcupboards.api.crafting.RecipeDefinition;
import it.yteicos.toolcupboards.api.item.ItemKey;
import it.yteicos.toolcupboards.api.shop.ShopEntry;
import it.yteicos.toolcupboards.api.upkeep.UpkeepRequirement;
import it.yteicos.toolcupboards.core.block.DefaultBuildBlockDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Reads {@code blocks/*.yml} into {@link DefaultBuildBlockDefinition}s.
 * <p>
 * Each file is parsed on its own: a malformed one is logged and skipped so the rest of the server's
 * blocks still load. The definition id is the file name without its extension, lower-cased.
 */
public final class YamlBlockDefinitionLoader implements BlockDefinitionLoader {

    /**
     * Path separator used while parsing block files.
     * <p>
     * Item keys are section names {@code mmoitems:MATERIAL.STEEL} and Bukkit would split them
     * into nested sections on the default {@code '.'}. Swapping the separator before {@code load()}
     * keeps such keys whole; every path below therefore uses {@code /}.
     */
    private static final char PATH_SEPARATOR = '/';

    private static final String YAML_EXTENSION = ".yml";

    private final Logger logger;

    /**
     * @param logger where skipped files are reported
     */
    public YamlBlockDefinitionLoader(final Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public List<BuildBlockDefinition> loadAll(final Path blocksFolder) {
        Objects.requireNonNull(blocksFolder, "blocksFolder");
        try {
            Files.createDirectories(blocksFolder);
        } catch (final IOException failure) {
            logger.severe("Cannot create the blocks folder " + blocksFolder + ": " + failure.getMessage());
            return List.of();
        }

        final List<Path> files;
        try (Stream<Path> entries = Files.list(blocksFolder)) {
            files = entries
                    .filter(Files::isRegularFile)
                    .filter(path -> fileName(path).endsWith(YAML_EXTENSION))
                    .sorted(Comparator.comparing(YamlBlockDefinitionLoader::fileName))
                    .toList();
        } catch (final IOException failure) {
            logger.severe("Cannot list the blocks folder " + blocksFolder + ": " + failure.getMessage());
            return List.of();
        }

        final List<BuildBlockDefinition> definitions = new ArrayList<>(files.size());
        for (final Path file : files) {
            try {
                definitions.add(load(file));
            } catch (final InvalidDefinitionException invalid) {
                logger.warning("Skipping block definition " + invalid.getMessage());
            } catch (final RuntimeException unexpected) {
                logger.warning("Skipping block definition " + file.getFileName() + ": " + unexpected);
            }
        }
        return List.copyOf(definitions);
    }

    /**
     * Parses a single file.
     *
     * @param file the file to read
     * @return the definition it declares
     * @throws InvalidDefinitionException if the file cannot be read or is malformed
     */
    private BuildBlockDefinition load(final Path file) {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator(PATH_SEPARATOR);
        try {
            yaml.load(file.toFile());
        } catch (final IOException | InvalidConfigurationException failure) {
            throw new InvalidDefinitionException(file, "cannot be parsed", failure);
        }

        if (!ConfigVersion.isCurrent(yaml)) {
            throw new InvalidDefinitionException(file, ConfigVersion.describeMismatch(yaml));
        }

        final ConfigurationSection settings = requireSection(yaml, file, "settings");
        final String blockName = requireString(settings, file, "block-name");
        final BuildBlockType type = readType(settings, file);
        final double maxHealth = settings.getDouble("block-health", 0.0D);
        if (maxHealth <= 0.0D) {
            throw new InvalidDefinitionException(file, "settings/block-health must be positive");
        }

        final ItemKey model = readModel(yaml, file);

        try {
            return new DefaultBuildBlockDefinition(
                    idOf(file),
                    blockName,
                    type,
                    maxHealth,
                    readUpkeep(settings.getConfigurationSection("required-materials"), file),
                    readShop(yaml.getConfigurationSection("shop-settings"), file),
                    model,
                    model,
                    readRecipe(yaml.getConfigurationSection("crafting-recipe"), file));
        } catch (final IllegalArgumentException rejected) {
            throw new InvalidDefinitionException(file, rejected.getMessage());
        }
    }

    private static BuildBlockType readType(final ConfigurationSection settings, final Path file) {
        final String raw = requireString(settings, file, "block-type");
        try {
            return BuildBlockType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException unknown) {
            throw new InvalidDefinitionException(file,
                    "settings/block-type must be FOUNDATION or STRUCTURE, got '" + raw + "'");
        }
    }

    /**
     * Reads {@code model-settings.item-type}, the single key deciding both the held item and the
     * placed appearance. A value without a namespace resolves to a plain Minecraft item.
     */
    private static ItemKey readModel(final ConfigurationSection yaml, final Path file) {
        final ConfigurationSection model = requireSection(yaml, file, "model-settings");
        final String raw = requireString(model, file, "item-type");
        try {
            return ItemKey.parse(raw);
        } catch (final IllegalArgumentException malformed) {
            throw new InvalidDefinitionException(file,
                    "model-settings/item-type is not a valid item key: " + malformed.getMessage());
        }
    }

    private static List<UpkeepRequirement> readUpkeep(final ConfigurationSection materials, final Path file) {
        if (materials == null) {
            return List.of();
        }

        final List<UpkeepRequirement> requirements = new ArrayList<>();
        for (final String rawKey : materials.getKeys(false)) {
            final ConfigurationSection entry = materials.getConfigurationSection(rawKey);
            if (entry == null) {
                throw new InvalidDefinitionException(file,
                        "settings/required-materials/" + rawKey + " must be a section");
            }

            final int frequency = entry.getInt("frequency-check", 0);
            if (frequency <= 0) {
                throw new InvalidDefinitionException(file,
                        "settings/required-materials/" + rawKey + "/frequency-check must be positive");
            }

            final Duration unit = timeUnit(requireString(entry, file, "time-unit"), rawKey, file);
            try {
                requirements.add(new UpkeepRequirement(
                        ItemKey.parse(rawKey),
                        entry.getInt("amount", 0),
                        unit.multipliedBy(frequency)));
            } catch (final IllegalArgumentException rejected) {
                throw new InvalidDefinitionException(file,
                        "settings/required-materials/" + rawKey + ": " + rejected.getMessage());
            }
        }
        return List.copyOf(requirements);
    }

    private static Duration timeUnit(final String raw, final String materialKey, final Path file) {
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "SECONDS" -> Duration.ofSeconds(1L);
            case "MINUTES" -> Duration.ofMinutes(1L);
            case "HOURS" -> Duration.ofHours(1L);
            case "DAYS" -> Duration.ofDays(1L);
            default -> throw new InvalidDefinitionException(file, "settings/required-materials/"
                    + materialKey + "/time-unit must be SECONDS, MINUTES, HOURS or DAYS, got '" + raw + "'");
        };
    }

    private static ShopEntry readShop(final ConfigurationSection shop, final Path file) {
        if (shop == null) {
            return ShopEntry.DISABLED;
        }
        try {
            return new ShopEntry(
                    shop.getBoolean("enabled", false),
                    shop.getDouble("price", 0.0D),
                    shop.getInt("amount-available", ShopEntry.UNLIMITED));
        } catch (final IllegalArgumentException rejected) {
            throw new InvalidDefinitionException(file, "shop-settings: " + rejected.getMessage());
        }
    }

    private static RecipeDefinition readRecipe(final ConfigurationSection recipe, final Path file) {
        if (recipe == null) {
            return null;
        }

        final List<String> pattern = recipe.getStringList("pattern");
        if (pattern.isEmpty()) {
            throw new InvalidDefinitionException(file, "crafting-recipe/pattern must list 1..3 rows");
        }

        final Map<Character, ItemKey> ingredients = new LinkedHashMap<>();
        for (final String slot : recipe.getKeys(false)) {
            if ("pattern".equals(slot) || "result-amount".equals(slot)) {
                continue;
            }
            if (slot.length() != 1) {
                throw new InvalidDefinitionException(file,
                        "crafting-recipe/" + slot + " must be a single pattern character");
            }
            final String rawItem = recipe.getString(slot);
            if (rawItem == null || rawItem.isBlank()) {
                throw new InvalidDefinitionException(file, "crafting-recipe/" + slot + " has no item");
            }
            ingredients.put(slot.charAt(0), ItemKey.parse(rawItem));
        }

        try {
            return new RecipeDefinition(pattern, ingredients, recipe.getInt("result-amount", 1));
        } catch (final IllegalArgumentException rejected) {
            throw new InvalidDefinitionException(file, "crafting-recipe: " + rejected.getMessage());
        }
    }

    private static ConfigurationSection requireSection(final ConfigurationSection parent,
                                                       final Path file,
                                                       final String path) {
        final ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new InvalidDefinitionException(file, "missing section '" + path + "'");
        }
        return section;
    }

    private static String requireString(final ConfigurationSection section,
                                        final Path file,
                                        final String path) {
        final String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new InvalidDefinitionException(file,
                    "missing '" + section.getCurrentPath() + PATH_SEPARATOR + path + "'");
        }
        return value;
    }

    /** @return the file name without its extension, lower-cased — the definition id */
    private static String idOf(final Path file) {
        final String name = fileName(file);
        return name.substring(0, name.length() - YAML_EXTENSION.length());
    }

    private static String fileName(final Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT);
    }
}
