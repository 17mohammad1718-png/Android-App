#!/usr/bin/env sh
set -eu

./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace
