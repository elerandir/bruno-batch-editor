package com.elerandir.brunobatcheditor.model;

import java.nio.file.Path;

public record ReplacementResult(Path file, int replacementCount, String error) {

    public static ReplacementResult success(Path file, int replacementCount) {
        return new ReplacementResult(file, replacementCount, null);
    }

    public static ReplacementResult failure(Path file, String error) {
        return new ReplacementResult(file, 0, error);
    }

    public boolean isError() {
        return error != null;
    }
}
