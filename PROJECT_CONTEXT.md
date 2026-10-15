# SDOCX Converter (Android) - Project Master Context

> **Welcome to the Android SDOCX Converter Development Workspace!**  
> This document contains the complete context, history, rules, and architecture of the Android application. Any AI agent operating in this workspace should read this document first to resume work seamlessly.

---

## 1. Project Overview & Repository

- **Application Name**: SDOCX Converter
- **Target OS**: Android 8.0+ (API 26 to 35, optimized for Samsung Galaxy Tablets & DeX)
- **Architecture**: ARM64-v8a (`aarch64-linux-android`)
- **Local Workspace**: `/Users/kyungmolee01/Projects/sdocx-converter-android`
- **GitHub Repository**: [FizzyHedgehog/sdocx-converter-android](https://github.com/FizzyHedgehog/sdocx-converter-android)
- **Active Branches**:
  - `main`: Official release branch (Release `v1.0.0`, Asset: `SDOCX_Converter.apk`, 3.70 MB)
  - `feature/background-share`: Current feature branch for background share from Samsung Notes
- **Git Author**: `FizzyHedgehog <fizzyhedgehog@users.noreply.github.com>`
- **GitHub Personal Access Token**: Stored securely in `~/.config/github_token.txt` (Never commit tokens to git!)

---

## 2. Strict User Rules & Guidelines

1. **NO Local MacBook Downloads**:
   - **NEVER** save or download generated `.apk` files to the MacBook's `~/Desktop` or personal user storage.
   - All automated build scripts (`monitor_and_download_apk.py`) extract APKs strictly to `/tmp/sdocx_apk_build/`, send them to Telegram, and immediately delete all local files from the MacBook.
2. **Hidden Feature Branch & Release Isolation**:
   - When developing or testing new features, work strictly on isolated feature branches (e.g. `feature/background-share`).
   - Do NOT overwrite public Release v1.0.0 or `main` until the user explicitly tests and approves ("정식 배포해" / "올려줘").
3. **English Code & Commits**:
   - All code comments, Git commit messages, and documentation must be written in clear English without emojis.
4. **Preserve User Privacy**:
   - Keep GitHub profile anonymous (`FizzyHedgehog`).

---

## 3. Architecture & Key Components

- **Tech Stack**: Kotlin, Jetpack Compose (Material3), Coroutines, AndroidX DocumentFile (SAF), Rust JNI
- **Key Modules**:
  - `ConversionHelper.kt`: Pure modular engine for SDOCX -> PDF conversion via Rust JNI, SAF tree file saving, MediaStore Downloads fallback, and notifications.
  - `ShareHandlerActivity.kt`: Pure transparent activity (`Theme.Transparent`) intercepting `ACTION_SEND` and `ACTION_SEND_MULTIPLE`.
    - Handles 3 modes based on user settings:
      1. `OPEN_APP`: Forwards intent to `MainActivity` with no UI delay.
      2. `AUTO_SAVE`: Converts silently in background, saves to destination, displays custom One UI style pill toast for **0.5s** (converting) + **0.5s** (complete) at bottom center.
      3. `RE_SHARE`: Converts in background and immediately launches Android system `ShareSheet` (`Intent.createChooser`) for instant forwarding to KakaoTalk, Quick Share, etc.
  - `MainActivity.kt`: Main application screen and document queue management.
  - `SettingsDialog.kt`: User settings dialog with pen/marker sliders, auto-save destination, notification toggle, and the 3-way `ShareReceiveAction` radio group.
  - `rust-core/`: Pure Rust vector rendering engine compiled via `cargo-ndk` to `libsdocx2pdf.so`.

---

## 4. CI/CD & Build Workflow

- `.github/workflows/build-apk.yml`:
  - Triggers on push to `main` and `feature/**`.
  - Sets up Java 17, Rust toolchain for `aarch64-linux-android`, `cargo-ndk`, Android NDK r26d, and Gradle 8.11.1.
  - Produces release APK artifact `SDOCX_Converter_APK`.
- `scripts/monitor_and_download_apk.py`:
  - Monitors GitHub Actions runs, downloads artifact zip to `/tmp/`, extracts APK, delivers to Telegram, and cleans up local storage.

---

## 5. Current Status & Next Steps

- **Branch `feature/background-share`**:
  - Implemented background silent auto-save and instant re-share modes.
  - Configured 0.5s exact toast display duration at bottom center.
  - Tested and delivered to user via Telegram.
- **Pending Action**:
  - Awaiting user's final confirmation to merge `feature/background-share` into `main` and tag Release `v1.1.0`.

---

## 6. Telegram Integration & Listener Rules

- Telegram config: `~/.config/sdocx_telegram_config.json`
- Notification utility: `python3 /Users/kyungmolee01/.sdocx_telegram_agent/notify_telegram.py`
- **Telegram Incoming Command Listener**:
  - Script: `python3 /Users/kyungmolee01/.sdocx_telegram_agent/wait_for_telegram_android.py`
  - Group: `sdocx-converter-android` (Chat ID: `-5102225522`)
  - **OPERATIONAL RULE**:
    Whenever the user says **"텔레그램 대기"**, **"텔레그램 기다려"**, **"대기해"**, or asks to wait for Telegram messages:
    You **MUST execute the listener as a background task**:
    `run_command(CommandLine="python3 /Users/kyungmolee01/.sdocx_telegram_agent/wait_for_telegram_android.py", Cwd="/Users/kyungmolee01/Projects/sdocx-converter-android", WaitMsBeforeAsync=1000)`
    Do NOT simply reply "대기하겠습니다" with plain text. Always launch the background listener so you wake up reactively when a command arrives from the Android Telegram group!

