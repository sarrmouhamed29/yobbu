# ADR 0009: Diagram format

- **Status:** accepted
- **Date:** 2026-10-28
- **Deciders:** project owner

## Context

Phase 0.3 of `PLAN.md` requires a complete design written as code, readable in pull requests. The diagrams needed cover UML (use case, class, state, sequence, activity, component, deployment), entity-relationship and the C4 model.

Constraints:

- Text sources, diffable and versioned.
- Readable by a reviewer on GitHub.
- Few tools to install.
- Real UML coverage, not approximations.

## Options considered

### A. Mermaid only

- Pros: rendered natively by GitHub in files and pull requests, nothing to install.
- Cons: no real use case, deployment or UML activity diagram (they would be imitated with flowcharts), C4 support still experimental, limited control over layout on large diagrams.

### B. PlantUML for everything

- Pros: covers every diagram type listed above, including entity-relationship and C4 (the C4 library ships with PlantUML). One format, consistent.
- Cons: GitHub does not render `.puml` sources, so rendered SVG files must be committed and kept up to date. Generation needs Docker. Older syntax than Mermaid.

### C. Hybrid: Mermaid for sequence, state, class and ER, PlantUML for the rest

- Pros: native rendering on GitHub for most diagrams.
- Cons: two formats and two styles to maintain, and a choice to make for each diagram. Against the rule of few technologies.

### D. Visual editor (diagrams.net)

- Cons: XML files that cannot be reviewed or diffed. Rejected.

## Decision

**Option B: PlantUML for all diagrams.**

- **Sources and outputs:** each `.puml` source lives next to its rendered `.svg`, both committed. `docs/design/` holds UML and data diagrams; `docs/architecture/` holds C4 and technical views.
- **Generation:** `scripts/diagrams.sh` renders every `.puml` under `docs/` through the official PlantUML Docker image, pinned by digest so the output is reproducible. It needs Docker only; no Java or Graphviz installation on the host. On Windows it runs from Git Bash. `scripts/diagrams.sh --check` renders then fails if any SVG differs from what is committed.
- **Verified:** on the owner's machine, with PlantUML 1.2026.9, a use case diagram and a C4 container diagram were generated, contained the expected text, and re-running produced byte-identical files. The script was tested with no source, a missing SVG, an up-to-date SVG and a stale SVG, and behaved as expected.
- **Not yet done:** the `--check` mode is not run in CI yet; the CI pipeline arrives in phase 0.4. Until then it is run by hand before each commit that changes a diagram.
- **Review in pull requests:** reviewers read the diff of the `.puml` source; the SVG is for viewing.

## Consequences

- Docker becomes a prerequisite to modify diagrams, not to read them.
- Committed SVG files add noise to diffs; reviewers should focus on the sources.
- PlantUML's own license (GPL) applies to the tool, not to the diagrams produced; the image is used as an external tool and is not redistributed.
- Upgrading PlantUML means changing the pinned digest and regenerating all SVG files in the same commit, since output can change between versions.

## Revisit when

- Running Docker to render diagrams becomes a burden for contributors: consider a CI-only rendering with SVG published as build artifacts.
- GitHub or Mermaid gains reliable support for the missing UML diagram types: reconsider Mermaid for new diagrams.
