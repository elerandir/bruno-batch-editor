package com.elerandir.brunobatcheditor.model;

import java.nio.file.Path;

/**
 * Parsed CLI arguments for a single run, bound into the Dagger graph via
 * {@code @BindsInstance} so injected services can depend on it directly.
 */
public record RuntimeConfig(Path targetPath, String search, String replacement, boolean dryRun) {
}
