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
  - Share directly from Samsung Notes: Long-press a note in Samsung Notes and choose `Share > SDOCX Converter` to start conversion immediately.
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
- **100% Offline and Private**:
  - Powered by an embedded Rust native engine (`libsdocx2pdf.so`). All conversions run locally on your device without network requests or external telemetry.

---

## Installation

1. Download the latest **`SDOCX_Converter.apk`** from the [Releases](https://github.com/FizzyHedgehog/sdocx-converter-android/releases) page.
2. Open the downloaded APK on your device to install.
   - *Note*: As this is an open-source application distributed outside the Google Play Store, Android will display an "Install unknown apps" prompt. Enable permission for your browser or file manager to proceed.
3. Launch the app and import your `.sdocx` files to begin converting.

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
   ├── DropZone (Drag & Drop, SAF File / Tree Folder Intent)
   ├── ConvertedPdfCard & QueueFileCard (StateFlow Reactive UI)
   └── SettingsDialog (DataStore Preferences)
                   │ (JNI / NDK Bridge)
                   ▼
[ Native Core (Rust) ]
   ├── sdocx Parser (ZIP & Directory Container Extraction)
   ├── Stroke & Shape Reconstructor (Path & Bezier Interpolation)
   └── krilla Vector Engine (PDF 1.7 Lossless Vector Generation)
```

- **UI Framework**: Android Jetpack Compose, Material 3, AndroidX
- **Concurrency**: Kotlin Coroutines, StateFlow, ViewModel
- **Storage**: Android Storage Access Framework (SAF), Scoped Storage, AndroidX FileProvider
- **Native Engine**: Rust 2021, Cargo-NDK, `libsdocx2pdf.so` (ARM64-v8a)
- **Minimum SDK**: Android 8.0 (API 26) / **Target SDK**: Android 15 (API 35)

---

## Credits & License

- **License**: [MIT License](LICENSE.txt)
- **Copyright**: Copyright © 2026 FizzyHedgehog. All rights reserved.
- **Core Engine Credit**: Based on the core conversion engine of [squ1dd13/sdocx2pdf](https://github.com/squ1dd13/sdocx2pdf) under the MIT License.
