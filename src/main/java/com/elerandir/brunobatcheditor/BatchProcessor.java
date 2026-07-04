package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BodyReplacementOutcome;
import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.ReplacementResult;
import com.elerandir.brunobatcheditor.model.RuntimeConfig;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-run worker: not Dagger-managed, built by the CLI command from the injected
 * collaborators plus the parsed {@link RuntimeConfig}.
 */
@RequiredArgsConstructor
public class BatchProcessor {

    private final RuntimeConfig config;
    private final BruParser parser;
    private final BodyBlockReplacer replacer;
    private final BruFileLocator locator;

    public List<ReplacementResult> run() throws IOException {
        List<ReplacementResult> results = new ArrayList<>();
        for (Path file : locator.locate()) {
            results.add(processFile(file));
        }
        return results;
    }

    private ReplacementResult processFile(Path file) {
        try {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            BruDocument document = parser.parse(original);
            BodyReplacementOutcome outcome = replacer.replace(document);
            if (outcome.replacementCount() > 0 && !config.dryRun()) {
                Files.writeString(file, parser.render(outcome.document()), StandardCharsets.UTF_8);
            }
            return ReplacementResult.success(file, outcome.replacementCount());
        } catch (BruParseException | IOException e) {
            return ReplacementResult.failure(file, e.getMessage());
        }
    }
}
