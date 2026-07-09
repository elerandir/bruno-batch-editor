package com.elerandir.brunobatcheditor;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class BruConstants {

    public static final String BRU_FILE_EXTENSION = ".bru";
    public static final String BODY_BLOCK_NAME = "body";
    public static final String BODY_BLOCK_PREFIX = "body:";
    public static final String META_BLOCK_NAME = "meta";
    public static final String AUTH_BEARER_BLOCK_NAME = "auth:bearer";

    /**
     * Block names Bruno uses for a request's HTTP method: the lowercase verb itself
     * (e.g. {@code get}, {@code post}) for standard methods, or {@code http} for
     * non-standard ones. This is the block that carries the {@code auth: <mode>} field.
     */
    public static final Set<String> HTTP_METHOD_BLOCK_NAMES =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace", "connect", "http");
}
