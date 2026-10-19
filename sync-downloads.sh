#!/bin/bash
# ~/Downloads/SDOCX_Converter.apk 를 최신 빌드로 최신화한다.
# - 건드리는 파일은 ~/Downloads/SDOCX_Converter.apk 딱 하나뿐이다.
# - 기존 파일이 있고 내용이 다르면 덮어쓰지 않고 macOS 휴지통으로 이동(복구 가능)한 뒤 배치한다.
# - 동일 내용(sha256)이면 아무 작업도 하지 않는다.
set -e
BASE="$(cd "$(dirname "$0")" && pwd)"
SRC="$BASE/dist-local/SDOCX_Converter.apk"
DEST="$HOME/Downloads/SDOCX_Converter.apk"

if [ ! -f "$SRC" ]; then
  SRC="$BASE/app/build/outputs/apk/release/app-release.apk"
fi
if [ ! -f "$SRC" ]; then
  echo "[오류] 배치할 새 APK가 없습니다: $SRC"
  exit 1
fi

if [ -f "$DEST" ]; then
  NEW_SUM="$(shasum -a 256 "$SRC" | awk '{print $1}')"
  OLD_SUM="$(shasum -a 256 "$DEST" | awk '{print $1}')"
  if [ "$NEW_SUM" = "$OLD_SUM" ]; then
    echo "이미 최신 APK가 Downloads에 있습니다: $DEST"
    exit 0
  fi
  echo "기존 APK를 휴지통으로 이동합니다: $DEST"
  TS="$(date +%Y%m%d_%H%M%S)"
  mv "$DEST" "$HOME/.Trash/SDOCX_Converter_${TS}.apk"
fi

cp "$SRC" "$DEST"
echo "Downloads에 최신 APK를 배치했습니다: $DEST"
ls -la "$DEST"
