package com.elerandir.brunobatcheditor.model;

/**
 * One segment of a parsed {@code .bru} file, in source order. Concatenating every
 * node's rendered text reproduces the original file byte-for-byte when no block
 * content has been changed.
 */
public sealed interface BruNode {

    record PlainText(String text) implements BruNode {
    }

    record Block(String header, String name, String content, String closeText) implements BruNode {
    }
}
