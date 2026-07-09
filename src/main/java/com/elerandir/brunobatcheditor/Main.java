package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.cli.RootCommand;
import lombok.experimental.UtilityClass;
import picocli.CommandLine;

@UtilityClass
public class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
