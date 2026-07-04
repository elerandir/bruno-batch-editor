package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.RuntimeConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Resolves the {@code .bru} file(s) a run should touch: a single file, or a directory walked recursively. */
@Singleton
public class BruFileLocator {

    private final RuntimeConfig config;

    @Inject
    public BruFileLocator(RuntimeConfig config) {
        this.config = config;
    }

    public List<Path> locate() throws IOException {
        Path target = config.targetPath();
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
