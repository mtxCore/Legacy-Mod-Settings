#!/bin/sh
set -e
APP_HOME=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "ERROR: gradle-wrapper.jar not found at $WRAPPER_JAR"
    echo "This shouldn't happen; the jar is included in the project zip."
    exit 1
fi

exec java -cp "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
