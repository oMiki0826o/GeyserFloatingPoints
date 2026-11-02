#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -x ./gradlew && -f gradle/wrapper/gradle-wrapper.jar ]]; then
  ./gradlew clean shadowJar --no-daemon "$@"
elif command -v gradle >/dev/null 2>&1; then
  gradle clean shadowJar --no-daemon "$@"
else
  echo 'Please install Gradle 9.x, then run: gradle clean shadowJar' >&2
  echo 'No Gradle wrapper binaries are bundled in this source-only archive.' >&2
  exit 1
fi
