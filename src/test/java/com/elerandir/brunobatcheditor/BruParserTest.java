package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.BruNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BruParser")
class BruParserTest {

    private final BruParser parser = new BruParser();

    private static final String REQUEST_WITH_JSON_BODY = """
            meta {
              name: Get Users
              type: http
              seq: 1
            }

            get {
              url: https://api.example.com/users
              body: json
            }

            body:json {
              {
                "filter": {
                  "active": true
                }
              }
            }

            script:post-response {
              console.log("done");
            }
            """;

    @Nested
    @DisplayName("given a well-formed .bru file")
    class GivenWellFormedFile {

        @Test
        @DisplayName("when parsed then rendered, reproduces the original text byte-for-byte")
        void roundTripsByteForByte() {
            BruDocument document = parser.parse(REQUEST_WITH_JSON_BODY);

            String rendered = parser.render(document);

            assertThat(rendered).isEqualTo(REQUEST_WITH_JSON_BODY);
        }

        @Test
        @DisplayName("when parsed then splits blocks by name, in source order")
        void splitsBlocksInOrder() {
            BruDocument document = parser.parse(REQUEST_WITH_JSON_BODY);

            var blockNames = document.nodes().stream()
                    .filter(BruNode.Block.class::isInstance)
                    .map(BruNode.Block.class::cast)
                    .map(BruNode.Block::name)
                    .toList();

            assertThat(blockNames).containsExactly("meta", "get", "body:json", "script:post-response");
        }
    }

    @Nested
    @DisplayName("given a body block whose content contains nested JSON braces")
    class GivenNestedBraces {

        @Test
        @DisplayName("when parsed then finds the true matching closing brace, not the first '}'")
        void tracksBraceDepthAcrossNestedJson() {
            BruDocument document = parser.parse(REQUEST_WITH_JSON_BODY);

            BruNode.Block bodyBlock = document.nodes().stream()
                    .filter(BruNode.Block.class::isInstance)
                    .map(BruNode.Block.class::cast)
                    .filter(block -> block.name().equals("body:json"))
                    .findFirst()
                    .orElseThrow();

            assertThat(bodyBlock.content())
                    .contains("\"active\": true")
                    .doesNotContain("script:post-response");
        }
    }

    @Nested
    @DisplayName("given malformed input")
    class GivenMalformedInput {

        @Test
        @DisplayName("when a block is never closed, throws BruParseException")
        void throwsOnUnbalancedBraces() {
            String malformed = """
                    meta {
                      name: Broken
                    """;

            assertThatThrownBy(() -> parser.parse(malformed))
                    .isInstanceOf(BruParseException.class)
                    .hasMessageContaining("Unbalanced braces");
        }
    }
}
