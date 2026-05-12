package toutouchien.itemsadderadditions.common.util;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility methods for file-system operations used across multiple sub-systems.
 */
@NullMarked
public final class FileUtils {
    private static final String LOG_TAG = "FileUtils";

    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Recursively collects all {@code .yml} files under {@code dir} using NIO
     * {@link Files#walk}, which uses native {@code readdir} iteration and avoids
     * the intermediate {@code File[]} array allocations that {@link File#listFiles()}
     * produces at every directory level.
     *
     * <p>The order of the returned list is undefined and depends on the
     * underlying file system.
     *
     * @param dir the root directory to scan (must exist and be a directory)
     * @return a mutable list of every {@code .yml} file found; never {@code null},
     * but may be empty if the directory is empty or contains no YAML files
     */
    public static List<File> collectYamlFiles(File dir) {
        List<File> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            stream
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.toString().endsWith(".yml"))
                    .forEach(p -> result.add(p.toFile()));
        } catch (IOException e) {
            Log.error(LOG_TAG, "Failed to walk directory: " + dir.getPath(), e);
        }
        return result;
    }
}
