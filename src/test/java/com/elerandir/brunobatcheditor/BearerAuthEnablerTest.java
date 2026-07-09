package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BearerAuthOutcome;
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
    @DisplayName("given a request with no auth field at all")
    class GivenNoAuthField {

        @Test
        @DisplayName("when enabled, adds auth: bearer to the method block and an auth:bearer block")
        void addsAuthFieldAndBlock() {
            BearerAuthOutcome outcome = enable(REQUEST_WITHOUT_AUTH, "jwt");

            assertThat(outcome.changed()).isTrue();
            String rendered = parser.render(outcome.document());
            assertThat(rendered).contains("get {\n  url: https://api.example.com/users\n  auth: bearer\n}");
            assertThat(rendered).contains("auth:bearer {\n  token: {{jwt}}\n}");
        }
    }

    @Nested
    @DisplayName("given a request whose method block is set to a different auth mode")
    class GivenDifferentMode {

        @Test
        @DisplayName("when enabled, rewrites only the auth field to bearer")
        void rewritesAuthFieldOnly() {
            String request = """
                    meta {
                      name: Get Users
                      type: http
                      seq: 1
                    }

                    get {
                      url: https://api.example.com/users
                      auth: basic
                    }
                    """;

            BearerAuthOutcome outcome = enable(request, "jwt");

            assertThat(outcome.changed()).isTrue();
            String rendered = parser.render(outcome.document());
            assertThat(rendered).contains("get {\n  url: https://api.example.com/users\n  auth: bearer\n}");
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

                    get {
                      url: https://api.example.com/users
                      auth: bearer
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

    @Nested
    @DisplayName("given a request with no recognizable HTTP method block")
    class GivenNoMethodBlock {

        @Test
        @DisplayName("when enabled, reports no change and leaves the file untouched")
        void reportsNoChange() {
            String request = """
                    meta {
                      name: Folder settings
                      type: http
                    }
                    """;

            BearerAuthOutcome outcome = enable(request, "jwt");

            assertThat(outcome.changed()).isFalse();
            assertThat(parser.render(outcome.document())).isEqualTo(request);
        }
    }
}
