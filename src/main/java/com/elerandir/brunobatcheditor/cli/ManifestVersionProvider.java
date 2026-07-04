package com.elerandir.brunobatcheditor.cli;

import picocli.CommandLine.IVersionProvider;

public class ManifestVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return new String[]{"bruno-batch-editor " + (version != null ? version : "development")};
    }
}
