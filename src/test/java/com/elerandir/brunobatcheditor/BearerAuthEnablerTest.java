package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BearerAuthOutcome;
import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.EnableBearerAuthConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BearerAuthEnabler")
class BearerAuthEnablerTest {

    private final BruParser parser = new BruParser();

    private static final String REQUEST_WITHOUT_AUTH = """
            meta {
              name: Get Users
              type: http
              seq: 1
            }

            get {
              url: https://api.example.com/users
            }
            """;

    private BearerAuthOutcome enable(String requestText, String tokenVariable) {
        EnableBearerAuthConfig config = new EnableBearerAuthConfig(Path.of("unused.bru"), tokenVariable, false);
        BearerAuthEnabler enabler = new BearerAuthEnabler(config);
        return enabler.enable(parser.parse(requestText));
    }

    @Nested
    @DisplayName("given a request with no auth block at all")
    class GivenNoAuthBlock {

        @Test
        @DisplayName("when enabled, appends an auth block set to bearer and an auth:bearer block")
        void addsBothBlocks() {
            BearerAuthOutcome outcome = enable(REQUEST_WITHOUT_AUTH, "jwt");

            assertThat(outcome.changed()).isTrue();
            String rendered = parser.render(outcome.document());
            assertThat(rendered).contains("auth {\n  mode: bearer\n}");
            assertThat(rendered).contains("auth:bearer {\n  token: {{jwt}}\n}");
        }
    }

    @Nested
    @DisplayName("given a request whose auth block is set to a different mode")
    class GivenDifferentMode {

        @Test
        @DisplayName("when enabled, rewrites the mode to bearer without touching the rest of the block")
        void rewritesModeOnly() {
            String request = """
                    meta {
                      name: Get Users
                      type: http
                      seq: 1
                    }

                    auth {
                      mode: basic
                    }
                    """;

            BearerAuthOutcome outcome = enable(request, "jwt");

            assertThat(outcome.changed()).isTrue();
            String rendered = parser.render(outcome.document());
            assertThat(rendered).contains("auth {\n  mode: bearer\n}");
            assertThat(rendered).contains("auth:bearer {\n  token: {{jwt}}\n}");
        }
    }

    @Nested
    @DisplayName("given a request already fully configured for bearer auth")
    class GivenAlreadyBearer {

        @Test
        @DisplayName("when enabled, reports no change and leaves the file untouched")
        void reportsNoChange() {
            String request = """
                    meta {
                      name: Get Users
                      type: http
                      seq: 1
                    }

                    auth {
                      mode: bearer
                    }

                    auth:bearer {
                      token: {{jwt}}
                    }
                    """;

            BearerAuthOutcome outcome = enable(request, "jwt");

            assertThat(outcome.changed()).isFalse();
            assertThat(parser.render(outcome.document())).isEqualTo(request);
        }
    }

    @Nested
    @DisplayName("given a custom token variable name")
    class GivenCustomTokenVariable {

        @Test
        @DisplayName("when enabled, references the configured variable")
        void usesConfiguredVariable() {
            BearerAuthOutcome outcome = enable(REQUEST_WITHOUT_AUTH, "accessToken");

            String rendered = parser.render(outcome.document());
            assertThat(rendered).contains("token: {{accessToken}}");
        }
    }
}
