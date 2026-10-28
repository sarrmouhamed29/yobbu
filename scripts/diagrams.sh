#!/usr/bin/env bash
# Render every PlantUML source under docs/ to an SVG next to it.
#
# Usage:
#   scripts/diagrams.sh           render all diagrams
#   scripts/diagrams.sh --check   render, then fail if any SVG changed or was
#                                 missing before rendering (stale or missing SVG)
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

# One line per source: the SVG path and a checksum, or "missing".
snapshot() {
  find docs -name '*.puml' | sort | while IFS= read -r source; do
    svg="${source%.puml}.svg"
    if [ -f "$svg" ]; then
      echo "$svg $(cksum < "$svg")"
    else
      echo "$svg missing"
    fi
  done
}

before="$(snapshot)"

find docs -name '*.puml' -print0 \
  | xargs -0 docker run --rm -v "$ROOT:/work" -w /work "$IMAGE" -tsvg

if [ "${1:-}" = "--check" ]; then
  after="$(snapshot)"
  if [ "$before" != "$after" ]; then
    echo "Diagrams are out of date. SVG files were regenerated; commit them." >&2
    diff <(echo "$before") <(echo "$after") >&2 || true
    exit 1
  fi
  echo "Diagrams are up to date."
fi
