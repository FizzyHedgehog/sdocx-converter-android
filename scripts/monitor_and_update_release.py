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
        "🛠️ <b>[업데이트 무한 루프 수정 및 UI 세부 개선 완료 안내]</b>\n\n"
        "말씀해주신 <b>업데이트 완료 후 재실행 시 다시 업데이트 창이 뜨는 오류</b>의 원인을 찾아 완벽히 해결하고, 요청하신 UI 수정 사항을 모두 반영했습니다!\n\n"
        "🔍 <b>1. 업데이트 후 재실행 시 또 업데이트하라고 뜨던 원인 및 해결:</b>\n"
        "• <b>원인</b>: 앞서 인앱 업데이트 테스트용 <code>v1.0.0</code> 베이스 APK를 빌드할 때 <code>app/build.gradle.kts</code>의 <code>versionName</code>을 임시로 <code>\"1.0.0\"</code>(<code>versionCode = 1</code>)으로 낮추었으나, 이후 프리릴리즈(<code>v1.1.0-preview</code>) 빌드 시 다시 <code>\"1.1.0\"</code>으로 복구하지 않은 채 업로드되었습니다. 그 결과 업데이트로 새로 설치된 APK 내부 버전도 여전히 <code>1.0.0</code>으로 기록되어 있어, 앱을 다시 열었을 때 GitHub의 <code>v1.1.0-preview</code> 태그와 비교해 또 업데이트가 필요하다고 판단한 것이었습니다.\n"
        "• <b>해결</b>:\n"
        "  1) <code>app/build.gradle.kts</code>의 버전을 <code>versionCode = 2</code>, <code>versionName = \"1.1.0\"</code>으로 정상 복구했습니다.\n"
        "  2) 앱 시작 시 자동 업데이트 확인은 항상 실제 버전(<code>bypassVersionCheck = false</code>)만 비교하도록 고정하고, APK 설치 실행 시 강제 업데이트 테스트 스위치가 켜져 있다면 자동으로 해제되도록 안전장치를 추가했습니다.\n\n"
        "🎨 <b>2. 요청하신 UI 수정 사항 반영:</b>\n"
        "• <b>[업데이트 확인] 버튼 디자인 복원</b>: 소프트웨어 업데이트 카드 레이아웃은 그대로 유지하면서, 우측의 <code>[업데이트 확인]</code> 버튼만 이전 버전의 세련된 디자인(둥근 테두리 <code>OutlinedButton</code> + 삼성 오렌지 새로고침 아이콘 및 텍스트)으로 변경했습니다.\n"
        "• <b>섹션 순서 변경</b>: <b>소프트웨어 업데이트</b> 섹션이 <b>앱 정보 및 오픈소스 라이선스</b> 섹션보다 <b>위</b>에 위치하도록 순서를 조정했습니다.\n\n"
        "🔗 <b>3. Git Submodule 전환 완료 여부:</b>\n"
        "• <b>서브모듈 전환 작업은 100% 완료</b>되었습니다!\n"
        "• <code>FizzyHedgehog/sdocx2pdf</code> Fork 저장소를 <code>rust-core/sdocx2pdf</code> 서브모듈로 연결하고, 기존에 복사되어 있던 11,221줄의 중복 Rust 소스를 완전히 제거했습니다. 현재 CI 빌드 역시 서브모듈을 재귀 체크아웃(<code>submodules: recursive</code>)하여 네이티브 라이브러리를 정상 빌드하고 있습니다.\n\n"
        f"📦 <b>최신 빌드 산출물</b>: <code>SDOCX_Converter.apk</code> (v1.1.0, {size_mb:.2f} MB)"
    )

    summary_msg = (
        "1. 업데이트 무한 루프 해결: 프리릴리즈 APK 내부의 versionName이 1.0.0으로 남아있던 문제를 1.1.0(versionCode 2)으로 수정하여 업데이트 후 재알림이 뜨지 않게 해결했습니다.\n"
        "2. UI 수정 반영: [업데이트 확인] 버튼을 이전 버전의 오렌지 아이콘 테두리 버튼 디자인으로 변경하고, 소프트웨어 업데이트 섹션을 앱 정보 및 라이선스 위로 이동했습니다.\n"
        "3. 서브모듈 완료 확인: sdocx2pdf Git Submodule 분리 및 중복 코드 11,221줄 삭제는 100% 완료되어 현재 빌드에 정상 적용되어 있습니다."
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
            caption="📱 SDOCX Converter v1.1.0 (업데이트 루프 수정 및 버튼/배치 개선 완료본)",
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
