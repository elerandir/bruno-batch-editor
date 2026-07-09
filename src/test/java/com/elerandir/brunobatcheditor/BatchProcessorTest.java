package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.ReplacementResult;
import com.elerandir.brunobatcheditor.model.RuntimeConfig;
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

@DisplayName("BatchProcessor")
class BatchProcessorTest {

    private static final String REQUEST_TEMPLATE = """
            meta {
              name: %s
              type: http
              seq: 1
            }

            get {
              url: https://old-host.example.com/api
            }

            body:json {
              {
                "host": "old-host.example.com"
              }
            }
            """;

    private BatchProcessor buildProcessor(RuntimeConfig config) {
        AppComponent component = DaggerAppComponent.factory().create(config);
        return new BatchProcessor(config, component.bruParser(), component.bodyBlockReplacer());
    }

    private void writeRequest(Path file, String name) throws IOException {
        Files.writeString(file, REQUEST_TEMPLATE.formatted(name), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("given a single .bru file with a matching body")
    class GivenSingleFile {

        @Test
        @DisplayName("when run, rewrites the file and reports the replacement count")
        void rewritesFileAndReportsCount(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("request.bru");
            writeRequest(file, "Request");

            RuntimeConfig config = new RuntimeConfig(file, "old-host.example.com", "new-host.example.com", false);
            List<ReplacementResult> results = buildProcessor(config).run();

            assertThat(results).singleElement().satisfies(result -> {
                assertThat(result.isError()).isFalse();
                assertThat(result.replacementCount()).isEqualTo(1);
            });
            assertThat(Files.readString(file)).contains("\"host\": \"new-host.example.com\"");
            assertThat(Files.readString(file)).contains("url: https://old-host.example.com/api");
        }
    }

    @Nested
    @DisplayName("given a directory of .bru files, including nested subdirectories")
    class GivenDirectory {

        @Test
        @DisplayName("when run, processes every .bru file recursively and ignores other files")
        void processesEveryBruFileRecursively(@TempDir Path tempDir) throws IOException {
            Path topLevel = tempDir.resolve("top.bru");
            Path nestedDir = Files.createDirectories(tempDir.resolve("nested"));
            Path nested = nestedDir.resolve("nested.bru");
            Path notBru = tempDir.resolve("notes.txt");
            writeRequest(topLevel, "Top");
            writeRequest(nested, "Nested");
            Files.writeString(notBru, "old-host.example.com should be ignored");

            RuntimeConfig config = new RuntimeConfig(tempDir, "old-host.example.com", "new-host.example.com", false);
            List<ReplacementResult> results = buildProcessor(config).run();

            assertThat(results).hasSize(2);
            assertThat(results).allSatisfy(result -> assertThat(result.replacementCount()).isEqualTo(1));
            assertThat(Files.readString(topLevel)).contains("new-host.example.com");
            assertThat(Files.readString(nested)).contains("new-host.example.com");
            assertThat(Files.readString(notBru)).isEqualTo("old-host.example.com should be ignored");
        }
    }

    @Nested
    @DisplayName("given a dry run")
    class GivenDryRun {

        @Test
        @DisplayName("when run, reports the would-be replacement count but leaves the file untouched")
        void reportsWithoutWriting(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("request.bru");
            writeRequest(file, "Request");
            String original = Files.readString(file);

            RuntimeConfig config = new RuntimeConfig(file, "old-host.example.com", "new-host.example.com", true);
            List<ReplacementResult> results = buildProcessor(config).run();

            assertThat(results).singleElement().satisfies(result ->
                    assertThat(result.replacementCount()).isEqualTo(1));
            assertThat(Files.readString(file)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("given a file with no matching text")
    class GivenNoMatch {

        @Test
        @DisplayName("when run, reports zero replacements and leaves the file untouched")
        void reportsZeroReplacements(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("request.bru");
            writeRequest(file, "Request");
            String original = Files.readString(file);

            RuntimeConfig config = new RuntimeConfig(file, "does-not-appear", "irrelevant", false);
            List<ReplacementResult> results = buildProcessor(config).run();

            assertThat(results).singleElement().satisfies(result ->
                    assertThat(result.replacementCount()).isZero());
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

            RuntimeConfig config = new RuntimeConfig(tempDir, "old-host.example.com", "new-host.example.com", false);
            List<ReplacementResult> results = buildProcessor(config).run();

            assertThat(results).hasSize(2);
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
                    .satisfies(result -> assertThat(result.replacementCount()).isEqualTo(1));
            assertThat(Files.readString(good)).contains("new-host.example.com");
        }
    }
}
