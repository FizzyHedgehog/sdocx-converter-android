# SDOCX Converter for Android & Galaxy Tab 📱✨

<p align="center">
  <strong>갤럭시 탭 및 스마트폰을 위한 초고속 무손실 벡터(Vector) PDF 변환기</strong><br/>
  삼성노트(<code>.sdocx</code>) 필기 문서를 아무리 확대해도 글씨와 획이 절대 깨지지 않는 선명한 고품질 벡터 PDF로 변환합니다.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Target-Android%2015%20(API%2035)-blue" alt="Target Android 15" />
  <img src="https://img.shields.io/badge/Arch-ARM64--v8a-orange" alt="ARM64" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Core-Rust%20Native%20(NDK)-red" alt="Rust NDK" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License" />
</p>

---

## 💎 Why SDOCX Converter? (흐릿한 비트맵 vs 선명한 100% 무손실 벡터)

> **"삼성노트에서 PDF로 내보내면 왜 글씨가 흐릿하게 깨질까요?"**  
> 삼성노트 앱의 기본 PDF 내보내기 기능은 손글씨와 형광펜 획을 **저해상도 비트맵 이미지(래스터화)**로 변환하여 출력합니다. 이로 인해 태블릿 화면을 확대하거나 인쇄할 때 글씨 테두리가 깍두기처럼 깨지고 흐릿해집니다.
> 
> **SDOCX Converter**는 이 문제를 근본적으로 해결합니다:  
> 삼성노트의 원본 파일인 **`.sdocx`**를 직접 파싱하여, S펜 손글씨·형광펜·도형·직선 획을 수학적 곡선 경로인 **무손실 벡터 그래픽(Vector Paths)**으로 재구성합니다. 태블릿을 아무리 확대해도, 대형 모니터로 보거나 고해상도로 인쇄해도 한 치의 흐림 없는 칼날 같은 선명도를 자랑합니다.

---

## ✨ 주요 기능 (Key Features)

### 🎨 1. 100% 무손실 벡터 그래픽 렌더링
- 손글씨, 형광펜, 수식, 도형의 모든 필기 획을 완벽한 벡터 곡선으로 변환합니다.
- 400%, 800% 이상 무한 확대해도 픽셀 깨짐이 전혀 없습니다.

### 📱 2. 삼성 갤럭시 대화면 & 멀티윈도우 완벽 최적화
- **갤럭시 탭 / 삼성 DeX (Two-Pane)**: 대화면 환경에서 좌측 [문서 추가 및 변환 대기열] + 우측 [변환 완료 PDF 목록]의 2단 어댑티브 레이아웃을 제공합니다.
- **분할 화면 (Split Screen) & 팝업 화면**: 삼성노트와 나란히 띄워두고 작업할 때도 가로형 UI 배치와 동적 확장(`wrapContentHeight`)으로 잘림 없이 편안하게 이용할 수 있습니다.

### 📂 3. 다중 파일 및 폴더 통째 일괄 변환
- 원하는 `.sdocx` 파일들을 자유롭게 다중 선택하여 한 번에 변환할 수 있습니다.
- 폴더를 지정하면 폴더 내부의 모든 삼성노트 파일들을 자동으로 탐색하여 대기열에 일괄 등록합니다.

### 🔗 4. 삼성노트 및 '내 파일' 앱과의 완벽한 연동
- **삼성노트 즉시 공유**: 삼성노트 앱에서 노트를 길게 누른 후 `공유 > SDOCX 변환기`를 선택하면 앱이 열리며 즉시 대기열에 등록됩니다.
- **분할 화면 드래그 앤 드롭**: '내 파일(My Files)' 앱을 옆에 띄우고 노트를 끌어다 놓으면 즉시 추가됩니다.

### 👆 5. 원터치 PDF 즉시 열기 및 간편 공유
- 변환 완료된 PDF 카드를 탭하면 번거로운 파일 탐색 없이 기본 PDF 뷰어로 즉시 열립니다.
- 개별 공유 및 카카오톡, 이메일, 삼성 퀵쉐어(Quick Share)로 바로 전달할 수 있습니다.
- 일괄 선택 모드를 통해 여러 PDF를 한꺼번에 선택하여 공유하거나 삭제할 수 있습니다.

### 💾 6. 안드로이드 Scoped Storage 맞춤 저장
- **자동 저장 모드**: 변환 완료 시 지정한 폴더로 즉시 자동 저장되도록 설정할 수 있습니다.
- **수동 저장 모드**: 기기 내부 공간을 아끼고 싶다면 앱 내부 격리 저장소에 보관하다가 필요할 때만 원하는 위치로 내보낼 수 있습니다.

### ⚙️ 7. 펜 및 마커 굵기 비율 커스텀 조절
- 기본 펜(1.5x) 및 형광펜(1.0x)의 굵기 비율을 슬라이더로 0.1 단위로 자유롭게 미세 조정할 수 있습니다.

### 🔔 8. 백그라운드 변환 알림 지원
- 대용량 노트나 다량의 문서를 변환할 때 다른 작업을 하고 있어도, 변환이 끝나면 시스템 푸시 알림으로 즉시 알려줍니다.

### 🔒 9. 100% 로컬 오프라인 & 철저한 개인정보 보호
- 변환 엔진이 앱 내부(Rust Native Core)에 내장되어 있어 **외부 인터넷 연결 없이 100% 오프라인**으로 작동합니다.
- 사용자의 중요한 노트와 필기 데이터가 외부 서버로 절대 전송되지 않습니다.

---

## 📥 다운로드 및 설치 방법 (Installation)

1. [Releases](https://github.com/FizzyHedgehog/sdocx-converter-android/releases) 탭에서 최신 버전의 **`SDOCX_Converter.apk`** 파일을 다운로드합니다.
2. 다운로드된 APK 파일을 실행하여 설치를 진행합니다.
   - *안내*: 구글 플레이스토어 외부에서 직접 배포되는 오픈소스 앱이므로, 최초 설치 시 **"출처를 알 수 없는 앱 설치 허용"** 안내가 나타날 수 있습니다. **'이 출처의 앱 허용'**을 켜고 계속 진행하시면 정상적으로 설치됩니다.
3. 앱 설치 완료 후 바로 삼성노트 문서를 추가하여 변환을 시작하세요!

---

## ⚠️ 기기 테스트 안내 및 사용자 피드백 요청 (Feedback Wanted!)

> **📢 다양한 갤럭시 기기 사용자분들의 소중한 피드백을 기다립니다!**  
> 
> 본 앱은 개인 오픈소스 프로젝트로, **개발 환경에서 보유하고 있는 삼성 기기(특정 갤럭시 탭 모델)가 제한적**이어서 시중에 출시된 모든 갤럭시 스마트폰(Galaxy S20~S24 시리즈, Z Fold/Flip 시리즈, A 시리즈), 다양한 화면비의 갤럭시 탭, 그리고 안드로이드 및 One UI 버전별 전수 테스트를 진행하기에는 현실적인 한계가 있었습니다.
> 
> 따라서 여러분의 다양한 기기에서 직접 사용해보시며:
> 1. **버그 및 이상 현상 제보**:
>    - 특정 기기나 안드로이드 버전에서 변환이 멈추거나 파일 접근에 실패하는 경우
>    - 분할 화면, 팝업 화면, 가로/세로 회전 시 UI가 어색하거나 깨지는 경우
> 2. **기능 제안 및 개선 요청**:
>    - *"이런 편의 기능이 추가되면 좋겠다"*
>    - *"삼성노트와의 연동이나 필기 렌더링에서 이런 점이 더 개선되었으면 좋겠다"*
> 
> 등의 의견이 있으시다면 언제든지 **[GitHub Issues](https://github.com/FizzyHedgehog/sdocx-converter-android/issues)** 또는 **[📢 기기 호환성 및 피드백 공지 이슈](https://github.com/FizzyHedgehog/sdocx-converter-android/issues/1)**에 편하게 글을 남겨주세요! 보내주시는 피드백을 하나하나 소중히 검토하여 빠르게 반영하겠습니다. 🙌

---

## 🏗️ 시스템 아키텍처 (Tech Stack)

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
- **Min SDK**: Android 8.0 (API 26) / **Target SDK**: Android 15 (API 35)

---

## 📜 오픈소스 라이선스 & 크레딧

- **License**: [MIT License](LICENSE.txt)  
- **Copyright**: Copyright © 2026 FizzyHedgehog. All rights reserved.  
- **Core Engine Credit**: Based on the core conversion engine of [squ1dd13/sdocx2pdf](https://github.com/squ1dd13/sdocx2pdf) under the MIT License.
