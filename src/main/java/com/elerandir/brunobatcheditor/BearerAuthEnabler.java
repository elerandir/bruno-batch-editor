package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BearerAuthOutcome;
import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.BruNode;
import com.elerandir.brunobatcheditor.model.EnableBearerAuthConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ensures a request has an {@code auth { mode: bearer }} block and a corresponding
 * {@code auth:bearer} block referencing a token variable, adding or fixing up whichever of
 * the two is missing or set to something other than bearer.
 */
@Singleton
public class BearerAuthEnabler {

    private static final Pattern MODE_LINE = Pattern.compile("(?m)^([ \t]*mode:[ \t]*).*$");

    private final EnableBearerAuthConfig config;

    @Inject
    public BearerAuthEnabler(EnableBearerAuthConfig config) {
        this.config = config;
    }

    public BearerAuthOutcome enable(BruDocument document) {
        List<BruNode> nodes = new ArrayList<>(document.nodes());
        boolean changed = setBearerMode(nodes);

        if (indexOfBlock(nodes, BruConstants.AUTH_BEARER_BLOCK_NAME) == -1) {
            appendBlock(nodes, BruConstants.AUTH_BEARER_BLOCK_NAME, "  token: {{" + config.tokenVariable() + "}}\n");
            changed = true;
        }

        return new BearerAuthOutcome(new BruDocument(nodes), changed);
    }

    private static boolean setBearerMode(List<BruNode> nodes) {
        int authIndex = indexOfBlock(nodes, BruConstants.AUTH_BLOCK_NAME);
        if (authIndex == -1) {
            appendBlock(nodes, BruConstants.AUTH_BLOCK_NAME, "  mode: bearer\n");
            return true;
        }

        BruNode.Block authBlock = (BruNode.Block) nodes.get(authIndex);
        Matcher modeMatcher = MODE_LINE.matcher(authBlock.content());
        if (modeMatcher.find()) {
            if (modeMatcher.group().trim().equals("mode: bearer")) {
                return false;
            }
            String newContent = modeMatcher.replaceFirst(Matcher.quoteReplacement(modeMatcher.group(1) + "bearer"));
            nodes.set(authIndex, new BruNode.Block(authBlock.header(), authBlock.name(), newContent, authBlock.closeText()));
            return true;
        }

        String newContent = ensureTrailingNewline(authBlock.content()) + "  mode: bearer\n";
        nodes.set(authIndex, new BruNode.Block(authBlock.header(), authBlock.name(), newContent, authBlock.closeText()));
        return true;
    }

    private static int indexOfBlock(List<BruNode> nodes, String name) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) instanceof BruNode.Block block && block.name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static void appendBlock(List<BruNode> nodes, String name, String content) {
        ensureTrailingBlankLine(nodes);
        nodes.add(new BruNode.Block(name + " {\n", name, content, "}"));
        nodes.add(new BruNode.PlainText("\n"));
    }

    private static void ensureTrailingBlankLine(List<BruNode> nodes) {
        if (nodes.isEmpty()) {
            return;
        }
        int last = nodes.size() - 1;
        if (nodes.get(last) instanceof BruNode.PlainText(String text)) {
            nodes.set(last, new BruNode.PlainText(ensureBlankLine(text)));
        } else {
            nodes.add(new BruNode.PlainText("\n\n"));
        }
    }

    private static String ensureBlankLine(String text) {
        String withNewline = ensureTrailingNewline(text);
        return withNewline.endsWith("\n\n") ? withNewline : withNewline + "\n";
    }

    private static String ensureTrailingNewline(String text) {
        return text.endsWith("\n") ? text : text + "\n";
    }
}
