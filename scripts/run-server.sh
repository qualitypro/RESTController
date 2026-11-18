#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

: "${REST_HOST:=127.0.0.1}"
: "${REST_PORT:=8080}"
: "${REST_DATA_FILE:=$ROOT/data/people.json}"

if [[ "$REST_HOST" != "127.0.0.1" && "$REST_HOST" != "::1" && "$REST_HOST" != "localhost" && -z "${REST_API_KEY:-}" ]]; then
  echo "Refusing non-loopback binding without REST_API_KEY" >&2
  exit 1
fi

if [[ -n "${REST_API_KEY:-}" && "${#REST_API_KEY}" -lt 24 ]]; then
  echo "REST_API_KEY must contain at least 24 characters" >&2
  exit 1
fi

JAR="$ROOT/target/restcontroller-0.1.0-SNAPSHOT-server.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Missing $JAR. Run mvn --batch-mode verify first." >&2
  exit 1
fi

export REST_HOST REST_PORT REST_DATA_FILE
exec java ${JAVA_OPTS:-} -jar "$JAR"
