package com.elerandir.brunobatcheditor.model;

import java.nio.file.Path;

/**
 * Parsed CLI arguments for a single {@code enable-bearer-auth} run, bound into the Dagger
 * graph via {@code @BindsInstance} so injected services can depend on it directly.
 */
public record EnableBearerAuthConfig(Path targetPath, String tokenVariable, boolean dryRun) {
}
