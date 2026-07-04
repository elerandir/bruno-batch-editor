package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.BruNode;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a {@code .bru} file into an ordered list of {@link BruNode}s: block headers
 * (e.g. {@code meta {}, body:json {}}) and the plain text between them.
 *
 * <p>A block's closing brace is found by depth-counting rather than indentation, since
 * JSON/GraphQL bodies routinely contain their own unbalanced-looking braces.
 */
@Singleton
public class BruParser {

    private static final Pattern BLOCK_HEADER =
            Pattern.compile("(?m)^([A-Za-z][A-Za-z0-9_-]*(?::[A-Za-z0-9_-]+)*)[ \t]*\\{[ \t]*\r?\n");

    @Inject
    public BruParser() {
    }

    public BruDocument parse(String content) {
        List<BruNode> nodes = new ArrayList<>();
        Matcher matcher = BLOCK_HEADER.matcher(content);
        int pos = 0;
        while (matcher.find(pos)) {
            if (matcher.start() > pos) {
                nodes.add(new BruNode.PlainText(content.substring(pos, matcher.start())));
            }
            String name = matcher.group(1);
            String header = matcher.group(0);
            int blockContentStart = matcher.end();
            int closeIndex = findMatchingBrace(content, blockContentStart);
            String blockContent = content.substring(blockContentStart, closeIndex);
            nodes.add(new BruNode.Block(header, name, blockContent, "}"));
            pos = closeIndex + 1;
        }
        if (pos < content.length()) {
            nodes.add(new BruNode.PlainText(content.substring(pos)));
        }
        return new BruDocument(nodes);
    }

    public String render(BruDocument document) {
        StringBuilder rendered = new StringBuilder();
        for (BruNode node : document.nodes()) {
            rendered.append(switch (node) {
                case BruNode.PlainText(String text) -> text;
                case BruNode.Block(String header, String name, String blockContent, String closeText) ->
                        header + blockContent + closeText;
            });
        }
        return rendered.toString();
    }

    private static int findMatchingBrace(String content, int start) {
        int depth = 1;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new BruParseException("Unbalanced braces: a block starting at offset " + start + " is never closed");
    }
}
