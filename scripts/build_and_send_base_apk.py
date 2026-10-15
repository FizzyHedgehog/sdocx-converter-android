#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SDOCX Converter - 1.0.0 베이스 APK 모니터링 및 텔레그램 발송 스크립트
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

def api_request(endpoint, method="GET", data=None):
    url = f"https://api.github.com/repos/{REPO}/{endpoint}"
    req = urllib.request.Request(url, method=method, headers={
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

def wait_for_run(run_id):
    print(f"Monitoring GitHub Actions run ID: {run_id}...")
    while True:
        data = api_request(f"actions/runs/{run_id}")
        status = data.get("status")
        conclusion = data.get("conclusion")
        print(f"[{time.strftime('%H:%M:%S')}] Run {run_id} -> Status: {status}, Conclusion: {conclusion}")
        if status == "completed":
            if conclusion == "success":
                return run_id
            else:
                print(f"[Error] Build failed with conclusion: {conclusion}")
                sys.exit(1)
        time.sleep(15)

def download_and_extract_apk(run_id):
    print(f"Fetching artifacts for run {run_id}...")
    artifacts_data = api_request(f"actions/runs/{run_id}/artifacts")
    artifacts = artifacts_data.get("artifacts", [])
    if not artifacts:
        print("[Error] No artifacts found!")
        sys.exit(1)

    apk_artifact = artifacts[0]
    art_id = apk_artifact["id"]
    download_url = f"https://api.github.com/repos/{REPO}/actions/artifacts/{art_id}/zip"
    zip_path = os.path.join(OUTPUT_DIR, "apk_artifact.zip")

    if not download_file(download_url, zip_path):
        print("[Error] Failed to download artifact zip.")
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

    return final_apk

def main():
    run_id = int(sys.argv[1]) if len(sys.argv) > 1 else 38787191402
    wait_for_run(run_id)
    apk_path = download_and_extract_apk(run_id)
    apk_size_mb = os.path.getsize(apk_path) / (1024 * 1024)

    full_msg = (
        "📦 <b>[버전 안내 및 1.0.0 베이스 APK 발송]</b>\n\n"
        "질문해 주신 내용과 다중 이미지 일괄 처리 요청에 대해 답변드립니다.\n\n"
        "1. <b>'지금 새로 보낸 앱은 1.0.0 버전 앱이지?'에 대한 답변</b>:\n"
        "• <b>아닙니다!</b> 앞서 보내드린 앱은 <code>1.1.0</code> 버전이었습니다.\n"
        "• 이미 1.1.0인 상태에서는 GitHub Pre-release(v1.1.0-preview)와 버전이 같기 때문에, 업데이트 창이 뜨지 않고 '최신 버전을 사용하고 있습니다'로 나오게 됩니다.\n"
        "• 따라서 <b>자연스러운 1.0.0 ➔ 1.1.0 인앱 업데이트 흐름</b>을 직접 검증해 보실 수 있도록, <b>방금 적용한 영구 서명 키(keystore.jks)로 동일하게 서명된 '1.0.0 베이스 앱'</b>을 빌드하여 아래 첨부해 드립니다!\n\n"
        "2. <b>사진 여러 장 및 설명 묶음 일괄 처리 개편 완료</b>:\n"
        "• 텔레그램 중앙 디스패처(<code>dispatcher.py</code>)에 <b>Media Group(앨범 묶음) 자동 취합 엔진</b>을 탑재했습니다.\n"
        "• 이제 텔레그램으로 사진 여러 장과 설명을 묶어서 보내셔도, 개별 사진마다 따로 반응하지 않고 <b>하나의 말풍선 묶음으로 모든 사진과 캡션을 한 번에 취합하여 처리</b>합니다!\n\n"
        "📲 <b>최종 테스트 절차:</b>\n"
        "1. 갤탭에서 기존 앱을 <b>길게 눌러 1회 삭제</b>합니다.\n"
        "2. 아래 첨부된 <b>1.0.0 베이스 APK</b>를 설치합니다.\n"
        "3. 앱 실행 후 [설정] > 버전 7회 탭 ➔ 암호(3208) 입력으로 개발자 옵션 활성화\n"
        "4. [업데이트 확인] 터치 ➔ <b>'새로운 버전 발견 (v1.1.0-preview)'</b> 팝업 표시 확인\n"
        "5. [지금 업데이트] 터치 ➔ <b>패키지 충돌 없이 매끄럽게 1.1.0으로 자동 업데이트 성공!</b>"
    )

    summary_msg = (
        "1. 앞서 보낸 앱은 1.1.0이었으며, 인앱 업데이트 테스트를 위해 영구 서명 키가 적용된 '1.0.0 베이스 APK'를 새로 빌드하여 아래 전송해 드립니다.\n"
        "2. 사진 여러 장과 설명을 보낼 때 하나씩 처리하지 않고 묶음(앨범) 단위로 한 번에 처리하도록 디스패처를 완벽 개편했습니다.\n"
        "3. 기존 앱 삭제 후 아래 첨부된 1.0.0 APK를 설치하시면, [업데이트 확인]을 통해 1.1.0으로 무충돌 자동 갱신되는 전 과정을 테스트하실 수 있습니다."
    )

    if notify_telegram:
        print("Sending response to Telegram (reply_to=340)...")
        notify_telegram.send_full_and_summary(
            full_content=full_msg,
            summary_content=summary_msg,
            platform="android",
            reply_to_message_id=340
        )

        print("Sending 1.0.0 APK file to Telegram...")
        notify_telegram.send_telegram_document(
            apk_path,
            caption="📱 SDOCX Converter v1.0.0 (인앱 업데이트 테스트용 베이스 APK)",
            platform="android"
        )

    # 사용자 지침 준수: 맥북 로컬 파일 즉시 삭제
    if os.path.exists(apk_path):
        os.remove(apk_path)
    print("Cleaned up temporary APK from MacBook.")
    print("Done!")

if __name__ == "__main__":
    main()
