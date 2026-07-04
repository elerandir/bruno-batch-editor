package com.elerandir.brunobatcheditor;

/** Thrown when a {@code .bru} file's block braces are unbalanced or malformed. */
public class BruParseException extends RuntimeException {

    public BruParseException(String message) {
        super(message);
    }
}
