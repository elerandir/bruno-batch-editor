package com.elerandir.brunobatcheditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Resolves the {@code .bru} file(s) a run should touch: a single file, or a directory walked recursively. */
public final class BruFileLocator {

    private BruFileLocator() {
    }

    public static List<Path> locate(Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            return List.of(target);
        }
        try (Stream<Path> paths = Files.walk(target)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(BruConstants.BRU_FILE_EXTENSION))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }
}
