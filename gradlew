#!/bin/sh
# Small Gradle launcher for this project. It uses local Gradle when available;
# otherwise downloads the official Gradle 8.7 distribution into ~/.gradle.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=8.7
if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$APP_HOME" "$@"
fi
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"${HOME:-/tmp}/.gradle"}
DIST_ROOT="$GRADLE_USER_HOME/neon-dodge-gradle"
GRADLE_HOME="$DIST_ROOT/gradle-$VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$DIST_ROOT"
  ZIP="$DIST_ROOT/gradle-$VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  echo "Gradle $VERSION မတွေ့ပါ။ Official distribution ကို download လုပ်နေပါတယ်..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 2 "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "curl သို့မဟုတ် wget မရှိပါ။ Gradle $VERSION ကို install လုပ်ပြီး ပြန် run ပါ။" >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP" -d "$DIST_ROOT"
  else
    echo "unzip မရှိပါ။ Gradle $VERSION ZIP ကိုဖြည်ပြီး ပြန် run ပါ။" >&2
    exit 1
  fi
fi
exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
