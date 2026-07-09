package com.elerandir.brunobatcheditor.cli;

import com.elerandir.brunobatcheditor.DaggerEnableBearerAuthComponent;
import com.elerandir.brunobatcheditor.EnableBearerAuthComponent;
import com.elerandir.brunobatcheditor.EnableBearerAuthProcessor;
import com.elerandir.brunobatcheditor.model.BearerAuthResult;
import com.elerandir.brunobatcheditor.model.EnableBearerAuthConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "enable-bearer-auth",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        description = "Batch-enable bearer auth across .bru requests, skipping requests already named "
                + "after a token/JWT or located in an auth/token folder."
)
public class EnableBearerAuthCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "PATH", description = "A .bru file, or a directory searched recursively for .bru files.")
    Path targetPath;

    @Option(names = {"--token-var"}, defaultValue = "jwt",
            description = "Name of the Bruno variable referenced by the generated auth:bearer token, "
                    + "i.e. {{VAR}} (default: ${DEFAULT-VALUE}).")
    String tokenVariable;

    @Option(names = {"--dry-run"}, description = "Report what would change without writing any files.")
    boolean dryRun;

    @Override
    public Integer call() {
        EnableBearerAuthConfig config = new EnableBearerAuthConfig(targetPath, tokenVariable, dryRun);
        EnableBearerAuthComponent component = DaggerEnableBearerAuthComponent.factory().create(config);
        EnableBearerAuthProcessor processor =
                new EnableBearerAuthProcessor(config, component.bruParser(), component.bearerAuthEnabler());

        List<BearerAuthResult> results;
        try {
            results = processor.run();
        } catch (IOException e) {
            System.err.println("Failed to locate .bru files under " + targetPath + ": " + e.getMessage());
            return 1;
        }
        return report(results);
    }

    private int report(List<BearerAuthResult> results) {
        int enabled = 0;
        int alreadyEnabled = 0;
        int skipped = 0;
        int errors = 0;
        String verb = dryRun ? "would enable" : "enabled";

        for (BearerAuthResult result : results) {
            if (result.isError()) {
                errors++;
                System.err.println(result.file() + ": ERROR " + result.error());
            } else if (result.skipped()) {
                skipped++;
            } else if (result.enabled()) {
                enabled++;
                System.out.println(result.file() + ": " + verb + " bearer auth");
            } else {
                alreadyEnabled++;
            }
        }

        System.out.println((dryRun ? "Dry run: " : "") + enabled + " file(s) " + verb + ", "
                + alreadyEnabled + " already using bearer auth, " + skipped + " skipped ("
                + results.size() + " scanned, " + errors + " error(s))");
        return errors > 0 ? 1 : 0;
    }
}
