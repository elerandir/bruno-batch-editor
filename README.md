# bruno-batch-editor

[![CI](https://github.com/elerandir/bruno-batch-editor/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/elerandir/bruno-batch-editor/actions/workflows/ci.yml)
[![CodeQL](https://github.com/elerandir/bruno-batch-editor/actions/workflows/codeql.yml/badge.svg?branch=main)](https://github.com/elerandir/bruno-batch-editor/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/elerandir/bruno-batch-editor/badge)](https://securityscorecards.dev/viewer/?uri=github.com/elerandir/bruno-batch-editor)
[![Secret scan](https://github.com/elerandir/bruno-batch-editor/actions/workflows/gitleaks.yml/badge.svg?branch=main)](https://github.com/elerandir/bruno-batch-editor/actions/workflows/gitleaks.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Built with Gradle](https://img.shields.io/badge/Built%20with-Gradle-02303A.svg?logo=gradle)](https://gradle.org)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A CLI tool that batch-edits [Bruno](https://www.usebruno.com/) `.bru` request files under
a given path, leaving the rest of each file byte-for-byte untouched.

## Usage

Build a standalone install once:

```sh
./gradlew installDist
```

This produces `build/install/bruno-batch-editor/`, with a launcher script at
`bin/bruno-batch-editor` that bundles its own classpath — no Gradle or `-jar` flags
needed afterward. Optionally put it on your `PATH`:

```sh
ln -s "$(pwd)/build/install/bruno-batch-editor/bin/bruno-batch-editor" /usr/local/bin/bruno-batch-editor
```

Then run one of the subcommands below. During development, `./gradlew run --args="..."`
works too (compiles and runs in one step, no `installDist` needed):

```sh
./gradlew run --args="replace-body ./requests --search old-api.example.com --replace new-api.example.com"
```

### `replace-body`

Replaces a literal string with a new one wherever it appears inside a request's body.

```sh
bruno-batch-editor replace-body <PATH> --search <TEXT> --replace <TEXT> [--dry-run]
```

Example:

```sh
bruno-batch-editor replace-body ./requests --search old-api.example.com --replace new-api.example.com
```

| Option              | Required | Description                                                        |
|----------------------|----------|----------------------------------------------------------------------|
| `PATH`               | yes      | A `.bru` file, or a directory to search recursively.                |
| `-s, --search`       | yes      | Literal string to find inside each request body.                    |
| `-r, --replace`      | yes      | Replacement string.                                                  |
| `--dry-run`          | no       | Report what would change without writing any files.                 |
| `-h, --help`         | no       | Show usage help.                                                      |
| `-V, --version`      | no       | Show version information.                                             |

Only text inside `body`/`body:*` blocks (e.g. `body:json`, `body:text`, `body:graphql`) is
searched and rewritten. URLs, headers, scripts, and every other block are left exactly as
they were.

### `enable-bearer-auth`

Sets every eligible request's `auth` block to `mode: bearer` and adds an `auth:bearer` block
referencing a Bruno variable for the token (`{{jwt}}` by default), so you can define that
variable once (e.g. on the collection or an environment) and reuse it everywhere.

```sh
bruno-batch-editor enable-bearer-auth <PATH> [--token-var <NAME>] [--dry-run]
```

Example:

```sh
bruno-batch-editor enable-bearer-auth ./requests
```

A request is skipped, left completely untouched, if either is true:

- its name (the `meta { name: ... }` field, falling back to the filename) contains "token"
  or "jwt", case-insensitively — these are assumed to already be about managing tokens
  themselves;
- it lives directly inside a folder named `auth` or `token`, case-insensitively.

Requests already set to `mode: bearer` are left as-is (their existing `auth:bearer` block,
if any, is not overwritten). Requests with a different `mode` have only that line rewritten;
everything else in the `auth` block is preserved.

| Option              | Required | Description                                                                          |
|----------------------|----------|----------------------------------------------------------------------------------------|
| `PATH`               | yes      | A `.bru` file, or a directory to search recursively.                                  |
| `--token-var`        | no       | Bruno variable referenced by the generated `auth:bearer` token, i.e. `{{VAR}}` (default: `jwt`). |
| `--dry-run`          | no       | Report what would change without writing any files.                                   |
| `-h, --help`         | no       | Show usage help.                                                                        |
| `-V, --version`      | no       | Show version information.                                                               |

## Build

Requires JDK 21. The build uses the currently running JDK, not a downloaded toolchain.

```sh
./gradlew build
```

If you open the project in an IDE, enable the Lombok plugin/annotation processing so
generated code (constructors, utility classes) resolves correctly.

After bumping any dependency version, regenerate the dependency verification metadata so
the build's checksum allowlist stays in sync:

```sh
./gradlew --write-verification-metadata sha256 build
```

## Security / supply chain

- **Dependency verification**: Gradle checksum-verifies every resolved jar against
  `gradle/verification-metadata.xml` (see `gradle/verification-metadata.xml` — metadata-file
  verification is intentionally disabled, see the comment there for why).
- **CodeQL** static analysis runs on every push/PR to `main` and weekly.
- **gitleaks** secret scanning runs on every push/PR to `main` and weekly.
- **Dependency review** blocks PRs that introduce dependencies with known vulnerabilities
  or disallowed licenses.
- **OpenSSF Scorecard** analysis runs weekly and publishes a public score.
- **Dependabot** keeps Gradle and GitHub Actions dependencies current.
- CI workflow runners are hardened via `step-security/harden-runner`.

## License

Apache License 2.0. Copyright © 2026 elerandir. See [LICENSE](LICENSE) for details.
