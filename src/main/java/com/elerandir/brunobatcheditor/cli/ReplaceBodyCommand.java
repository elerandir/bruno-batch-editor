package com.elerandir.brunobatcheditor.cli;

import com.elerandir.brunobatcheditor.AppComponent;
import com.elerandir.brunobatcheditor.BatchProcessor;
import com.elerandir.brunobatcheditor.DaggerAppComponent;
import com.elerandir.brunobatcheditor.model.ReplacementResult;
import com.elerandir.brunobatcheditor.model.RuntimeConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParameterException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "bruno-batch-editor",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        description = "Batch-replace a literal string in every request body across .bru files."
)
public class ReplaceBodyCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "PATH", description = "A .bru file, or a directory searched recursively for .bru files.")
    Path targetPath;

    @Option(names = {"-s", "--search"}, required = true, description = "Literal string to find in each request body.")
    String search;

    @Option(names = {"-r", "--replace"}, required = true, description = "Replacement string.")
    String replacement;

    @Option(names = {"--dry-run"}, description = "Report what would change without writing any files.")
    boolean dryRun;

    @Override
    public Integer call() {
        if (search.isEmpty()) {
            throw new ParameterException(new CommandLine(this), "--search must not be empty");
        }
        RuntimeConfig config = new RuntimeConfig(targetPath, search, replacement, dryRun);
        AppComponent component = DaggerAppComponent.factory().create(config);
        BatchProcessor processor = new BatchProcessor(
                config, component.bruParser(), component.bodyBlockReplacer(), component.bruFileLocator());

        List<ReplacementResult> results;
        try {
            results = processor.run();
        } catch (IOException e) {
            System.err.println("Failed to locate .bru files under " + targetPath + ": " + e.getMessage());
            return 1;
        }
        return report(results);
    }

    private int report(List<ReplacementResult> results) {
        int totalReplacements = 0;
        int filesChanged = 0;
        int errors = 0;
        String verb = dryRun ? "would replace" : "replaced";

        for (ReplacementResult result : results) {
            if (result.isError()) {
                errors++;
                System.err.println(result.file() + ": ERROR " + result.error());
            } else if (result.replacementCount() > 0) {
                filesChanged++;
                totalReplacements += result.replacementCount();
                System.out.println(result.file() + ": " + verb + " " + result.replacementCount() + " occurrence(s)");
            }
        }

        System.out.println((dryRun ? "Dry run: " : "") + totalReplacements + " occurrence(s) across "
                + filesChanged + " file(s) (" + results.size() + " scanned, " + errors + " error(s))");
        return errors > 0 ? 1 : 0;
    }
}
