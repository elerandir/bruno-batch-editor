package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BodyReplacementOutcome;
import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.BruNode;
import com.elerandir.brunobatcheditor.model.RuntimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BodyBlockReplacer")
class BodyBlockReplacerTest {

    private final BruParser parser = new BruParser();

    private static final String REQUEST = """
            meta {
              name: Lookup
              type: http
              seq: 1
            }

            get {
              url: https://old-host.example.com/lookup
            }

            body:json {
              {
                "target": "old-host.example.com"
              }
            }

            script:post-response {
              console.log("hit old-host.example.com");
            }
            """;

    private BodyReplacementOutcome replace(String search, String replacement) {
        RuntimeConfig config = new RuntimeConfig(Path.of("unused.bru"), search, replacement, false);
        BodyBlockReplacer replacer = new BodyBlockReplacer(config);
        return replacer.replace(parser.parse(REQUEST));
    }

    @Nested
    @DisplayName("given a request with a matching body and non-body occurrences")
    class GivenMatchingBody {

        @Test
        @DisplayName("when replaced, only rewrites text inside body blocks")
        void replacesOnlyWithinBodyBlocks() {
            BodyReplacementOutcome outcome = replace("old-host.example.com", "new-host.example.com");

            assertThat(outcome.replacementCount()).isEqualTo(1);

            var blocks = outcome.document().nodes().stream()
                    .filter(BruNode.Block.class::isInstance)
                    .map(BruNode.Block.class::cast)
                    .toList();

            assertThat(blocks)
                    .filteredOn(block -> block.name().equals("body:json"))
                    .extracting(BruNode.Block::content)
                    .allSatisfy(content -> assertThat(content).contains("new-host.example.com"));

            assertThat(blocks)
                    .filteredOn(block -> block.name().equals("get"))
                    .extracting(BruNode.Block::content)
                    .allSatisfy(content -> assertThat(content).contains("old-host.example.com"));

            assertThat(blocks)
                    .filteredOn(block -> block.name().equals("script:post-response"))
                    .extracting(BruNode.Block::content)
                    .allSatisfy(content -> assertThat(content).contains("old-host.example.com"));
        }
    }

    @Nested
    @DisplayName("given a search string that never appears in any body")
    class GivenNoMatch {

        @Test
        @DisplayName("when replaced, reports zero replacements and leaves content unchanged")
        void reportsZeroReplacements() {
            BodyReplacementOutcome outcome = replace("does-not-appear", "irrelevant");

            assertThat(outcome.replacementCount()).isZero();
            assertThat(parser.render(outcome.document())).isEqualTo(REQUEST);
        }
    }

    @Nested
    @DisplayName("given the same search string appears multiple times in one body")
    class GivenMultipleOccurrences {

        @Test
        @DisplayName("when replaced, counts and rewrites every occurrence")
        void countsEveryOccurrence() {
            String repeatedBody = """
                    body:json {
                      {
                        "a": "dup",
                        "b": "dup",
                        "c": "dup"
                      }
                    }
                    """;
            BruDocument document = parser.parse(repeatedBody);
            RuntimeConfig config = new RuntimeConfig(Path.of("unused.bru"), "dup", "unique", false);

            BodyReplacementOutcome outcome = new BodyBlockReplacer(config).replace(document);

            assertThat(outcome.replacementCount()).isEqualTo(3);
            assertThat(parser.render(outcome.document())).doesNotContain("dup");
        }
    }
}
