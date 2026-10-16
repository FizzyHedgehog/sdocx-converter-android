# SDOCX Converter for Android

High-performance, lossless vector PDF converter for Samsung Notes (`.sdocx`) documents on Android and Samsung Galaxy Tab devices.

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Target-Android%2015%20(API%2035)-blue" alt="Target Android 15" />
  <img src="https://img.shields.io/badge/Arch-ARM64--v8a-orange" alt="ARM64" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Core-Rust%20Native%20(NDK)-red" alt="Rust NDK" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License" />
</p>

---

## Why SDOCX Converter? (True Vector PDFs vs. Rasterized Bitmaps)

When exporting handwritten notes directly to PDF within the Samsung Notes application, handwriting strokes and highlights are typically rasterized into low-resolution bitmap images. This results in blurry, pixelated text and lines whenever zooming in, viewing on high-resolution displays, or printing.

**SDOCX Converter** solves this issue at the foundation:
By directly parsing the native `.sdocx` container, all S Pen handwriting, marker strokes, and shapes are reconstructed into mathematical vector paths (PDF vector curves). Notes remain razor-sharp at any zoom level, print size, or screen resolution.

---

## Key Features

- **Lossless Vector Graphic Rendering**: Converts handwriting, highlights, formulas, and geometric shapes into smooth vector curves without pixelation.
- **Optimized for Galaxy Tab and Large Screens**:
  - Two-pane adaptive layout for tablets and Samsung DeX (queue on the left, completed PDFs on the right).
  - Split screen and pop-up view support with responsive layouts.
- **Batch and Recursive Folder Conversion**:
  - Multi-select multiple `.sdocx` files for simultaneous conversion.
  - Select an entire folder to automatically scan and import all Samsung Notes within it.
- **Samsung Notes and System Integration**:
  - Share directly from Samsung Notes with three configurable reception modes: **Default (Open App)**, **Background Silent Auto-Save**, and **Instant Re-Share PDF**.
  - Drag and drop support: Drag `.sdocx` files directly from the My Files app in split-screen mode.
- **One-Tap PDF Viewing and Sharing**:
  - Tap any converted card to open the vector PDF directly in your default PDF reader.
  - Quick share to external apps or batch export multiple files at once.
- **Scoped Storage and Flexible Saving**:
  - Auto-save mode: Automatically export converted PDFs to your Downloads folder or a designated custom folder.
  - Manual export mode: Keep files in isolated app storage until you choose to export them.
- **Custom Stroke Thickness**:
  - Adjust pen and marker stroke multipliers in settings to suit your personal handwriting weight.
- **Intelligent Page Splitting**:
  - Option to split wide continuous canvas notes vertically or horizontally.
- **Background Conversion Notifications**:
  - System notifications alert you when large notes or batch conversions finish in the background.
- **100% Offline Core Conversion**:
  - Powered by an embedded Rust native engine (`libsdocx2pdf.so` via Git submodule). All document conversions run locally on your device without uploading files or external telemetry.

---

## Sharing Directly from Samsung Notes (Workflow & Modes)

SDOCX Converter integrates directly into the Android system Share sheet, allowing you to convert single or multiple notes without manually exporting and importing files.

### Prerequisites & How to Share

1. **Configure Your Preferred Share Action (Optional)**:
   - Open **SDOCX Converter > Settings (gear icon) > Action on Received Share** and select how the app should handle incoming shared files.
2. **Select Notes in Samsung Notes**:
   - Long-press one or more notes in your Samsung Notes list (or open a note and tap the **More (three dots)** menu) and tap **Share**.
3. **Required Format Condition (`Samsung Notes file (.sdocx)`)**:
   - When Samsung Notes asks which format to share, you **must select `Samsung Notes file (.sdocx)`**.
   - *Important*: Do **not** select `PDF file` or `Image file` in Samsung Notes—those formats are already rasterized into low-resolution bitmaps by Samsung Notes before leaving the app. Selecting `Samsung Notes file (.sdocx)` passes the original vector note container to SDOCX Converter so it can reconstruct true lossless vector paths.
4. **Choose SDOCX Converter**:
   - In the Android system share sheet that appears, tap **SDOCX Converter**.

### Available Share Reception Modes (Including 2 New Background Modes)

In **Settings > Action on Received Share**, you can choose between the standard interactive mode and **two newly added zero-interruption background modes**:

1. **Default (Open App)** *(Standard Mode)*
   - Launches the SDOCX Converter main interface, imports the shared `.sdocx` file(s) into the queue, and converts them interactively inside the app where you can preview, manage, or manually export them.
2. **Background Silent Auto-Save** *(New)*
   - **How it works**: Converts the shared `.sdocx` file(s) in the background and saves the resulting vector PDF(s) directly to your configured save destination (**Downloads** folder by default, or any custom folder set under **Settings > Storage & Saving**) **without opening the SDOCX Converter window**.
   - **What it enables**: You remain inside Samsung Notes with zero screen transitions or interruptions to your note-taking session. A compact 1.0-second floating pill banner briefly appears at the bottom of your screen (`Converting SDOCX to PDF...` -> `Saved: '...'`) to confirm that your vector PDF has been saved in the background.
3. **Instant Re-Share PDF** *(New)*
   - **How it works**: Converts the shared `.sdocx` file(s) to lossless vector PDF(s) silently in the background (while also saving a copy to your default save folder) and **immediately opens the Android system Share sheet with the newly generated `.pdf` file(s) attached**.
   - **What it enables**: Acts as a direct **Vector PDF Bridge** from Samsung Notes to any external app—such as KakaoTalk, Slack, Gmail, Google Drive, Notion, Goodnotes, or Telegram. Instead of sharing a blurry rasterized PDF from Samsung Notes, or manually converting a file and searching for it in a file manager, you can send a crystal-clear vector PDF from Samsung Notes to any messenger, email client, or cloud drive in one continuous flow.

---

## Installation

1. Download the latest **`SDOCX_Converter.apk`** from the [Releases](https://github.com/FizzyHedgehog/sdocx-converter-android/releases) page.
2. Open the downloaded APK on your device to install.
   - **Install Unknown Apps**: Because this open-source application is distributed outside Google Play, Android will prompt for permission to install unknown apps. Enable permission for your browser or file manager to proceed.
   - **Google Play Protect Notice**: If Google Play Protect displays a warning stating *"Unrecognized developer"* or *"Blocked by Play Protect"*, tap **More details** and select **Install anyway**. This is standard Android security verification for community-signed open-source applications.
3. Launch the app and import your `.sdocx` files to begin converting.

---

## Updating

SDOCX Converter supports seamless in-app self-updating without losing your conversion history or settings:

- **Automatic Check on Launch**: By default, the app quietly checks GitHub Releases for new updates when started. When a new version is detected, an update dialog displays the release notes and provides a one-tap download and installation button.
- **Manual Update Check**: You can check for updates at any time by navigating to **Settings (gear icon) > Software Update > Check for Updates**.
- **Configure Auto-Updates**: You can enable or disable automatic update checks at launch through the toggle in **Settings > Software Update**.
- **Direct Sideload Update**: You can also download the latest `SDOCX_Converter.apk` from GitHub Releases and install it directly over the existing app. Because the release keystore remains consistent across all releases, Android will update the existing package cleanly without conflict.

---

## Hardware Compatibility & Feedback Wanted

This project is maintained as an open-source utility. Because development and testing were conducted on a limited set of hardware (a Galaxy Tab model), comprehensive testing across the entire spectrum of Samsung Galaxy devices—including various Galaxy phones (S-series, Z Fold, Z Flip, A-series), different tablet screen aspect ratios, and varying One UI / Android versions—has not been exhaustive.

Your feedback and testing on other devices are invaluable:
- **Device Compatibility**: If you notice unexpected behavior, layout clipping in split-screen/DeX, or file access issues on your specific device, please report it.
- **Feature Requests**: If you have suggestions for improving handwriting rendering, user experience, or Samsung Notes integration, feedback is always welcome.

Please feel free to open a thread on [GitHub Issues](https://github.com/FizzyHedgehog/sdocx-converter-android/issues) or join the discussion in [Issue #1](https://github.com/FizzyHedgehog/sdocx-converter-android/issues/1).

---

## Technical Architecture

```
[ UI Layer (Kotlin + Jetpack Compose) ]
   ├── MainScreen (Two-Pane Tablet & Single-Column Phone Adaptive Layout)
   ├── ShareReceiverActivity (Background Auto-Save, Floating Pill & Instant Re-Share)
   ├── UpdateManager (GitHub Releases In-App Self-Updater)
   └── SettingsDialog (SharedPreferences & SAF Persistence)
                   │ (JNI / NDK Bridge: rust-core/src/lib.rs)
                   ▼
[ Native Core (Rust Git Submodule: rust-core/sdocx2pdf) ]
   ├── sdocx Parser (ZIP Container & Note Page Extraction)
   ├── Stroke & Shape Reconstructor (Path & Bezier Interpolation)
   └── krilla Vector Engine (PDF 1.7 Lossless Vector Generation)
```

- **UI Framework**: Android Jetpack Compose, Material 3, AndroidX
- **Concurrency**: Kotlin Coroutines, StateFlow, ViewModel
- **Storage**: Android Storage Access Framework (SAF), Scoped Storage, AndroidX FileProvider
- **Native Engine**: Rust 2021, Cargo-NDK, `libsdocx2pdf.so` (ARM64-v8a) built from the `sdocx2pdf` Git submodule
- **Minimum SDK**: Android 8.0 (API 26) / **Target SDK**: Android 15 (API 35)

---

## Credits & License

- **License**: [MIT License](LICENSE.txt)
- **Copyright**: Copyright © 2026 FizzyHedgehog. All rights reserved.
- **Core Engine Credit**: Based on the core conversion engine of [squ1dd13/sdocx2pdf](https://github.com/squ1dd13/sdocx2pdf) under the MIT License.
