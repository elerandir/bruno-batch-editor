# Security Policy

## Reporting a vulnerability

Please report security vulnerabilities privately using
[GitHub Private Vulnerability Reporting](https://github.com/elerandir/bruno-batch-editor/security/advisories/new)
rather than filing a public issue. You should receive an initial response within 5
business days.

Please include:
- A description of the vulnerability and its potential impact.
- Steps to reproduce, or a proof-of-concept.
- The affected version/commit.

## Threat model

`bruno-batch-editor` is a local CLI tool that reads and rewrites `.bru` files on the
filesystem paths its user explicitly points it at. It does not open network listeners,
make outbound network calls, or execute file contents. The main risks in scope are:

- **Path handling**: the tool should only ever read/write files under the path supplied
  on the command line.
- **Parser robustness**: a malformed or adversarially crafted `.bru` file should cause a
  reported, per-file error — not a crash of the whole batch, memory exhaustion, or
  unintended writes to unrelated files.
- **Supply chain**: the dependencies this tool is built from and against (see below).

Running the tool against untrusted `.bru` files carries the same risk as opening them in
any text-processing tool: treat directories from untrusted sources with the same caution
you would apply elsewhere.

## Supply-chain controls

- **Gradle dependency verification** — every resolved dependency jar is checksum-verified
  against `gradle/verification-metadata.xml`; a build fails if a jar doesn't match.
- **CodeQL** static analysis on every push/PR to `main` and weekly.
- **gitleaks** secret scanning on every push/PR to `main` and weekly.
- **Dependency review** on every PR, blocking known-vulnerable or disallowed-license
  dependencies.
- **OpenSSF Scorecard** analysis, published weekly.
- **Dependabot** for automated dependency and GitHub Actions updates.
- **Harden-Runner** audits/restricts outbound network access from CI runners.
