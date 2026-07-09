package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.BruDocument;
import com.elerandir.brunobatcheditor.model.BruNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a request should be excluded from bulk bearer-auth enablement: requests
 * named after a token/JWT, or that live directly inside an "auth"/"token" folder, are assumed
 * to already manage their own authentication and are left untouched.
 */
final class BearerAuthEligibility {

    private static final List<String> EXCLUDED_NAME_KEYWORDS = List.of("token", "jwt");
    private static final List<String> EXCLUDED_DIR_NAMES = List.of("auth", "token");
    private static final Pattern META_NAME = Pattern.compile("(?m)^[ \t]*name:[ \t]*(.*)$");

    private BearerAuthEligibility() {
    }

    static boolean isEligible(Path file, BruDocument document) {
        return !isInExcludedDirectory(file) && !hasExcludedName(requestName(file, document));
    }

    private static boolean isInExcludedDirectory(Path file) {
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent == null || parent.getFileName() == null) {
            return false;
        }
        String dirName = parent.getFileName().toString().toLowerCase(Locale.ROOT);
        return EXCLUDED_DIR_NAMES.contains(dirName);
    }

    private static boolean hasExcludedName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return EXCLUDED_NAME_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private static String requestName(Path file, BruDocument document) {
        for (BruNode node : document.nodes()) {
            if (node instanceof BruNode.Block block && block.name().equals(BruConstants.META_BLOCK_NAME)) {
                Matcher matcher = META_NAME.matcher(block.content());
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        }
        return fileNameWithoutExtension(file);
    }

    private static String fileNameWithoutExtension(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(BruConstants.BRU_FILE_EXTENSION)
                ? fileName.substring(0, fileName.length() - BruConstants.BRU_FILE_EXTENSION.length())
                : fileName;
    }
}
