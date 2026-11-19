#!/usr/bin/env bash
# Clone, build, test and exercise a fresh RESTController checkout.
set -Eeuo pipefail
REPO_URL="${REST_TEST_REPO:-https://github.com/qualitypro/RESTController.git}"
REF="${REST_TEST_REF:-feature/sprint-01-foundation-audit}"
KEEP="${REST_TEST_KEEP:-0}"
ROOT="$(mktemp -d "${TMPDIR:-/tmp}/restcontroller-harness.XXXXXXXX")"
LOG="${REST_TEST_LOG:-$PWD/restcontroller-test-$(date +%Y%m%d-%H%M%S).log}"
PID=""
exec > >(tee -a "$LOG") 2>&1
cleanup() {
  local status=$?
  trap - EXIT
  if [[ -n "$PID" ]]; then kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; fi
  if [[ "$KEEP" != 1 ]]; then rm -rf "$ROOT"; else echo "Checkout retained at: $ROOT"; fi
  echo "Harness exit status: $status"
  echo "Complete report: $LOG"
  if command -v wl-copy >/dev/null 2>&1 && [[ -n "${WAYLAND_DISPLAY:-}" ]]; then
    wl-copy < "$LOG" || true
  elif command -v xclip >/dev/null 2>&1 && [[ -n "${DISPLAY:-}" ]]; then
    xclip -selection clipboard < "$LOG" || true
  else
    echo "Clipboard unavailable; report saved on disk."
  fi
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

for cmd in git java mvn python3 curl; do
  command -v "$cmd" >/dev/null || { echo "Missing required command: $cmd"; exit 2; }
done
java -version
mvn -version
python3 --version
echo "Cloning $REPO_URL at $REF"
git clone --no-tags "$REPO_URL" "$ROOT/repo"
cd "$ROOT/repo"
git checkout "$REF" || { git fetch origin "$REF"; git checkout --detach FETCH_HEAD; }
echo "Checked out commit: $(git rev-parse HEAD)"
mvn --batch-mode --no-transfer-progress clean verify
JAR="$PWD/target/restcontroller-0.1.0-SNAPSHOT-server.jar"
test -s "$JAR"
PORT="${REST_TEST_PORT:-}"
if [[ -z "$PORT" ]]; then
  PORT="$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1]);s.close()')"
fi
BASE="http://127.0.0.1:$PORT"
KEY="$(python3 -c 'import secrets;print(secrets.token_hex(32))')"
export REST_HOST=127.0.0.1 REST_PORT="$PORT" REST_DATA_FILE="$ROOT/people.json" REST_API_KEY="$KEY"
start_server() {
  java -jar "$JAR" > "$ROOT/server.log" 2>&1 &
  PID=$!
  local ready=0
  for attempt in {1..50}; do
    if ! kill -0 "$PID" 2>/dev/null; then
      echo "Server exited unexpectedly"; cat "$ROOT/server.log"; return 1
    fi
    if curl --max-time 1 --silent --fail "$BASE/health" | grep -q '"status":"ok"'; then
      ready=1; break
    fi
    sleep 0.2
  done
  if [[ "$ready" != 1 ]]; then echo "Server did not become ready"; cat "$ROOT/server.log"; return 1; fi
}
stop_server() {
  kill "$PID" 2>/dev/null || true
  wait "$PID" 2>/dev/null || true
  PID=""
}
start_server
python3 tests/harness/api_suite.py --base "$BASE" --token "$KEY" --phase initial
stop_server
echo "Restarting server to verify durable records"
start_server
python3 tests/harness/api_suite.py --base "$BASE" --token "$KEY" --phase restart
stop_server
echo "Testing corrupt storage startup failure"
printf 'not json' > "$REST_DATA_FILE"
if java -jar "$JAR" > "$ROOT/corrupt.log" 2>&1; then
  echo "FAIL: server accepted corrupt storage"; exit 1
fi
echo "PASS: corrupt storage rejected"
echo "ALL HARNESS STAGES PASSED"
