#!/data/data/com.termux/files/usr/bin/bash
# Run pure-JVM tests for the Android app's core logic.
set -euo pipefail
cd "$(dirname "$0")"

OUT="build/test"
rm -rf "$OUT"
mkdir -p "$OUT"

javac -d "$OUT" \
  src/com/kaam/app/Task.java \
  src/com/kaam/app/TaskMath.java \
  tests/TaskMathTest.java

java -cp "$OUT" TaskMathTest
