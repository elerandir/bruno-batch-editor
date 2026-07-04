package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BodyReplacementOutcome;
import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.BruNode;
import com.elerandir.brunobatcheditor.model.RuntimeConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/** Replaces literal occurrences of a search string inside every {@code body*} block. */
@Singleton
public class BodyBlockReplacer {

    private final RuntimeConfig config;

    @Inject
    public BodyBlockReplacer(RuntimeConfig config) {
        this.config = config;
    }

    public BodyReplacementOutcome replace(BruDocument document) {
        List<BruNode> nodes = new ArrayList<>();
        int replacementCount = 0;
        for (BruNode node : document.nodes()) {
            if (node instanceof BruNode.Block block && isBodyBlock(block.name())) {
                replacementCount += countOccurrences(block.content(), config.search());
                String newContent = block.content().replace(config.search(), config.replacement());
                nodes.add(new BruNode.Block(block.header(), block.name(), newContent, block.closeText()));
            } else {
                nodes.add(node);
            }
        }
        return new BodyReplacementOutcome(new BruDocument(nodes), replacementCount);
    }

    private static boolean isBodyBlock(String name) {
        return name.equals(BruConstants.BODY_BLOCK_NAME) || name.startsWith(BruConstants.BODY_BLOCK_PREFIX);
    }

    private static int countOccurrences(String text, String target) {
        if (target.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }
}
