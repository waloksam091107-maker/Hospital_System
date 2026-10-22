#!/usr/bin/env bash
set -eu

host="${OCEANBASE_HOST:-oceanbase}"
port="${OCEANBASE_PORT:-2881}"
timeout="${OCEANBASE_WAIT_SECONDS:-360}"
deadline=$(( $(date +%s) + timeout ))

echo "Waiting for OceanBase TCP endpoint ${host}:${port}..."
while ! (echo >"/dev/tcp/${host}/${port}") >/dev/null 2>&1; do
  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "OceanBase endpoint did not become reachable within ${timeout} seconds." >&2
    exit 1
  fi
  sleep 2
done

exec "$@"
