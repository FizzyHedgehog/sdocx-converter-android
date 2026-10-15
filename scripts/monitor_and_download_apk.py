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

    if os.path.exists(final_apk):
        size_mb = os.path.getsize(final_apk) / (1024 * 1024)
        print(f"🎉 APK successfully extracted to: {final_apk} ({size_mb:.2f} MB)")

        msg = (
            "🎉 <b>[SDOCX Converter 폴더 선택 및 저장 탐색기 개선 업데이트]</b>\n\n"
            "요청해주신 두 가지 사항이 완벽히 개선되었습니다!\n\n"
            "🔍 <b>개선 및 안내 내용:</b>\n"
            "1. <b>폴더 / 파일 동시 선택 안내 및 추가 방식 개편</b>:\n"
            "   • <b>안내</b>: 안드로이드 OS 자체 보안 정책(SAF)상 단일 창에서 파일 다중 선택과 폴더 트리 선택을 동시에 할 수 없도록 시스템이 분리되어 있습니다.\n"
            "   • <b>해결</b>: 좌측 주황색 영역에 <b>[파일 선택 (다중)]</b>과 <b>[폴더 선택]</b> 원터치 버튼을 함께 제공하고, 영역 어디를 터치하거나 우하단 <b>[+] 버튼</b>을 누르면 파일/폴더를 고를 수 있는 <b>추가 다이얼로그</b>가 열리도록 했습니다!\n\n"
            "2. <b>저장할 폴더 선택 시 팝업 제거 ➡️ 즉시 탐색기 실행</b>:\n"
            "   • 선택 모드에서 <b>[저장할 폴더 선택]</b> 클릭 시 중간 팝업창 없이 <b>안드로이드 폴더 선택 탐색기가 즉시 실행</b>되어 원하는 위치로 바로 저장됩니다!\n\n"
            f"• 파일명: <code>SDOCX_Converter.apk</code> ({size_mb:.2f} MB)\n\n"
            "아래 첨부된 최신 APK 파일을 다운로드하여 탭에서 바로 확인해보세요!"
        )

        if notify_telegram:
            print("Sending message to Telegram...")
            notify_telegram.send_telegram_message(msg)

            print("Sending APK file to Telegram...")
            doc_sent = notify_telegram.send_telegram_document(
                final_apk,
                caption="📱 SDOCX Converter (최신 빌드 APK)"
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
