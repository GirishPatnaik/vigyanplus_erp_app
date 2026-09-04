#!/bin/sh
# Gradle start up script - standard wrapper launcher.
# When first opened in Android Studio, the IDE will auto-download the
# matching gradle-wrapper.jar for this project (declared in
# gradle/wrapper/gradle-wrapper.properties). If running from a terminal
# with Gradle already installed, you can also just run: gradle wrapper
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "${GRADLE_HOME:-gradle}" -p "$DIR" "$@" 2>/dev/null || {
  echo "This wrapper needs gradle-wrapper.jar. Open the project in Android Studio"
  echo "(it will download it automatically), or run: gradle wrapper"
  exit 1
}
