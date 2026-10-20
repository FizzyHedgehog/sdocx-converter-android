# SDOCX Converter for Android

<p align="center"><b>English</b> | <a href="#한국어">한국어</a></p>

<a name="english"></a>

High-performance, lossless vector PDF converter for Samsung Notes (`.sdocx`) documents on Android and Samsung Galaxy Tab devices — handwriting, typed text, and images in one pass.

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Target-Android%2015%20(API%2035)-blue" alt="Target Android 15" />
  <img src="https://img.shields.io/badge/Arch-ARM64--v8a-orange" alt="ARM64" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Core-Rust%20Native%20(NDK)-red" alt="Rust NDK" />
  <img src="https://img.shields.io/badge/Content-Handwriting%20%7C%20Text%20%7C%20Images-yellow" alt="Handwriting, Text and Images" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License" />
</p>

---

## Why SDOCX Converter? (True Vector PDFs vs. Rasterized Bitmaps)

When exporting handwritten notes directly to PDF within the Samsung Notes application, handwriting strokes and highlights are typically rasterized into low-resolution bitmap images. This results in blurry, pixelated text and lines whenever zooming in, viewing on high-resolution displays, or printing.

**SDOCX Converter** solves this issue at the foundation:
By directly parsing the native `.sdocx` container, all S Pen handwriting, marker strokes, and shapes are reconstructed into mathematical vector paths (PDF vector curves). Notes remain razor-sharp at any zoom level, print size, or screen resolution.

---

## Key Features

- **Lossless Vector Graphic Rendering**: Converts handwriting, highlights, formulas, geometric shapes, typed text, and embedded images into smooth vector curves without pixelation.
- **Full Typed-Text Layout**: Typed text is laid out with true glyph font metrics, block flow layout, and Korean syllable-level line wrapping, so mixed handwriting-and-text notes render completely and in the right places.
- **Embedded Fonts (Korean & Latin)**: The engine embeds **NanumBarunGothic** (Regular/Bold, SIL Open Font License) and renders all text with it, so output looks identical on every device regardless of installed system fonts.
- **Rich Text Styling**: Bold, italic, underline, strikethrough, paragraph alignment (left/center/right), and bulleted or numbered lists are all carried into the PDF.
- **All Image Formats (JPEG & PNG)**: Embedded images support rotation, floating-image avoidance around text, background images, and clear text margins.
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

## Known Limitations (Preview)

The text-layout engine is currently distributed through the [v1.2.0](https://github.com/FizzyHedgehog/sdocx-converter-android/releases/tag/v1.2.0) release. While it is verified against reference documents, the following are not yet complete:

- **Free-form (lasso) cropped images and stickers** are stored in Samsung's proprietary `.spi` format and do not appear in the PDF (rectangular crops work normally).
- **Highlighter strokes** render about twice as dark as in Samsung Notes' own PDF export.
- **Rotated multi-line text boxes**: line wrapping inside rotated text boxes can differ from the original in some cases.
- **Image formats**: only JPEG and PNG are supported.
- **Font substitution**: all text is rendered with the embedded NanumBarunGothic. Notes written with fonts other than Samsung's defaults may show shifted line breaks or slightly different glyph placement. (Samsung's own default fonts are proprietary and cannot be embedded.)

See the release notes on the Releases page for the full bilingual feature list.

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

---

<a name="한국어"></a>

## 한국어 버전

<p align="center"><a href="#english">English</a> | <b>한국어</b></p>

삼성노트(`.sdocx`) 문서를 Android 및 Samsung Galaxy Tab 기기에서 고성능으로 무손실 벡터 PDF로 변환하는 앱입니다 — 손글씨, 타이핑 텍스트, 이미지를 한 번에.

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Target-Android%2015%20(API%2035)-blue" alt="Target Android 15" />
  <img src="https://img.shields.io/badge/Arch-ARM64--v8a-orange" alt="ARM64" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Core-Rust%20Native%20(NDK)-red" alt="Rust NDK" />
  <img src="https://img.shields.io/badge/Content-Handwriting%20%7C%20Text%20%7C%20Images-yellow" alt="Handwriting, Text and Images" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License" />
</p>

---

## 왜 SDOCX Converter인가? (진짜 벡터 PDF vs 래스터화 비트맵)

삼성노트 앱에서 필기한 노트를 직접 PDF로 내보내면 손글씨 획과 형광펜이 일반적으로 저해상도 비트맵 이미지로 래스터화됩니다. 그 결과 확대하거나 고해상도 디스플레이에서 보거나 인쇄할 때 텍스트와 선이 흐릿하고 픽셀화되어 보입니다.

**SDOCX Converter**는 이 문제의 근본부터 해결합니다:
네이티브 `.sdocx` 컨테이너를 직접 파싱하여 모든 S Pen 손글씨, 마커 획, 도형을 수학적 벡터 경로(PDF 벡터 곡선)로 재구성하기 때문입니다. 노트는 어떤 확대 배율, 인쇄 크기, 화면 해상도에서도 날카롭게 유지됩니다.

---

## 주요 기능

- **무손실 벡터 그래픽 렌더링**: 손글씨, 형광펜, 수식, 도형, 타이핑 텍스트, 삽입 이미지를 픽셀화 없이 부드러운 벡터 곡선으로 변환합니다.
- **전체 타이핑 텍스트 레이아웃**: 타이핑 텍스트를 실제 글리프 폰트 메트릭, 블록 흐름 레이아웃, 한글 음절 단위 줄바꿈으로 배치하여, 손글씨와 텍스트가 섞인 노트도 빠짐없이 올바른 위치에 렌더링됩니다.
- **내장 폰트 (한글 & 라틴)**: 엔진에 **나눔바른고딕**(Regular/Bold, SIL Open Font License)을 내장하여 모든 텍스트를 렌더링하므로, 기기에 설치된 시스템 폰트와 무관하게 동일한 결과가 나옵니다.
- **리치 텍스트 서식**: 볼드, 이탤릭, 밑줄, 취소선, 문단 정렬(왼쪽/가운데/오른쪽), 불릿/번호 목록이 모두 PDF에 반영됩니다.
- **모든 이미지 포맷 (JPEG & PNG)**: 삽입 이미지는 회전, 텍스트 주변 플로팅 회피, 배경 이미지, 텍스트 여백 확보를 지원합니다.
- **Galaxy Tab 및 대형 화면 최적화**:
  - 태블릿 및 Samsung DeX용 2패널 적응형 레이아웃(왼쪽에 변환 대기열, 오른쪽에 완료된 PDF).
  - 분할 화면 및 팝업 뷰 지원, 반응형 레이아웃.
- **일괄 및 재귀 폴더 변환**:
  - 여러 `.sdocx` 파일을 다중 선택하여 동시에 변환.
  - 폴더 전체를 선택하면 안의 모든 삼성노트를 자동 스캔하여 가져옵니다.
- **삼성노트 및 시스템 연동**:
  - 삼성노트에서 바로 공유: 수신 모드 3가지 — **기본 (앱 열기)**, **백그라운드 무음 자동 저장**, **즉시 PDF 재공유**.
  - 드래그 앤 드롭 지원: 분할 화면 모드에서 My Files 앱에서 `.sdocx` 파일을 직접 드래그.
- **원탭 PDF 보기 및 공유**:
  - 변환된 카드를 탭하면 기본 PDF 리더에서 벡터 PDF를 바로 열기.
  - 외부 앱으로 빠른 공유 또는 여러 파일 일괄 내보내기.
- **Scoped Storage 및 유연한 저장**:
  - 자동 저장 모드: 변환된 PDF를 다운로드 폴더 또는 지정된 사용자 폴더로 자동 내보내기.
  - 수동 내보내기 모드: 내보낼 때까지 격리된 앱 저장소에 보관.
- **획 두께 사용자 지정**:
  - 설정에서 펜 및 마커 획 배율을 조절하여 개인의 손글씨 굵기에 맞춤.
- **스마트 페이지 분할**:
  - 넓은 연속 캔버스 노트를 세로 또는 가로로 분할하는 옵션.
- **백그라운드 변환 알림**:
  - 대용량 노트나 일괄 변환이 백그라운드에서 완료되면 시스템 알림으로 알려줍니다.
- **100% 오프라인 핵심 변환**:
  - 내장된 Rust 네이티브 엔진(Git 서브모듈을 통한 `libsdocx2pdf.so`)으로 구동. 모든 문서 변환은 파일 업로드나 외부 텔레메트리 없이 기기에서 로컬로 실행됩니다.

---

## 삼성노트에서 직접 공유 (워크플로 & 모드)

SDOCX Converter는 Android 시스템 공유 시트에 직접 연동되어, 파일을 수동으로 내보내고 가져오지 않아도 노트 하나 또는 여러 개를 변환할 수 있습니다.

### 사전 조건 & 공유 방법

1. **선호하는 공유 동작 설정 (선택)**:
   - **SDOCX Converter > 설정(톱니바퀴 아이콘) > 공유 수신 시 동작** 에서 들어오는 공유 파일의 처리 방법을 선택하세요.
2. **삼성노트에서 노트 선택**:
   - 삼성노트 목록에서 노트 하나 또는 여러 개를 길게 누르거나(또는 노트를 열고 **더보기(점 3개)** 메뉴에서) **공유**를 탭하세요.
3. **필수 파일 형식 조건 (`삼성노트 파일 (.sdocx)`)**:
   - 삼성노트에서 어떤 형식으로 공유할지 물으면 **반드시 `삼성노트 파일 (.sdocx)`를 선택**해야 합니다.
   - *중요*: 삼성노트의 `PDF 파일`이나 `이미지 파일`은 앱을 떠나기 전에 이미 저해상도 비트맵으로 래스터화된 상태이므로 선택하지 마세요. `삼성노트 파일 (.sdocx)`를 선택하면 진짜 무손실 벡터 경로를 재구성할 수 있는 원본 벡터 노트 컨테이너가 SDOCX Converter에 전달됩니다.
4. **SDOCX Converter 선택**:
   - Android 시스템 공유 시트가 나타나면 **SDOCX Converter**를 탭하세요.

### 공유 수신 모드 (2가지 백그라운드 모드 포함)

**설정 > 공유 수신 시 동작**에서 표준 대화형 모드와 **새로 추가된 2가지 무중단 백그라운드 모드** 중에서 선택할 수 있습니다:

1. **기본 (앱 열기)** *(표준 모드)*
   - SDOCX Converter 메인 인터페이스를 실행하고, 공유된 `.sdocx` 파일을 대기열에 가져온 뒤 앱 안에서 미리보기·관리·수동 내보내기를 하며 대화형으로 변환합니다.
2. **백그라운드 무음 자동 저장** *(신규)*
   - **동작 방식**: 공유된 `.sdocx` 파일을 백그라운드에서 변환하여, 결과 벡터 PDF를 설정된 저장 위치(**기본값은 다운로드 폴더**, 또는 **설정 > 저장 위치 및 보관**에서 설정한 사용자 폴더)에 **SDOCX Converter 창을 열지 않고** 저장합니다.
   - **이렇게 달라집니다**: 화면 전환이나 필기 흐름 방해 없이 삼성노트 안에 그대로 있으면서 무손실 벡터 PDF로 내보낼 수 있습니다. 화면 하단에 1.0초짜리 간결한 플로팅 필 배너(`SDOCX를 PDF로 변환 중...` -> `저장됨: '...'`)가 잠깐 나타나 벡터 PDF가 백그라운드에서 저장되었음을 알려줍니다.
3. **즉시 PDF 재공유** *(신규)*
   - **동작 방식**: 공유된 `.sdocx` 파일을 백그라운드에서 무손실 벡터 PDF로 조용히 변환하고(기본 저장 폴더에도 사본 저장), **새로 생성된 `.pdf` 파일이 첨부된 상태로 Android 시스템 공유 시트를 즉시 엽니다**.
   - **이렇게 달라집니다**: 삼성노트에서 모든 외부 앱(카카오톡, Slack, Gmail, Google Drive, Notion, Goodnotes, Telegram 등)으로 가는 직접적인 **벡터 PDF 브리지** 역할을 합니다. 흐릿한 래스터 PDF를 공유하거나, 파일을 변환한 뒤 파일 관리자에서 찾는 대신, 한 번의 연속된 동작으로 어느 메신저·이메일·클라우드 드라이브로든 선명한 벡터 PDF를 보낼 수 있습니다.

---

## 설치

1. [Releases](https://github.com/FizzyHedgehog/sdocx-converter-android/releases) 페이지에서 최신 **`SDOCX_Converter.apk`**를 다운로드하세요.
2. 기기에서 다운로드한 APK를 열어 설치하세요.
   - **알 수 없는 앱 설치**: 이 오픈소스 앱은 Google Play 외부에서 배포되므로, Android에서 알 수 없는 앱 설치 권한을 요청합니다. 브라우저 또는 파일 관리자에 대해 권한을 활성화하고 진행하세요.
   - **Google Play Protect 안내**: Google Play Protect가 *"인식할 수 없는 개발자"* 또는 *"Play Protect에서 차단됨"* 경고를 표시하면 **자세히**를 탭하고 **그래도 설치**를 선택하세요. 이는 커뮤니티 서명 오픈소스 앱에 대한 표준 Android 보안 확인 절차입니다.
3. 앱을 실행하고 `.sdocx` 파일을 가져와 변환을 시작하세요.

---

## 업데이트

SDOCX Converter는 변환 기록이나 설정을 잃지 않고 원활한 인앱 자체 업데이트를 지원합니다:

- **실행 시 자동 확인**: 기본적으로 앱 시작 시 GitHub Releases에서 새 업데이트를 조용히 확인합니다. 새 버전이 감지되면 릴리즈 노트와 원탭 다운로드·설치 버튼이 있는 업데이트 대화상자가 표시됩니다.
- **수동 업데이트 확인**: **설정(톱니바퀴 아이콘) > 소프트웨어 업데이트 > 업데이트 확인**에서 언제든지 확인할 수 있습니다.
- **자동 업데이트 설정**: **설정 > 소프트웨어 업데이트**의 토글로 실행 시 자동 확인을 켜고 끌 수 있습니다.
- **직접 사이드로드 업데이트**: GitHub Releases에서 최신 `SDOCX_Converter.apk`를 다운로드하여 기존 앱 위에 직접 설치할 수도 있습니다. 모든 릴리즈에서 릴리즈 키스토어가 일관되게 유지되므로, Android가 기존 패키지를 충돌 없이 깔끔하게 업데이트합니다.

---

## 알려진 제한

텍스트 레이아웃 엔진은 현재 [v1.2.0](https://github.com/FizzyHedgehog/sdocx-converter-android/releases/tag/v1.2.0) 릴리즈로 배포됩니다. 참조 문서 대비 검증은 완료했으나 다음 항목은 아직 완성되지 않았습니다:

- **자유롭게(올가미) 자른 이미지와 스티커**는 삼성 고유 포맷 `.spi`로 저장되어 PDF에 나타나지 않습니다(사각형 자르기는 정상 동작).
- **형광펜 획**이 삼성노트 자체 PDF 내보내기보다 약 2배 진하게 렌더링됩니다.
- **회전된 여러 줄 텍스트 박스**: 회전된 텍스트 박스의 줄바꿈이 원본과 다르게 될 수 있습니다.
- **이미지 포맷**: JPEG와 PNG만 지원됩니다.
- **폰트 대체**: 모든 텍스트는 내장된 나눔바른고딕으로 렌더링됩니다. 삼성 기본 폰트 이외의 폰트를 사용한 노트는 줄바꿈이 밀리거나 글자 배치가 달라질 수 있습니다. (삼성의 기본 폰트는 독점 폰트라 내장할 수 없습니다.)

전체 기능 목록은 Releases 페이지의 이중 언어 릴리즈 노트를 참고하세요.

---

## 하드웨어 호환성 & 피드백 환영

이 프로젝트은 오픈소스 유틸리티로 유지보수됩니다. 제한된 하드웨어(Galaxy Tab 모델 하나)에서 개발과 테스트가 진행되어, 다양한 Galaxy 폰(S 시리즈, Z Fold, Z Flip, A 시리즈), 태블릿 화면 비율, One UI/Android 버전 전반에 대한 테스트는 철저하지 않습니다.

다른 기기에서의 테스트와 피드백은 매우 소중합니다:
- **기기 호환성**: 특정 기기에서 예기치 않은 동작, 분할 화면/DeX 레이아웃 잘림, 파일 접근 문제가 발생하면 보고해 주세요.
- **기능 요청**: 손글씨 렌더링, 사용자 경험, 삼성노트 연동에 대한 개선 제안은 언제든지 환영합니다.

[GitHub Issues](https://github.com/FizzyHedgehog/sdocx-converter-android/issues)에 스레드를 열거나 [Issue #1](https://github.com/FizzyHedgehog/sdocx-converter-android/issues/1) 논의에 참여해 주세요.

---

## 기술 아키텍처

```
[ UI 레이어 (Kotlin + Jetpack Compose) ]
   ├── MainScreen (태블릿 2패널 & 폰 단일 컬럼 적응형 레이아웃)
   ├── ShareReceiverActivity (백그라운드 자동 저장, 플로팅 필 & 즉시 재공유)
   ├── UpdateManager (GitHub Releases 인앱 자체 업데이터)
   └── SettingsDialog (SharedPreferences & SAF 저장)
                   │ (JNI / NDK 브리지: rust-core/src/lib.rs)
                   ▼
[ 네이티브 코어 (Rust Git 서브모듈: rust-core/sdocx2pdf) ]
   ├── sdocx 파서 (ZIP 컨테이너 & 노트 페이지 추출)
   ├── 획 & 도형 재구성기 (경로 & 베지어 보간)
   └── krilla 벡터 엔진 (PDF 1.7 무손실 벡터 생성)
```

- **UI 프레임워크**: Android Jetpack Compose, Material 3, AndroidX
- **동시성**: Kotlin Coroutines, StateFlow, ViewModel
- **저장소**: Android Storage Access Framework(SAF), Scoped Storage, AndroidX FileProvider
- **네이티브 엔진**: Rust 2021, Cargo-NDK, `sdocx2pdf` Git 서브모듈에서 빌드된 `libsdocx2pdf.so` (ARM64-v8a)
- **최소 SDK**: Android 8.0 (API 26) / **타겟 SDK**: Android 15 (API 35)

---

## 크레딧 & 라이선스

- **라이선스**: [MIT License](LICENSE.txt)
- **저작권**: Copyright © 2026 FizzyHedgehog. All rights reserved.
- **코어 엔진 크레딧**: MIT License 하의 [squ1dd13/sdocx2pdf](https://github.com/squ1dd13/sdocx2pdf)의 핵심 변환 엔진을 기반으로 합니다.
