package com.elerandir.brunobatcheditor.model;

import java.nio.file.Path;

public record BearerAuthResult(Path file, boolean skipped, boolean enabled, String error) {

    public static BearerAuthResult skipped(Path file) {
        return new BearerAuthResult(file, true, false, null);
    }

    public static BearerAuthResult processed(Path file, boolean enabled) {
        return new BearerAuthResult(file, false, enabled, null);
    }

    public static BearerAuthResult failure(Path file, String error) {
        return new BearerAuthResult(file, false, false, error);
    }

    public boolean isError() {
        return error != null;
    }
}
