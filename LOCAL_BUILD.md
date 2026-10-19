# Local macOS APK Build (WorkBuddy)

> 2026-10-20, WorkBuddy AI 세션에서 세팅. GitHub Actions CI와 병존하는 **로컬 직접 빌드** 체계.
> 이 문서는 사람과 AI 에이전트(antigravity 등) 모두를 위한 기록입니다.

## 1. 배경 (왜 이것이 존재하는가)

- 기존: APK 빌드는 GitHub Actions(`.github/workflows/build-apk.yml`)로만 수행. 산출물은 Telegram으로 전달되고 맥북 로컬에는 저장되지 않았다.
- 변경: WorkBuddy와의 로컬 개발 세션에서 수정 → **맥북에서 즉시 APK 빌드·검증**이 필요해져 로컬 빌드 체계를 세팅했다.
- 원본 소스 코드/기능 로직은 그대로이며, 추가된 것은 아래 파일뿐이다.

## 2. 추가된 파일 (소스 로직 변경 없음)

| 파일 | 역할 |
|---|---|
| `build-apk.sh` | 로컬 APK 빌드 원커맨드 (Rust JNI + Gradle assembleRelease + Downloads 최신화) |
| `sync-downloads.sh` | `~/Downloads/SDOCX_Converter.apk` 최신화 (단일 파일 교체, 기존 파일은 휴지통 이동) |
| `.tools/` | 로컬 빌드 툴체인 전체 (아래 3절). **gitignore 대상**. |
| `local.properties` | `sdk.dir` → `.tools/android-sdk` (gitignore 대상) |
| `.gitignore` | `.tools/`, `app/src/main/jniLibs/`, `dist-local/` 항목 추가 |
| `LOCAL_BUILD.md` | 이 문서 |
| `PROJECT_CONTEXT.md` | Local Build 섹션 추가 (에이전트가 첫 페이지로 읽는 문서) |

## 3. 로컬 툴체인 (`.tools/`, gitignore)

- `jdk17/` — Temurin JDK 17.0.20.1 (macOS arm64). AGP 8.7.3/Gradle 요구사항.
- `gradle-8.11.1/` — Gradle 8.11.1. CI 워크플로의 `gradle wrapper --gradle-version 8.11.1`과 동일 버전.
- `android-sdk/` — cmdline-tools(sdkmanager 12.0), `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`, `ndk;26.3.11579264` (NDK r26d, Apple Silicon 네이티브).
- `cargo-ndk/` — cargo-ndk 4.1.2 (`--root` 방식으로 이 경로에 설치).
- `gradle-home/` — Gradle 캐시/의존성 저장소(`GRADLE_USER_HOME`). 폴더 내 격리.

시스템에는 **아무것도 전역 설치하지 않았다** (node/java/gradle 전역 미설치 상태 유지).

## 4. 사용법

```bash
cd ~/Projects/sdocx-converter/sdocx-converter-android
./build-apk.sh              # Rust 엔진 변경 시 (Rust 증분 ~1분 + Gradle ~10초~1분)
./build-apk.sh --skip-rust  # Kotlin/Compose/리소스만 변경 시 (Gradle 증분만)
```

- APK 산출: `dist-local/SDOCX_Converter.apk` (GitHub Actions 산출물과 동일한 `app/keystore.jks`로 서명)
- 빌드가 끝나면 자동으로 `~/Downloads/SDOCX_Converter.apk` 최신화 (기존 파일은 휴지통으로 이동 후 교체, 동일 내용이면 스킵)
- 과거 APK 아카이브: `~/Projects/sdocx-android-local-build/dist/`

## 5. 주의사항

1. **`git clean -xfd` 금지**: `.tools/`는 gitignore 대상이라 이 명령 한 방에 5.3GB 툴체인이 삭제된다. 작업 전 항상 확인.
2. **`.tools/`는 삭제 금지**: 로컬 빌드의 전부다. 지우면 `build-apk.sh`가 오류와 함께 중단된다(안전장치 있음).
3. 원격 push 시 `.tools/`, `jniLibs/`, `dist-local/`, `local.properties`는 gitignore라 올라가지 않는다. GitHub Actions 빌드에는 영향 없음.
4. 로컬 빌드와 CI 빌드는 동일 keystore 서명 → 인앱 업데이트 상호 호환.
5. 기존 정책(맥북 APK 미保存 / Telegram 전달)은 이 로컬 체계로 대체/병된다. 사용자가 Telegram 전달을 원하면 별도 요청.

## 6. 검증 이력

- 2026-10-20: 본 레포에서 `./build-apk.sh` 최초 검증 성공 (상세 결과는 `~/Projects/sdocx-android-local-build/WORKLOG.md` 및 git 로그 참고)
- 서명 검증: `apksigner verify --print-certs` — Signer #1 certificate DN: CN=FizzyHedgehog, OU=SDOCX, O=FizzyHedgehog, C=KR
- 버전: versionName 1.3.0 / versionCode 4, arm64-v8a
