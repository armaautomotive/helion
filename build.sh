#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

rm -rf out
mkdir -p out
javac -d out $(find src/main/java -name '*.java')

echo "Build complete: $ROOT_DIR/out"
