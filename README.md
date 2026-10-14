# SDOCX Converter Lite for Android & Galaxy Tab 📱✨

> **Lossless Vector PDF Converter for Samsung Notes (`.sdocx`)**  
> 갤럭시 탭(태블릿) 및 갤럭시 스마트폰을 위한 고성능 무손실 벡터 PDF 변환 유틸리티입니다.

---

## 💎 Why SDOCX Converter Lite on Galaxy?

1. **100% 무손실 벡터(Vector) 렌더링**:
   - 삼성노트 기본 내보내기는 필기 획을 비트맵 이미지로 변환(래스터화)하여 확대하거나 인쇄할 때 글씨가 깨집니다.
   - 본 앱은 원본 `.sdocx` 데이터를 직접 파싱하여 아무리 확대해도 선명한 **무한 확대 벡터 PDF**를 생성합니다.
2. **삼성 DeX 및 대화면 최적화 (Two-Pane)**:
   - 갤럭시 탭 가로 모드 및 삼성 DeX 환경에서 좌측 드롭존 + 우측 변환 대시보드로 자동 분할되는 적응형 UI를 제공합니다.
3. **'내 파일(My Files)' 앱과 완벽 연동**:
   - **분할 화면 드래그 앤 드롭**: '내 파일' 앱에서 `.sdocx` 파일을 앱 화면으로 끌어다 놓으면 즉시 추가됩니다.
   - **공유(Share) 및 연결 프로그램**: 파일 관리자에서 `.sdocx` 파일을 롱클릭 후 `공유 > SDOCX 변환기`를 누르면 즉시 변환됩니다.
4. **완벽한 개인정보 보호 (100% 로컬 변환)**:
   - 외부 서버 통신 없이 기기 내부에서 네이티브 엔진으로만 동작하여 민감한 노트 필기가 외부로 유출되지 않습니다.
   - 변환된 결과물은 기기의 `Download/SDOCX_PDF` 폴더에 즉시 안전하게 저장됩니다.

---

## 🏗️ Architecture

- **UI & Framework**: Kotlin + Jetpack Compose + Material 3 (One UI Adaptive Navigation)
- **Engine Core**: Rust 기반 벡터 렌더링 파이프라인 (JNI / NDK)
- **Minimum SDK**: Android 8.0 (API 26) - 99% 이상의 갤럭시 기기 지원
- **Target SDK**: Android 15 (API 35)

---

## 📜 Open Source & License

- **License**: [MIT License](LICENSE.txt)
- **Copyright**: Copyright © 2026 FizzyHedgehog. All rights reserved.
- **Third-Party Credit**: Based on [squ1dd13/sdocx2pdf](https://github.com/squ1dd13/sdocx2pdf) under the MIT License.
