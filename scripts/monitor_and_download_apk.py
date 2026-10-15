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

TOKEN = os.environ.get("GITHUB_TOKEN", "")
if not TOKEN:
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

    print(f"Extracting APK to temporary directory {OUTPUT_DIR}...")
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
        print(f"🎉 APK successfully extracted: {final_apk} ({size_mb:.2f} MB)")

        full_msg = (
            "🛠️ <b>[SDOCX Converter 갤럭시 스타일 개발자 모드 탑재 빌드]</b>\n\n"
            "요청하신 <b>갤럭시 One UI 동일 7회 클릭 이스터에그 및 PIN(3208) 잠금</b>, 그리고 <b>비공개 사전 출시(Pre-release) 채널</b> 연동이 모두 적용되었습니다.\n\n"
            "🔍 <b>구현 및 동작 상세:</b>\n\n"
            "1. <b>갤럭시 One UI 동일 7회 클릭 카운트다운</b>\n"
            "   • [설정] > [버전 정보] 영역을 연속으로 터치합니다.\n"
            "   • 1~3회: 무반응 (갤럭시 One UI 정책 동일)\n"
            "   • 4회 터치 시: <i>\'개발자 모드를 켜려면 3번 더 누르세요\'</i> (3단계 남음)\n"
            "   • 5회 터치 시: <i>\'개발자 모드를 켜려면 2번 더 누르세요\'</i> (2단계 남음)\n"
            "   • 6회 터치 시: <i>\'개발자 모드를 켜려면 1번 더 누르세요\'</i> (1단계 남음)\n"
            "   • 7회 터치 시: 즉시 <b>PIN 번호 입력 다이얼로그</b> 팝업\n"
            "   • 이미 켜진 상태에서 터치 시: <i>\'개발자 모드가 이미 켜져 있습니다.\'</i>\n\n"
            "2. <b>지정 보안 PIN(3208) 잠금</b>\n"
            "   • 팝업창에 <b>3208</b> 입력 후 확인 시 <i>\'개발자 모드를 켰습니다.\'</i> 토스트와 함께 활성화\n"
            "   • 오입력 시 오류 안내 표시 및 재입력 유도\n\n"
            "3. <b>[개발자 옵션] 신규 설정 메뉴</b>\n"
            "   • <b>개발자 옵션 마스터 스위치</b>: 끄면 즉시 메뉴가 숨겨지고 원복\n"
            "   • <b>사전 출시(Pre-release) 채널</b> (기본 ON): 일반 사용자(releases/latest)에게는 안 보이고 비공개로 올린 Pre-release 버전만 내 기기에서 감지\n"
            "   • <b>동일 버전 재설치 허용 (Bypass Version Check)</b>: 버전이 같아도 강제로 최신 APK를 다운로드/설치 테스트 가능\n"
            "   • <b>[업데이트 화면 시뮬레이션] 버튼</b>: 실제 릴리즈 배포 전에도 업데이트 팝업 및 다운로드 UI를 즉시 테스트 가능\n\n"
            f"• 빌드 파일명: <code>SDOCX_Converter.apk</code> ({size_mb:.2f} MB)"
        )
        summary_msg = (
            "1. 갤럭시 One UI와 동일하게 버전 7회 탭 시 3회 남았을 때부터 카운트다운 토스트가 뜨며 PIN(3208) 입력창이 열립니다.\n"
            "2. 잠금 해제 시 [개발자 옵션]이 열려 일반 사용자에게 노출되지 않는 GitHub Pre-release 채널 및 시뮬레이션 기능을 자유롭게 사용하실 수 있습니다.\n"
            "3. 아래 첨부된 APK를 갤탭에 설치하여 바로 테스트해 보실 수 있습니다."
        )

        if notify_telegram:
            print("Sending message to Telegram (Android channel)...")
            notify_telegram.send_full_and_summary(
                full_content=full_msg,
                summary_content=summary_msg,
                platform="android",
                reply_to_message_id=299
            )

            print("Sending APK file to Telegram (Android channel)...")
            doc_sent = notify_telegram.send_telegram_document(
                final_apk,
                caption="📱 SDOCX Converter (개발자 모드 & Pre-release 채널 지원 APK)",
                platform="android"
            )
            print(f"Telegram document send result: {doc_sent}")

        # 사용자 지침 준수: 맥북 로컬(바탕화면 및 저장소)에 APK를 남기지 않고 즉시 삭제
        if os.path.exists(final_apk):
            os.remove(final_apk)
            print("Cleaned up temporary APK from MacBook.")
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
