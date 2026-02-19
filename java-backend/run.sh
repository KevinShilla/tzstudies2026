#!/usr/bin/env bash
set -euo pipefail

mkdir -p out

echo "Compiling Java files..."
# Only compile the plain-Java server (Java 8 compatible, no Maven needed)
find ./src/com -name "*.java" > sources.txt

if [ ! -s sources.txt ]; then
  echo "No Java source files found under ./src/com" >&2
  exit 1
fi

javac -d ./out @sources.txt

echo "Starting server on http://localhost:8080 ..."
java -cp ./out com.tzstudies.api.Main
