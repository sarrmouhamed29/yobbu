# Contributing to Yóbbu

Thank you for your interest in contributing. Yóbbu is a portfolio project; contributions that demonstrate good engineering judgment are welcome.

## Ground rules

- All work described in [CLAUDE.md](CLAUDE.md) and [docs/PROJECT.md](docs/PROJECT.md) applies to contributors too.
- One phase at a time. Do not start work on a later phase before the current one is merged and verified.
- No secrets in the repository, ever. Use `.env.example` to document new variables.
- CI must be green before requesting a review.

## Getting started

```bash
# 1. Fork and clone
git clone https://github.com/sarrmouhamed29/yobbu.git
cd yobbu

# 2. Copy the secrets template
cp .env.example .env
# Edit .env and fill in CHANGE_ME values

# 3. Start the full stack
make dev
```

## Branch naming

```
feat/<short-description>
fix/<short-description>
chore/<short-description>
docs/<short-description>
```

## Commits

This project enforces [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(order-svc): add order cancellation endpoint
fix(gateway): correct route predicate for /api/v1/couriers
docs(adr): add ADR-0003 for message broker choice
chore(ci): pin docker/build-push-action to v5
```

Allowed types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`, `ci`.

## Pull requests

- Use the PR template.
- One concern per PR. A PR that mixes a feature and unrelated refactoring will be asked to split.
- Link the relevant issue or ADR when applicable.
- Add or update tests. The CI must pass.
- Do not add infrastructure components without justification (see the test in `docs/PROJECT.md` §2).

## Architecture decisions

Significant technical decisions go through the ADR process described in `CLAUDE.md`. Open a draft PR with the ADR before implementing.

## Code style

- **Java:** standard Spring Boot conventions; hexagonal structure as in `docs/design/conventions.md`.
- **TypeScript:** strict mode; no `any`; React functional components.
- No comments that describe *what* the code does — only *why* when non-obvious.

## Questions

Open a GitHub Discussion or an issue with the `question` label.
