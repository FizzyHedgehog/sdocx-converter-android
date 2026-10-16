#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SDOCX Converter - Monitor CI build, update Pre-release asset, deliver to Telegram, and clean up.
"""

import sys
import os
import json
import time
import ssl
import zipfile
import subprocess
import urllib.request
import urllib.parse
import urllib.error

sys.path.append("/Users/kyungmolee01/.sdocx_telegram_agent")
try:
    import notify_telegram
except ImportError:
    notify_telegram = None

TOKEN = ""
token_file = os.path.expanduser("~/.config/github_token.txt")
if os.path.exists(token_file):
    with open(token_file, "r", encoding="utf-8") as f:
        TOKEN = f.read().strip()

REPO = "FizzyHedgehog/sdocx-converter-android"
SSL_CTX = ssl._create_unverified_context()
OUTPUT_DIR = "/tmp/sdocx_apk_build"
os.makedirs(OUTPUT_DIR, exist_ok=True)

class CrossDomainRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new_req = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new_req is not None:
            new_host = urllib.parse.urlparse(newurl).netloc
            orig_host = urllib.parse.urlparse(req.full_url).netloc
            if new_host != orig_host and 'Authorization' in new_req.headers:
                del new_req.headers['Authorization']
        return new_req

def api_request(endpoint, method="GET", data=None):
    url = f"https://api.github.com/repos/{REPO}/{endpoint}" if not endpoint.startswith("http") else endpoint
    body = None
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "FizzyHedgehog"
    }
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, method=method, data=body, headers=headers)
    with urllib.request.urlopen(req, context=SSL_CTX) as resp:
        content = resp.read().decode("utf-8")
        return json.loads(content) if content else {}

def download_file(url, target_path, max_retries=3):
    opener = urllib.request.build_opener(
        CrossDomainRedirectHandler(),
        urllib.request.HTTPSHandler(context=SSL_CTX)
    )
    for attempt in range(1, max_retries + 1):
        try:
            print(f"Downloading artifact (attempt {attempt}/{max_retries})...")
            req = urllib.request.Request(url, headers={
                "Authorization": f"Bearer {TOKEN}",
                "Accept": "application/vnd.github.v3+json",
                "User-Agent": "FizzyHedgehog"
            })
            with opener.open(req, timeout=60) as resp:
                with open(target_path, "wb") as f:
                    f.write(resp.read())
            if os.path.exists(target_path) and os.path.getsize(target_path) > 0:
                print(f"Downloaded successfully: {os.path.getsize(target_path)} bytes")
                return True
        except Exception as e:
            print(f"[Warn] urllib download attempt {attempt} failed: {e}")
            try:
                curl_cmd = [
                    "curl", "-sL",
                    "-H", f"Authorization: Bearer {TOKEN}",
                    "-H", "Accept: application/vnd.github.v3+json",
                    "-H", "User-Agent: FizzyHedgehog",
                    url, "-o", target_path
                ]
                subprocess.run(curl_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
                if os.path.exists(target_path) and os.path.getsize(target_path) > 0:
                    print(f"Curl fallback succeeded: {os.path.getsize(target_path)} bytes")
                    return True
            except Exception as ce:
                print(f"[Warn] curl fallback attempt {attempt} failed: {ce}")
            time.sleep(3)
    return False

def monitor_run(run_id):
    print(f"Monitoring GitHub Actions run {run_id}...")
    while True:
        data = api_request(f"actions/runs/{run_id}")
        status = data.get("status")
        conclusion = data.get("conclusion")
        print(f"[{time.strftime('%H:%M:%S')}] Status: {status}, Conclusion: {conclusion}")
        if status == "completed":
            if conclusion == "success":
                return True
            else:
                print(f"Build failed with conclusion: {conclusion}")
                return False
        time.sleep(10)

def main():
    run_id = int(sys.argv[1]) if len(sys.argv) > 1 else 38853440127

    if not monitor_run(run_id):
        print("[Error] Build did not succeed!")
        sys.exit(1)

    print(f"Build succeeded! Fetching artifacts for run {run_id}...")
    artifacts_data = api_request(f"actions/runs/{run_id}/artifacts")
    artifacts = artifacts_data.get("artifacts", [])
    if not artifacts:
        print("[Error] No artifacts found!")
        sys.exit(1)

    apk_artifact = None
    for a in artifacts:
        if "APK" in a.get("name", ""):
            apk_artifact = a
            break
    if not apk_artifact:
        apk_artifact = artifacts[0]

    art_id = apk_artifact["id"]
    download_url = f"https://api.github.com/repos/{REPO}/actions/artifacts/{art_id}/zip"
    zip_path = os.path.join(OUTPUT_DIR, "apk_artifact.zip")

    if not download_file(download_url, zip_path):
        print("[Error] Failed to download artifact!")
        sys.exit(1)

    final_apk = os.path.join(OUTPUT_DIR, "SDOCX_Converter.apk")
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        for member in zip_ref.namelist():
            if member.endswith(".apk"):
                with zip_ref.open(member) as source, open(final_apk, "wb") as target:
                    target.write(source.read())
                break
            else:
                zip_ref.extractall(OUTPUT_DIR)

    if os.path.exists(zip_path):
        os.remove(zip_path)

    if not os.path.exists(final_apk):
        print("[Error] Final APK not found!")
        sys.exit(1)

    size_mb = os.path.getsize(final_apk) / (1024 * 1024)
    print(f"APK Extracted: {final_apk} ({size_mb:.2f} MB)")

    # 1. Update GitHub Pre-release asset (Release ID 414138667)
    try:
        rel = api_request("releases/414138667")
        for asset in rel.get("assets", []):
            if asset.get("name") == "SDOCX_Converter.apk":
                print(f"Deleting previous asset {asset['id']}...")
                del_req = urllib.request.Request(
                    f"https://api.github.com/repos/{REPO}/releases/assets/{asset['id']}",
                    method="DELETE",
                    headers={
                        "Authorization": f"Bearer {TOKEN}",
                        "Accept": "application/vnd.github.v3+json",
                        "User-Agent": "FizzyHedgehog"
                    }
                )
                with urllib.request.urlopen(del_req, context=SSL_CTX):
                    pass
                time.sleep(1)

        upload_url = f"https://uploads.github.com/repos/{REPO}/releases/414138667/assets?name=SDOCX_Converter.apk"
        print(f"Uploading new APK to Pre-release: {upload_url}")
        curl_cmd = [
            "curl", "-sL", "-X", "POST",
            "-H", f"Authorization: Bearer {TOKEN}",
            "-H", "Content-Type: application/vnd.android.package-archive",
            "--data-binary", f"@{final_apk}",
            upload_url
        ]
        subprocess.run(curl_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
        print("Pre-release asset updated successfully!")
    except Exception as e:
        print(f"[Warn] Updating GitHub pre-release asset encountered error: {e}")

    # 2. Telegram Delivery
    full_msg = (
        "✨ <b>[소프트웨어 업데이트 UI 개편 및 빌드 완료 안내]</b>\n\n"
        "보내주신 참조 화면(macOS 앱 구조)에 맞추어 안드로이드 앱의 <b>설정 &gt; 소프트웨어 업데이트</b> 영역의 구조를 전면 개편하고 빌드를 완료했습니다!\n\n"
        "🎨 <b>UI 및 기능 개선 내역:</b>\n"
        "1. <b>소프트웨어 업데이트 전용 카드 컨테이너</b>:\n"
        "   • <b>상단 영역</b>: <code>현재 버전: v1.0.1</code> 텍스트와 우측 <code>[업데이트 확인]</code> 버튼을 가로로 정돈 배치했습니다.\n"
        "   • <b>이스터에그 보존</b>: 버전 라벨을 7회 연속 탭하면 비밀번호를 입력하여 개발자 모드를 진입하는 기능은 그대로 유지됩니다.\n"
        "   • <b>구분선 및 하단 옵션</b>: 가로 디바이더 하단에 <code>[☑] 앱 실행 시 자동으로 새 버전 확인</code> 체크박스와 설명 문구(<i>\"체크를 해제하면 수동으로 '업데이트 확인'을 클릭할 때만 확인합니다.\"</i>)를 배치했습니다.\n"
        "   • <b>One UI 테마 일관성</b>: 체크박스 및 프로그레스 인디케이터에 앱 고유의 삼성 오렌지 악센트 컬러(<code>#F58220</code>)를 적용하여 전체 테마와 자연스럽게 어우러지도록 설계했습니다.\n"
        "2. <b>자동 업데이트 확인 설정 연동</b>:\n"
        "   • 체크 해제 시 앱 실행 시 백그라운드 GitHub 버전 확인이 작동하지 않으며, 사용자가 직접 '업데이트 확인' 버튼을 누를 때만 조회하도록 로직을 구현했습니다.\n"
        "3. <b>README 문서화 강화</b>:\n"
        "   • 구글 플레이 프로텍트 경고 발생 시 <b>'세부정보 더보기' &gt; '무시하고 설치'</b> 절차를 명확히 안내했습니다.\n"
        "   • 인앱 자동 업데이트, 수동 업데이트, APK 직접 설치 방식에 대한 안내 섹션을 추가했습니다.\n\n"
        "🗺️ <b>향후 작업 계획 (Roadmap):</b>\n"
        "• <b>1단계: 기기 최종 검증</b>\n"
        "  - 아래 첨부된 최신 APK를 태블릿에 설치하여 개편된 설정 UI와 체크박스 동작, 수동 업데이트 확인 팝업을 최종 점검합니다.\n"
        "• <b>2단계: v1.1.0 정식 릴리즈 배포</b>\n"
        "  - 작업 브랜치(<code>feature/background-share</code>)를 <code>main</code> 브랜치로 병합(Merge)합니다.\n"
        "  - 개발자 모드/PIN 등의 보안 사항을 철저히 배제한 클린한 공식 릴리즈 노트를 작성하여 GitHub 정식 Release <code>v1.1.0</code>을 게시합니다.\n"
        "• <b>3단계: 유지보수 및 업스트림 관리</b>\n"
        "  - squ1dd13의 <code>sdocx2pdf</code> 엔진 업스트림 업데이트 모니터링 및 필요 시 서브모듈 동기화 진행.\n\n"
        f"📦 <b>빌드 산출물</b>: <code>SDOCX_Converter.apk</code> ({size_mb:.2f} MB)"
    )

    summary_msg = (
        "1. 보내주신 구조(상단: 버전+업데이트 확인 버튼 / 하단: 자동 확인 체크박스+설명)를 앱 UI 테마에 맞추어 완벽 개편했습니다.\n"
        "2. 플레이 프로텍트 안내 및 업데이트 가이드를 README에 반영하고, GitHub Pre-release 에셋 갱신과 CI 빌드를 완료했습니다.\n"
        "3. 향후 계획은 기기 최종 확인 후 main 브랜치 병합 및 정식 v1.1.0 릴리즈(클린 릴리즈 노트) 발행 순으로 진행됩니다."
    )

    if notify_telegram:
        print("Sending full message and summary to Telegram...")
        notify_telegram.send_full_and_summary(
            full_content=full_msg,
            summary_content=summary_msg,
            platform="android"
        )

        print("Sending APK document to Telegram...")
        notify_telegram.send_telegram_document(
            final_apk,
            caption="📱 SDOCX Converter 최신 빌드 (개편된 소프트웨어 업데이트 UI 적용)",
            platform="android"
        )

    # Clean up local APK (Strictly respect Rule 1)
    if os.path.exists(final_apk):
        os.remove(final_apk)
    if os.path.exists(OUTPUT_DIR):
        for f in os.listdir(OUTPUT_DIR):
            try:
                os.remove(os.path.join(OUTPUT_DIR, f))
            except Exception:
                pass
    print("Local temporary files cleaned up completely.")

if __name__ == "__main__":
    main()
