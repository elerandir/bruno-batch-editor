package com.elerandir.brunobatcheditor.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "bruno-batch-editor",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        description = "Batch-edit Bruno .bru request files.",
        subcommands = {ReplaceBodyCommand.class, EnableBearerAuthCommand.class}
)
public class RootCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
