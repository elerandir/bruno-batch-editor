package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BearerAuthResult;
import com.elerandir.brunobatcheditor.model.EnableBearerAuthConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EnableBearerAuthProcessor")
class EnableBearerAuthProcessorTest {

    private static final String REQUEST_TEMPLATE = """
            meta {
              name: %s
              type: http
              seq: 1
            }

            get {
              url: https://api.example.com/resource
            }
            """;

    private EnableBearerAuthProcessor buildProcessor(EnableBearerAuthConfig config) {
        EnableBearerAuthComponent component = DaggerEnableBearerAuthComponent.factory().create(config);
        return new EnableBearerAuthProcessor(config, component.bruParser(), component.bearerAuthEnabler());
    }

    private void writeRequest(Path file, String name) throws IOException {
        Files.writeString(file, REQUEST_TEMPLATE.formatted(name), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("given a plain request outside any auth/token folder")
    class GivenEligibleRequest {

        @Test
        @DisplayName("when run, enables bearer auth and reports it as enabled")
        void enablesBearerAuth(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("get-resource.bru");
            writeRequest(file, "Get Resource");

            EnableBearerAuthConfig config = new EnableBearerAuthConfig(file, "jwt", false);
            List<BearerAuthResult> results = buildProcessor(config).run();

            assertThat(results).singleElement().satisfies(result -> {
                assertThat(result.isError()).isFalse();
                assertThat(result.skipped()).isFalse();
                assertThat(result.enabled()).isTrue();
            });
            assertThat(Files.readString(file)).contains("auth {\n  mode: bearer\n}", "token: {{jwt}}");
        }
    }

    @Nested
    @DisplayName("given a request named after a token")
    class GivenTokenNamedRequest {

        @Test
        @DisplayName("when run, skips the request and leaves the file untouched")
        void skipsRequest(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("refresh-token.bru");
            writeRequest(file, "Refresh Token");
            String original = Files.readString(file);

            EnableBearerAuthConfig config = new EnableBearerAuthConfig(file, "jwt", false);
            List<BearerAuthResult> results = buildProcessor(config).run();

            assertThat(results).singleElement().satisfies(result -> assertThat(result.skipped()).isTrue());
            assertThat(Files.readString(file)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("given a request inside an auth folder")
    class GivenRequestInAuthFolder {

        @Test
        @DisplayName("when run, skips every request in that folder")
        void skipsFolderContents(@TempDir Path tempDir) throws IOException {
            Path authDir = Files.createDirectories(tempDir.resolve("auth"));
            Path login = authDir.resolve("login.bru");
            writeRequest(login, "Login");
            String original = Files.readString(login);

            EnableBearerAuthConfig config = new EnableBearerAuthConfig(tempDir, "jwt", false);
            List<BearerAuthResult> results = buildProcessor(config).run();

            assertThat(results)
                    .filteredOn(result -> result.file().equals(login))
                    .singleElement()
                    .satisfies(result -> assertThat(result.skipped()).isTrue());
            assertThat(Files.readString(login)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("given a dry run")
    class GivenDryRun {

        @Test
        @DisplayName("when run, reports the change but leaves the file untouched")
        void reportsWithoutWriting(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("get-resource.bru");
            writeRequest(file, "Get Resource");
            String original = Files.readString(file);

            EnableBearerAuthConfig config = new EnableBearerAuthConfig(file, "jwt", true);
            List<BearerAuthResult> results = buildProcessor(config).run();

            assertThat(results).singleElement().satisfies(result -> assertThat(result.enabled()).isTrue());
            assertThat(Files.readString(file)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("given a malformed .bru file among valid ones")
    class GivenMalformedFile {

        @Test
        @DisplayName("when run, reports an error for the bad file and still processes the rest")
        void reportsErrorWithoutAbortingBatch(@TempDir Path tempDir) throws IOException {
            Path good = tempDir.resolve("good.bru");
            Path bad = tempDir.resolve("bad.bru");
            writeRequest(good, "Good");
            Files.writeString(bad, "meta {\n  name: Broken\n", StandardCharsets.UTF_8);

            EnableBearerAuthConfig config = new EnableBearerAuthConfig(tempDir, "jwt", false);
            List<BearerAuthResult> results = buildProcessor(config).run();

            assertThat(results)
                    .filteredOn(result -> result.file().equals(bad))
                    .singleElement()
                    .satisfies(result -> {
                        assertThat(result.isError()).isTrue();
                        assertThat(result.error()).contains("Unbalanced braces");
                    });
            assertThat(results)
                    .filteredOn(result -> result.file().equals(good))
                    .singleElement()
                    .satisfies(result -> assertThat(result.enabled()).isTrue());
        }
    }
}
