# Contributing on GitHub

This repository is hosted on GitHub. For the full project contribution guide
(setup, coding style, commit messages, testing and licensing), read the
[project CONTRIBUTING guide](../CONTRIBUTING.md) first.

## Issues

- Use the issue templates in `.github/ISSUE_TEMPLATE/`:
  - **Bug report** — reproducible defects
  - **Feature request** — proposed enhancements
  - **Question** — usage and support questions
- Search existing issues before opening a new one.
- Include versions, environment and steps to reproduce for bug reports.

## Pull requests

- Open pull requests against the default branch.
- Fill in the PR template and link the issue it addresses (for example
  `Closes #123`).
- Keep changes focused; separate unrelated changes into their own PRs.
- Make sure the automated checks (`.github/workflows/`) pass before review.
- Do not commit secrets, keystores, `local.properties`, real `.env` files or
  build output.

## Reviews

- At least one maintainer review is expected before merge.
- Address review comments or explain why a suggestion is not applicable.
- Squash or rebase to keep history readable where appropriate.

## Code of conduct and security

- Follow the [Code of Conduct](../CODE_OF_CONDUCT.md) in all interactions.
- Report security issues privately as described in [SECURITY.md](../SECURITY.md).
