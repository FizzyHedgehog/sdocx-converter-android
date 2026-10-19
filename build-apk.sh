#!/bin/bash
# SDOCX Converter Android - 로컬 macOS APK 빌드 (WorkBuddy 세션용)
#
# 사용법:
#   ./build-apk.sh              전체 빌드 (Rust 엔진 + APK + Downloads 최신화)
#   ./build-apk.sh --skip-rust  빠른 빌드 (Rust 엔진 안 건드렸을 때, 증분 10초~1분)
#
# 참고:
#   - GitHub Actions CI와 독립적으로 동작한다. 결과물은 CI와 동일한 keystore.jks로 서명된다.
#   - 로컬 빌드 툴체인은 .tools/ 에 있다 (gitignore 대상, 저장소에 올라가지 않음).
#   - 상세 배경/사용법/주의사항은 LOCAL_BUILD.md 참고.
set -e
BASE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$BASE/.tools"

export JAVA_HOME="$TOOLS/jdk17/Contents/Home"
export ANDROID_HOME="$TOOLS/android-sdk"
export ANDROID_NDK_ROOT="$TOOLS/android-sdk/ndk/26.3.11579264"
export GRADLE_USER_HOME="$TOOLS/gradle-home"
export PATH="$TOOLS/cargo-ndk/bin:$PATH"

if [ ! -d "$TOOLS/android-sdk/ndk" ]; then
  echo "[오류] 로컬 빌드 툴체인이 없습니다: $TOOLS"
  echo "LOCAL_BUILD.md 의 셋업 절차를 참고하세요."
  exit 1
fi

if [ "$1" != "--skip-rust" ]; then
  echo "==> [1/3] Rust 네이티브 라이브러리 빌드 (arm64-v8a, API 26)"
  (cd "$BASE/rust-core" && cargo ndk -t arm64-v8a -P 26 -o ../app/src/main/jniLibs build --release)
else
  echo "==> [1/3] Rust 빌드 생략 (--skip-rust)"
fi

echo "==> [2/3] Gradle assembleRelease"
(cd "$BASE" && "$TOOLS/gradle-8.11.1/bin/gradle" assembleRelease --no-daemon)

mkdir -p "$BASE/dist-local"
cp "$BASE/app/build/outputs/apk/release/app-release.apk" "$BASE/dist-local/SDOCX_Converter.apk"
echo "==> APK 산출: $BASE/dist-local/SDOCX_Converter.apk"

echo "==> [3/3] Downloads 폴더 최신화"
"$BASE/sync-downloads.sh"
