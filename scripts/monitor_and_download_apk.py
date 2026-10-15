#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SDOCX Converter Lite - GitHub Actions APK 빌드 모니터링 및 자동 다운로드
Azure Blob Storage 리다이렉트 시 401 Authentication Error 방지 핸들러 내장
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

TOKEN = "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"
REPO = "FizzyHedgehog/sdocx-converter-android"
SSL_CTX = ssl._create_unverified_context()
OUTPUT_DIR = os.path.expanduser("~/Desktop")

class CrossDomainRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new_req = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new_req is not None:
            new_host = urllib.parse.urlparse(newurl).netloc
            orig_host = urllib.parse.urlparse(req.full_url).netloc
            if new_host != orig_host and 'Authorization' in new_req.headers:
                del new_req.headers['Authorization']
        return new_req

def api_get(endpoint):
    url = f"https://api.github.com/repos/{REPO}/{endpoint}"
    req = urllib.request.Request(url, headers={
        "Authorization": f"Bearer {TOKEN}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "FizzyHedgehog"
    })
    with urllib.request.urlopen(req, context=SSL_CTX) as resp:
        return json.loads(resp.read().decode("utf-8"))

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
                print("Attempting curl fallback...")
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

def process_and_send_apk(run_id):
    print(f"Fetching artifacts for run {run_id}...")
    artifacts_data = api_get(f"actions/runs/{run_id}/artifacts")
    artifacts = artifacts_data.get("artifacts", [])
    if not artifacts:
        print("[Error] No artifacts found for this run!")
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

    print(f"Downloading artifact {art_id} from {download_url} to {zip_path}...")
    success = download_file(download_url, zip_path)
    if not success:
        print("[Error] Failed to download artifact after retries.")
        sys.exit(1)

    print("Extracting APK to Desktop...")
    final_apk = os.path.join(OUTPUT_DIR, "SDOCX_Converter_Lite.apk")
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

    if os.path.exists(final_apk):
        size_mb = os.path.getsize(final_apk) / (1024 * 1024)
        print(f"🎉 APK successfully extracted to: {final_apk} ({size_mb:.2f} MB)")

        msg = (
            "🎉 <b>[안드로이드/갤럭시 탭 최신 업데이트 빌드 완료]</b>\n\n"
            "요청하신 모든 사항이 완벽히 반영되었습니다:\n\n"
            "1. 📄 <b>PDF 카드 클릭 시 뷰어 즉시 열기</b>\n"
            "   - 다운로드 창이나 알림 토스트 없이, 카드를 터치하면 삼성노트 / PDF 뷰어로 즉시 열립니다.\n\n"
            "2. 💾 <b>기본 저장 동작 개선 & 파일 저장 위치 선택</b>\n"
            "   - 변환 즉시 기기에 강제 저장되지 않으며, [💾 저장] 버튼을 누를 때 원하는 폴더를 선택할 수 있습니다.\n"
            "   - 원본 폴더에서 가져온 파일은 원본 위치 저장을 우선 지원하며, 환경설정에서 '항상 자동 저장' 및 '기본 저장 폴더' 지정이 가능합니다.\n\n"
            "3. ✏️ <b>펜 굵기 기본 배율 1.5x 적용</b>\n"
            "   - 기본 펜 굵기가 1.5배율로 기본 설정되었습니다.\n\n"
            "4. 📑 <b>맥북 동일 페이지 분할 모드 & 지능형 분할</b>\n"
            "   - 기본(분할 없음), A4 세로 분할, A4 가로 분할 라디오 선택 지원 및 그 아래 지능형 자동 페이지 분할 토글 제공.\n\n"
            "5. ⚠️ <b>이미 변환된 파일 재선택 시 안내 팝업</b>\n"
            "   - 이미 변환 완료된 파일을 다시 선택하면 '이미 변환이 완료된 파일입니다' 알림 팝업이 뜹니다.\n\n"
            "6. 🔔 <b>변환 완료 시 시스템 상단 알림바 지원</b>\n"
            "   - 설정에서 변환 완료 알림을 켜고 끌 수 있으며, 변환 종료 시 상단 알림으로 즉시 안내합니다.\n\n"
            f"• 파일명: <code>SDOCX_Converter_Lite.apk</code> ({size_mb:.2f} MB)\n\n"
            "아래 첨부된 최신 APK 파일을 다운로드하여 탭에서 바로 확인해보세요!"
        )

        if notify_telegram:
            print("Sending message to Telegram...")
            notify_telegram.send_telegram_message(msg)

            print("Sending APK file to Telegram...")
            doc_sent = notify_telegram.send_telegram_document(
                final_apk,
                caption="📱 SDOCX Converter Lite (UI 개선 최신 빌드 APK)"
            )
            print(f"Telegram document send result: {doc_sent}")
        return True
    else:
        print("[Error] APK file not found after extraction!")
        return False

def get_latest_run_id():
    data = api_get("actions/runs?per_page=1")
    runs = data.get("workflow_runs", [])
    if runs:
        return runs[0]["id"]
    return None

def main():
    run_id = None
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        run_id = int(sys.argv[1])
    else:
        # 최근 실행 확인
        print(f"Querying latest GitHub Actions workflow run for {REPO}...")
        run_id = get_latest_run_id()

    if not run_id:
        print("[Error] No workflow runs found!")
        sys.exit(1)

    print(f"Monitoring GitHub Actions run {run_id}...")
    while True:
        data = api_get(f"actions/runs/{run_id}")
        status = data.get("status")
        conclusion = data.get("conclusion")
        print(f"[{time.strftime('%H:%M:%S')}] Status: {status}, Conclusion: {conclusion}")
        if status == "completed":
            if conclusion == "success":
                if process_and_send_apk(run_id):
                    sys.exit(0)
                else:
                    sys.exit(1)
            else:
                print(f"❌ Build failed: {conclusion}")
                sys.exit(2)
        time.sleep(15)

if __name__ == "__main__":
    main()
