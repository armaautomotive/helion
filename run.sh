#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

mkdir -p out
javac -d out $(find src/main/java -name '*.java')

if [ "$#" -eq 0 ]; then
  echo "Starting Helion session for agent 'prospecting' in analyze mode..."
  exec java -cp out helion.Helion --session --agent prospecting analyze
fi

echo "Starting Helion with args: $*"
exec java -cp out helion.Helion "$@"
