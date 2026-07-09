package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BearerAuthOutcome;
import com.elerandir.brunobatcheditor.model.BearerAuthResult;
import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.EnableBearerAuthConfig;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-run worker: not Dagger-managed, built by the CLI command from the injected
 * collaborators plus the parsed {@link EnableBearerAuthConfig}.
 */
@RequiredArgsConstructor
public class EnableBearerAuthProcessor {

    private final EnableBearerAuthConfig config;
    private final BruParser parser;
    private final BearerAuthEnabler enabler;

    public List<BearerAuthResult> run() throws IOException {
        List<BearerAuthResult> results = new ArrayList<>();
        for (Path file : BruFileLocator.locate(config.targetPath())) {
            results.add(processFile(file));
        }
        return results;
    }

    private BearerAuthResult processFile(Path file) {
        try {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            BruDocument document = parser.parse(original);
            if (!BearerAuthEligibility.isEligible(file, document)) {
                return BearerAuthResult.skipped(file);
            }

            BearerAuthOutcome outcome = enabler.enable(document);
            if (outcome.changed() && !config.dryRun()) {
                Files.writeString(file, parser.render(outcome.document()), StandardCharsets.UTF_8);
            }
            return BearerAuthResult.processed(file, outcome.changed());
        } catch (BruParseException | IOException e) {
            return BearerAuthResult.failure(file, e.getMessage());
        }
    }
}
