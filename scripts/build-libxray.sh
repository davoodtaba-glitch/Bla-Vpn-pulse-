#!/usr/bin/env bash
# Build AndroidLibXrayLite AAR and copy into app/libs/
# Requirements: Go 1.22+, Android NDK/SDK, gomobile
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKDIR="${TMPDIR:-/tmp}/AndroidLibXrayLite"
AAR_DEST="$ROOT/app/libs"

echo "==> Cloning / updating AndroidLibXrayLite"
if [ -d "$WORKDIR/.git" ]; then
  git -C "$WORKDIR" pull --ff-only
else
  git clone --depth 1 https://github.com/2dust/AndroidLibXrayLite.git "$WORKDIR"
fi

cd "$WORKDIR"
echo "==> Preparing gomobile"
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest
export PATH="$(go env GOPATH)/bin:$PATH"
gomobile init
go mod tidy -v

echo "==> Building AAR (API 24+)"
gomobile bind -v -androidapi 24 -trimpath \
  -ldflags='-s -w -buildid= -checklinkname=0' ./

mkdir -p "$AAR_DEST"
if [ -f libv2ray.aar ]; then
  cp -f libv2ray.aar "$AAR_DEST/"
  echo "Copied libv2ray.aar -> $AAR_DEST"
elif [ -f libXray.aar ]; then
  cp -f libXray.aar "$AAR_DEST/"
  echo "Copied libXray.aar -> $AAR_DEST"
else
  echo "ERROR: No AAR produced. Check gomobile output."
  ls -la
  exit 1
fi

echo "==> Done. Rebuild the Android app in Android Studio."
