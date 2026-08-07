package it.yteicos.toolcupboards.core.config;

import it.yteicos.toolcupboards.api.block.BuildBlockDefinition;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads {@link BuildBlockDefinition}s from the {@code blocks/} folder.
 */
public interface BlockDefinitionLoader {

    /**
     * Loads every definition found in the folder.
     * <p>
     * A file that fails to parse is skipped and logged; one broken file must never stop the rest
     * of the server's blocks from loading.
     *
     * @param blocksFolder the folder to scan, created if missing
     * @return the successfully parsed definitions, in file-name order
     */
    List<BuildBlockDefinition> loadAll(Path blocksFolder);

    /**
     * Thrown when a single definition file is malformed.
     */
    class InvalidDefinitionException extends RuntimeException {

        /**
         * @param file    the offending file
         * @param message what is wrong with it
         */
        public InvalidDefinitionException(final Path file, final String message) {
            super(file.getFileName() + ": " + message);
        }

        /**
         * @param file    the offending file
         * @param message what is wrong with it
         * @param cause   the underlying failure
         */
        public InvalidDefinitionException(final Path file, final String message, final Throwable cause) {
            super(file.getFileName() + ": " + message, cause);
        }
    }
}
