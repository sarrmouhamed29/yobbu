#!/usr/bin/env bash
# Render every PlantUML source under docs/ to an SVG next to it.
#
# Usage:
#   scripts/diagrams.sh           render all diagrams
#   scripts/diagrams.sh --check   render, then fail if any SVG differs from
#                                 what is committed (stale or missing SVG)
#
# Requires Docker. On Windows, run it from Git Bash.
set -euo pipefail

# Pinned by digest so output stays reproducible (PlantUML 1.2026.9).
IMAGE="plantuml/plantuml@sha256:9b5071ab56a4192aac458ab4b0d42261c22c8109695894ae5be6013c3be6cac1"

cd "$(dirname "$0")/.."
ROOT="$(pwd -W 2>/dev/null || pwd)"
export MSYS_NO_PATHCONV=1

if ! find docs -name '*.puml' -print -quit | grep -q .; then
  echo "No .puml sources found under docs/."
  exit 0
fi

find docs -name '*.puml' -print0 \
  | xargs -0 docker run --rm -v "$ROOT:/work" -w /work "$IMAGE" -tsvg

if [ "${1:-}" = "--check" ]; then
  if [ -n "$(git status --porcelain -- 'docs/**/*.svg' 'docs/*.svg')" ]; then
    echo "Diagrams are out of date. Run scripts/diagrams.sh and commit the SVG files." >&2
    git status --short -- 'docs/**/*.svg' 'docs/*.svg' >&2
    exit 1
  fi
  echo "Diagrams are up to date."
fi
