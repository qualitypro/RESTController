#!/usr/bin/env bash
# Read-only source/build inventory for RESTController.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
OUT="${1:-$ROOT/restcontroller-audit.log}"
case "$OUT" in /*) ;; *) OUT="$ROOT/$OUT";; esac
mkdir -p "$(dirname "$OUT")"
exec > >(tee "$OUT") 2>&1
echo "=== RESTController Sprint 01 audit ==="
echo "UTC: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "Directory: $ROOT"
echo
echo "=== Git status ==="
git status --short || true
git rev-parse --short HEAD || true
echo
echo "=== Toolchain ==="
for tool in java javac mvn gradle; do
  if command -v "$tool" >/dev/null 2>&1; then
    echo "$tool: $(command -v "$tool")"
    "$tool" -version 2>&1 | head -n 3 || true
  else
    echo "$tool: not installed"
  fi
done
echo
echo "=== Tracked files ==="
git ls-files | sort
echo
echo "=== Java sources ==="
git ls-files '*.java' | while IFS= read -r f; do
  echo "--- $f"
  grep -nE '^[[:space:]]*(package |import |public |protected |class |interface |abstract class |enum |record |.*static void main\()' "$f" | head -n 70 || true
done
echo
echo "=== Build descriptors ==="
for f in pom.xml build.gradle build.gradle.kts settings.gradle gradlew Makefile; do
  test ! -f "$f" || echo "PRESENT: $f"
done
echo
echo "=== Potential test files ==="
git ls-files | grep -Ei '(^|/)(test|tests)/|Test\.java$|Tests\.java$' || true
echo
echo "=== Entry points / HTTP handling ==="
git grep -nE 'static void main\(|HttpServer|ServerSocket|HttpServlet|@Path\(|@GET|@POST|@PUT|@DELETE' -- '*.java' || true
echo
echo "=== Audit finished ==="
echo "Report: $OUT"
if command -v wl-copy >/dev/null 2>&1; then
  wl-copy < "$OUT" && echo "Copied report to Wayland clipboard"
elif command -v xclip >/dev/null 2>&1; then
  xclip -selection clipboard -i "$OUT" && echo "Copied report to X11 clipboard"
else
  echo "Clipboard tool unavailable; report remains at $OUT"
fi
