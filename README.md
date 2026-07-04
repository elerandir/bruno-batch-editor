# bruno-batch-editor

[![CI](https://github.com/elerandir/bruno-batch-editor/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/elerandir/bruno-batch-editor/actions/workflows/ci.yml)
[![CodeQL](https://github.com/elerandir/bruno-batch-editor/actions/workflows/codeql.yml/badge.svg?branch=main)](https://github.com/elerandir/bruno-batch-editor/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/elerandir/bruno-batch-editor/badge)](https://securityscorecards.dev/viewer/?uri=github.com/elerandir/bruno-batch-editor)
[![Secret scan](https://github.com/elerandir/bruno-batch-editor/actions/workflows/gitleaks.yml/badge.svg?branch=main)](https://github.com/elerandir/bruno-batch-editor/actions/workflows/gitleaks.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Built with Gradle](https://img.shields.io/badge/Built%20with-Gradle-02303A.svg?logo=gradle)](https://gradle.org)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A CLI tool that batch-edits [Bruno](https://www.usebruno.com/) `.bru` request files — for
example, replacing a string everywhere it appears inside a request's body — while leaving
the rest of each file byte-for-byte untouched.

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

Then run it directly:

```sh
bruno-batch-editor <PATH> --search <TEXT> --replace <TEXT> [--dry-run]
```

`PATH` is either a single `.bru` file or a directory, searched recursively for `.bru`
files. Example:

```sh
bruno-batch-editor ./requests --search old-api.example.com --replace new-api.example.com
```

During development, `./gradlew run --args="..."` works too (compiles and runs in one
step, no `installDist` needed):

```sh
./gradlew run --args="./requests --search old-api.example.com --replace new-api.example.com"
```

### Options

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
