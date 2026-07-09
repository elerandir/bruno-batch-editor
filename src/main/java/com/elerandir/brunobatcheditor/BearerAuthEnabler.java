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
 * Ensures a request's HTTP method block has {@code auth: bearer} and that a corresponding
 * {@code auth:bearer} block exists referencing a token variable, adding or fixing up
 * whichever of the two is missing or set to something other than bearer.
 *
 * <p>Bruno stores the active auth mode as an {@code auth: <mode>} field inside the request's
 * method block (e.g. {@code get { ... auth: bearer }}), not as a separate top-level block.
 */
@Singleton
public class BearerAuthEnabler {

    private static final Pattern AUTH_FIELD_LINE = Pattern.compile("(?m)^([ \t]*auth:[ \t]*).*$");

    private final EnableBearerAuthConfig config;

    @Inject
    public BearerAuthEnabler(EnableBearerAuthConfig config) {
        this.config = config;
    }

    public BearerAuthOutcome enable(BruDocument document) {
        List<BruNode> nodes = new ArrayList<>(document.nodes());
        int methodBlockIndex = indexOfMethodBlock(nodes);
        if (methodBlockIndex == -1) {
            return new BearerAuthOutcome(document, false);
        }

        boolean changed = setAuthField(nodes, methodBlockIndex);

        if (indexOfBlock(nodes, BruConstants.AUTH_BEARER_BLOCK_NAME) == -1) {
            appendBlock(nodes, BruConstants.AUTH_BEARER_BLOCK_NAME, "  token: {{" + config.tokenVariable() + "}}\n");
            changed = true;
        }

        return new BearerAuthOutcome(new BruDocument(nodes), changed);
    }

    private static boolean setAuthField(List<BruNode> nodes, int methodBlockIndex) {
        BruNode.Block methodBlock = (BruNode.Block) nodes.get(methodBlockIndex);
        Matcher authMatcher = AUTH_FIELD_LINE.matcher(methodBlock.content());
        if (authMatcher.find()) {
            if (authMatcher.group().trim().equals("auth: bearer")) {
                return false;
            }
            String newContent = authMatcher.replaceFirst(Matcher.quoteReplacement(authMatcher.group(1) + "bearer"));
            nodes.set(methodBlockIndex,
                    new BruNode.Block(methodBlock.header(), methodBlock.name(), newContent, methodBlock.closeText()));
            return true;
        }

        String newContent = ensureTrailingNewline(methodBlock.content()) + "  auth: bearer\n";
        nodes.set(methodBlockIndex,
                new BruNode.Block(methodBlock.header(), methodBlock.name(), newContent, methodBlock.closeText()));
        return true;
    }

    private static int indexOfMethodBlock(List<BruNode> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) instanceof BruNode.Block block
                    && BruConstants.HTTP_METHOD_BLOCK_NAMES.contains(block.name())) {
                return i;
            }
        }
        return -1;
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
