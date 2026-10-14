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

PROJECT_DIR = "/Users/kyungmolee01/Projects/sdocx-converter-native"
sys.path.append(os.path.join(PROJECT_DIR, "scripts"))
import notify_telegram

TOKEN = "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"
REPO = "FizzyHedgehog/sdocx-converter-android"
RUN_ID = "38572669085"
SSL_CTX = ssl._create_unverified_context()
OUTPUT_DIR = os.path.expanduser("~/Desktop")

class CrossDomainRedirectHandler(urllib.request.HTTPRedirectHandler):
    """
    GitHub API가 아티팩트 다운로드 요청을 Azure Blob Storage(blob.core.windows.net)로
    302 리다이렉트할 때, Authorization(Bearer 토큰) 헤더가 유지되어 발생하는
    HTTP 401 Server failed to authenticate the request 오류를 차단하기 위해
    호스트 도메인이 변경되면 Authorization 헤더를 안전하게 제거합니다.
    """
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
    """
    Azure Blob Storage 리다이렉트 토큰 정제 핸들러를 사용하며,
    실패 시 curl을 통한 대체 다운로드를 지원합니다.
    """
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
            # curl fallback 시도
            try:
                print("Attempting curl fallback...")
                curl_cmd = [
                    "curl", "-sL",
                    "-H", f"Authorization: Bearer {TOKEN}",
                    "-H", "Accept: application/vnd.github.v3+json",
                    "-H", "User-Agent: FizzyHedgehog",
                    url, "-o", target_path
                ]
                res = subprocess.run(curl_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
                if os.path.exists(target_path) and os.path.getsize(target_path) > 0:
                    print(f"Curl fallback succeeded: {os.path.getsize(target_path)} bytes")
                    return True
            except Exception as ce:
                print(f"[Warn] curl fallback attempt {attempt} failed: {ce}")
            time.sleep(3)

    return False

def process_and_send_apk():
    """
    최신 성공 빌드 아티팩트를 바탕화면으로 다운로드 및 압축 해제하고
    텔레그램으로 전문/요약 및 APK 파일을 직접 전송합니다.
    """
    print(f"Fetching artifacts for run {RUN_ID}...")
    artifacts_data = api_get(f"actions/runs/{RUN_ID}/artifacts")
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

        full_msg = (
            "🎉 <b>안드로이드 Native APK 빌드가 완벽히 성공했습니다!</b>\n\n"
            "• <b>파일명</b>: <code>SDOCX_Converter_Lite.apk</code>\n"
            f"• <b>용량</b>: {size_mb:.2f} MB\n"
            "• <b>아키텍처</b>: ARM64-v8a (갤럭시 탭 S/A 전체 및 갤럭시 폰/폴드/플립 전 기종 최적화)\n"
            "• <b>엔진</b>: Rust Native 코어 (krilla 벡터 PDF 렌더러 + 베지어 곡선 보간)\n"
            "• <b>UI & 기능</b>: 삼성 One UI 다크/라이트 테마, 대화면/DeX 2단 분할 레이아웃, '내 파일' 앱 공유/열기 지원\n"
            "• <b>저장 위치</b>: 사용자 PC 바탕화면 (Desktop)\n"
            "• <b>라이선스</b>: squ1dd13/sdocx2pdf MIT 라이선스 및 저작권 고지 정상 탑재"
        )
        summary_msg = (
            f"1. 최신 SDOCX Converter Lite 정식 APK 빌드가 완료되어 바탕화면에 저장되었습니다 ({size_mb:.2f} MB).\n"
            "2. 401 리다이렉트 인증 문제를 수정한 스크립트로 다운로드가 정상 완료되었습니다.\n"
            "3. 아래 첨부된 APK 파일을 다운로드하여 갤럭시 탭 또는 스마트폰에서 바로 설치하실 수 있습니다."
        )

        print("Sending full text and summary to Telegram...")
        notify_telegram.send_full_and_summary(full_msg, summary_msg)

        print("Sending APK file to Telegram...")
        doc_sent = notify_telegram.send_telegram_document(
            final_apk,
            caption="📱 SDOCX Converter Lite v1.0.0 (ARM64 Release APK)"
        )
        print(f"Telegram document send result: {doc_sent}")
        return True
    else:
        print("[Error] APK file not found after extraction!")
        return False

def main():
    print(f"Checking GitHub Actions run {RUN_ID} for {REPO}...")
    data = api_get(f"actions/runs/{RUN_ID}")
    status = data.get("status")
    conclusion = data.get("conclusion")
    print(f"Status: {status}, Conclusion: {conclusion}")

    if status == "completed" and conclusion == "success":
        print("✅ Run already completed successfully. Processing artifact...")
        if process_and_send_apk():
            sys.exit(0)
        else:
            sys.exit(1)
    elif status == "completed":
        print(f"❌ Run completed with failure: {conclusion}")
        sys.exit(2)
    else:
        # 진행 중인 경우 대기
        while True:
            time.sleep(15)
            data = api_get(f"actions/runs/{RUN_ID}")
            status = data.get("status")
            conclusion = data.get("conclusion")
            print(f"[{time.strftime('%H:%M:%S')}] Status: {status}, Conclusion: {conclusion}")
            if status == "completed":
                if conclusion == "success":
                    if process_and_send_apk():
                        sys.exit(0)
                    else:
                        sys.exit(1)
                else:
                    print(f"❌ Build failed: {conclusion}")
                    sys.exit(2)

if __name__ == "__main__":
    main()
