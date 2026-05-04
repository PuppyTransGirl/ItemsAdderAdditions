package toutouchien.itemsadderadditions.utils;

import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for file-system operations used across multiple sub-systems.
 */
@NullMarked
public final class FileUtils {
    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Recursively collects all {@code .yml} files under {@code dir}.
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
        collectRecursive(dir, result);
        return result;
    }

    private static void collectRecursive(File dir, List<File> accumulator) {
        File[] children = dir.listFiles();
        if (children == null)
            return;

        for (File child : children) {
            if (child.isDirectory())
                collectRecursive(child, accumulator);
            else if (child.getName().endsWith(".yml"))
                accumulator.add(child);
        }
    }
}
