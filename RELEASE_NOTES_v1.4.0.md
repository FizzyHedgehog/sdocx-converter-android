## SDOCX Converter v1.4.0 — PDF Merge and Marker Opacity Update

<p align="center"><b>English</b> | <a href="#한국어-설명">한국어</a></p>

This release introduces two major features based on community feedback: multi-file PDF merging with custom page ordering, and fine-grained highlighter opacity control.

---

<a name="english"></a>

## English

### 1. Multi-File PDF Merge & Reorder (New)

You can now merge multiple converted Samsung Notes documents into a single PDF directly within the app before sharing or saving.

- **Lossless Vector Merging**: PDFs are merged at the vector level via the internal rendering engine, preserving full resolution, crisp vector strokes, and embedded font fidelity without quality loss.
- **Custom Page/Document Ordering**: Reorder documents easily with up/down controls before merging.
- **Custom Output Naming**: Specify a custom filename for the merged PDF before sharing or saving.
- **Flexible Workflow**:
  - Merge and share via Android system share sheet (Quick Share, Drive, messaging apps, etc.)
  - Merge and save directly to your device's Downloads folder.

#### How to use PDF Merge:
1. Open the app and ensure documents are converted.
2. In the **Completed** list, tap **Select**.
3. Check two or more documents you wish to combine.
4. Tap **Share** (or **Save to Downloads**).
5. Choose **Merge into Single PDF**.
6. In the dialog, use the **Up / Down** arrows to arrange documents in your desired order, adjust the output filename if desired, and tap **Merge & Share** (or **Merge & Save**).

> *Note: If you prefer direct multi-file sharing without the merge prompt, you can keep **Always Share Separately** enabled in Settings (enabled by default).*

---

### 2. Highlighter Opacity Adjustment (New)

The highlighter (marker) rendering engine has been upgraded to support adjustable opacity.

- **Calibrated Default (35%)**: By default, highlighters now render at 35% opacity, closely matching Samsung Notes' official PDF export where text remains clearly visible beneath highlighted strokes.
- **Custom Opacity Range (0% to 100%)**:
  - `35%` (Default): Soft, translucent highlighting with optimal text legibility.
  - `100%`: Deep, high-contrast highlighting.
  - `0%`: Completely hides all highlighter strokes while keeping handwriting and typed text intact.

#### How to adjust Highlighter Opacity:
1. Tap the **Settings** icon on the top right.
2. Under the Stroke Settings section, locate the **Marker Opacity** slider below the Marker Thickness slider.
3. Adjust the slider from `0%` to `100%` according to your preference and tap **OK**.

---

### 3. Build & Compatibility Information

- **Version**: 1.4.0 (versionCode 6)
- **Target OS**: Android 8.0+ (API 26 to 35, optimized for Samsung Galaxy Tablets & DeX)
- **Architecture**: ARM64-v8a (`aarch64-linux-android`)
- **Package**: `com.fizzyhedgehog.sdocxconverter`

---

<a name="한국어-설명"></a>

## 한국어 설명

### 1. 다중 문서 PDF 병합 및 순서 정렬 기능 추가

변환 완료된 여러 개의 삼성노트 문서를 원하는 순서로 결합하여 하나의 단일 PDF 파일로 공유하거나 저장할 수 있습니다.

- **무손실 벡터 병합**: 렌더링 엔진 내부에서 벡터 수준으로 직접 병합하여 화질 저하 없이 원본 해상도와 폰트를 유지합니다.
- **순서 변경 UI**: 위/아래 화살표 버튼을 통해 원하는 순서대로 문서를 자유롭게 배치할 수 있습니다.
- **파일명 지정**: 결합될 최종 PDF 파일의 이름을 직접 입력할 수 있습니다.

#### 사용 방법:
1. 변환 완료 목록에서 **선택**을 누릅니다.
2. 하나로 합치고 싶은 문서들을 2개 이상 체크합니다.
3. 하단의 **공유** 또는 **다운로드에 저장**을 누릅니다.
4. **하나의 PDF로 병합**을 선택합니다.
5. 화살표 버튼으로 원하는 문서 순서를 정하고, 파일명을 확인한 뒤 **이 순서로 합쳐서 공유/저장**을 누릅니다.

---

### 2. 형광펜 불투명도(투명도) 조절 슬라이더 추가

형광펜 필기 획의 농도를 사용자가 직접 조절할 수 있습니다.

- **기본값 보정 (35%)**: 삼성노트 공식 PDF 내보내기와 동일하게 은은하고 자연스러운 투명도로 텍스트 가독성을 보장합니다.
- **0% ~ 100% 조절 가능**:
  - `35%` (기본값): 은은하고 부드러운 형광펜
  - `100%`: 짙고 선명한 형광펜
  - `0%`: 형광펜 획을 완전히 숨기고 본문과 일반 필기만 출력

#### 사용 방법:
1. 메인 화면 우측 상단의 **환경설정** 아이콘을 누릅니다.
2. 형광펜 굵기 아래에 위치한 **형광펜 불투명도** 슬라이더를 원하는 수치로 조정 후 확인을 누릅니다.
