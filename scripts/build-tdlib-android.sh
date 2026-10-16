#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TDLIB_COMMIT="a94b83d67ab4c8397c6245e541b31a4131ea2f79"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

if ! command -v docker >/dev/null 2>&1 || ! docker buildx version >/dev/null 2>&1; then
  echo "Docker Buildx is required to build the pinned TDLib runtime." >&2
  exit 1
fi
if ! command -v unzip >/dev/null 2>&1; then
  echo "unzip is required to unpack the TDLib Android archive." >&2
  exit 1
fi

TDLIB_CONTEXT="https://github.com/tdlib/td.git#"
TDLIB_CONTEXT+="$TDLIB_COMMIT"
TDLIB_CONTEXT+=":example/android"

echo "Building TDLib commit $TDLIB_COMMIT for Android (JSONJava interface)."
docker buildx build \
  --platform linux/amd64 \
  --build-arg "COMMIT_HASH=$TDLIB_COMMIT" \
  --build-arg "TDLIB_INTERFACE=JSONJava" \
  --output "type=local,dest=$BUILD_DIR/output" \
  "$TDLIB_CONTEXT"

unzip -q "$BUILD_DIR/output/tdlib.zip" -d "$BUILD_DIR/unpacked"
for ABI in arm64-v8a armeabi-v7a x86_64 x86; do
  SOURCE_LIB="$BUILD_DIR/unpacked/tdlib/libs/$ABI/libtdjsonjava.so"
  if [ ! -s "$SOURCE_LIB" ]; then
    echo "TDLib build did not produce $SOURCE_LIB" >&2
    exit 1
  fi
  DEST_DIR="$ROOT_DIR/app/src/main/jniLibs/$ABI"
  mkdir -p "$DEST_DIR"
  cp "$SOURCE_LIB" "$DEST_DIR/libtdjsonjava.so"
done

echo "TDLib native libraries are ready under app/src/main/jniLibs."
