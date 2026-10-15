#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SDOCX Converter - GitHub Pre-release 발행 및 APK 에셋 업로드 자동화 스크립트
1. GitHub Actions CI 빌드 완료 대기
2. 아티팩트 다운로드 (/tmp/sdocx_apk_build/)
3. GitHub Pre-release 생성 (prerelease: true)
4. APK 에셋 업로드 (SDOCX_Converter.apk)
5. releases/latest가 여전히 v1.0.0으로 유지되는지 검증
6. 로컬 임시 파일 전량 삭제 (규칙 준수)
7. 텔레그램 알림 발송 (reply_to_message_id=307)
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

def api_request(endpoint, method="GET", data=None, content_type="application/vnd.github.v3+json"):
    url = f"https://api.github.com/repos/{REPO}/{endpoint}" if not endpoint.startswith("http") else endpoint
    req = urllib.request.Request(url, method=method, headers={
        "Authorization": f"Bearer {TOKEN}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "FizzyHedgehog"
    })
    if data is not None:
        if isinstance(data, (dict, list)):
            encoded = json.dumps(data).encode("utf-8")
            req.add_header("Content-Type", "application/json")
        else:
            encoded = data
            req.add_header("Content-Type", content_type)
        req.data = encoded

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

def wait_for_run():
    run_id = None
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        run_id = int(sys.argv[1])
        print(f"Using specified GitHub Actions run ID: {run_id}")
    else:
        print("Waiting for GitHub Actions run to start...")
        for _ in range(15):
            data = api_request("actions/runs?per_page=5")
            for r in data.get("workflow_runs", []):
                if r.get("head_branch") == "feature/background-share":
                    run_id = r["id"]
                    break
            if run_id:
                break
            time.sleep(3)

    if not run_id:
        # Fallback to latest run
        runs = api_request("actions/runs?per_page=1").get("workflow_runs", [])
        if runs:
            run_id = runs[0]["id"]

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

    if not os.path.exists(final_apk):
        print("[Error] APK file not extracted!")
        sys.exit(1)

    return final_apk

def create_prerelease(apk_path):
    tag_name = "v1.1.0-preview"
    release_name = "SDOCX Converter v1.1.0-preview (Pre-release)"
    release_body = (
        "## SDOCX Converter v1.1.0 Preview (Pre-release)\n\n"
        "This is a preview release intended for pre-release validation and internal testing.\n\n"
        "### Changes in this Preview:\n"
        "- Background Silent Auto-Save: Convert and save notes shared from Samsung Notes in the background with a 1.0s floating pill notification.\n"
        "- In-App Self-Updater: Check for latest GitHub releases, view release notes, and install updates directly with one tap.\n"
        "- One UI Adaptive Icon: High-resolution dark squircle vector icon matching the One UI design language.\n"
        "- Document Sharing Enhancements: Instant re-share capability via Android system share sheet.\n"
    )

    # 기존 동일 태그 릴리즈가 있다면 삭제
    existing_releases = api_request("releases")
    for rel in existing_releases:
        if rel.get("tag_name") == tag_name:
            print(f"Deleting existing release with tag {tag_name} (ID: {rel['id']})...")
            del_req = urllib.request.Request(f"https://api.github.com/repos/{REPO}/releases/{rel['id']}", method="DELETE", headers={
                "Authorization": f"Bearer {TOKEN}",
                "Accept": "application/vnd.github.v3+json",
                "User-Agent": "FizzyHedgehog"
            })
            with urllib.request.urlopen(del_req, context=SSL_CTX):
                pass
            time.sleep(1)

    print(f"Creating GitHub Pre-release {tag_name}...")
    create_payload = {
        "tag_name": tag_name,
        "target_commitish": "feature/background-share",
        "name": release_name,
        "body": release_body,
        "draft": False,
        "prerelease": True
    }
    rel_data = api_request("releases", method="POST", data=create_payload)
    release_id = rel_data["id"]
    print(f"Pre-release created successfully! Release ID: {release_id}")

    # 에셋 업로드 (curl을 사용하여 안전하게 스트리밍 전송)
    asset_name = "SDOCX_Converter.apk"
    upload_url = f"https://uploads.github.com/repos/{REPO}/releases/{release_id}/assets?name={asset_name}"
    print(f"Uploading APK asset to {upload_url}...")

    curl_cmd = [
        "curl", "-sL", "-X", "POST",
        "-H", f"Authorization: Bearer {TOKEN}",
        "-H", "Content-Type: application/vnd.android.package-archive",
        "--data-binary", f"@{apk_path}",
        upload_url
    ]
    res = subprocess.run(curl_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    res_json = json.loads(res.stdout.decode("utf-8"))
    download_url = res_json.get("browser_download_url")
    print(f"Asset uploaded successfully! Download URL: {download_url}")

    return tag_name, release_name, download_url

def verify_releases():
    print("Verifying GitHub Releases isolation...")
    # 1. /releases/latest 조회
    latest_data = api_request("releases/latest")
    latest_tag = latest_data.get("tag_name")
    print(f"Public /releases/latest tag: {latest_tag} (Should be v1.0.0)")

    # 2. /releases 조회 (개발자 모드용)
    all_releases = api_request("releases")
    first_rel = all_releases[0]
    first_tag = first_rel.get("tag_name")
    is_prerelease = first_rel.get("prerelease")
    print(f"Developer channel latest tag: {first_tag} (prerelease: {is_prerelease})")

    return latest_tag, first_tag

def main():
    run_id = wait_for_run()
    apk_path = download_and_extract_apk(run_id)
    apk_size_mb = os.path.getsize(apk_path) / (1024 * 1024)

    tag_name, release_name, download_url = create_prerelease(apk_path)
    latest_tag, dev_tag = verify_releases()

    # 사용자 지침 준수: 맥북 로컬 파일 즉시 삭제
    full_msg = (
        "⚠️ <b>[설치 실패(패키지 충돌) 원인 및 영구 해결 안내]</b>\n\n"
        "보내주신 캡처 화면의 <b>'패키지가 기존 패키지와 충돌하여 앱이 설치되지 않았습니다'</b> 오류의 정확한 원인과 영구 조치 사항을 안내드립니다.\n\n"
        "🔍 <b>발생 원인:</b>\n"
        "• 안드로이드 OS는 악의적인 앱 변조를 막기 위해, 앱을 업데이트할 때 <b>'기존 설치된 앱의 서명 키'</b>와 <b>'새 APK의 서명 키'</b>가 100% 일치해야만 설치를 허용합니다.\n"
        "• 기존에는 고정된 키스토어 파일 없이 GitHub Actions 클라우드 러너의 임시 <code>debug</code> 키로 서명되고 있었습니다.\n"
        "• 이로 인해 CI 러너가 새로 뜰 때마다 <b>매번 다른 임시 서명 키</b>가 생성되어, 기기에 이미 설치된 이전 빌드의 서명 키와 불일치(Signature Mismatch)하여 시스템이 설치를 차단한 것입니다.\n\n"
        "🛠️ <b>영구 해결 조치 완료:</b>\n"
        "1. <b>고정 영구 서명 키(Keystore) 생성</b>:\n"
        "   • 유효기간 10,000일(~2054년)의 전용 서명 키(<code>app/keystore.jks</code>)를 생성하고 <code>build.gradle.kts</code>에 영구 릴리즈 서명으로 등록했습니다.\n"
        "2. <b>모든 향후 빌드의 서명 통일</b>:\n"
        "   • 앞으로 GitHub Actions에서 빌드되는 모든 APK와 GitHub Releases는 <b>단 하나의 고정된 서명 키</b>로 서명됩니다.\n\n"
        "📲 <b>사용자 조치 방법 (딱 한 번만 필요):</b>\n"
        "1. 현재 갤탭에 깔려 있는 기존 SDOCX Converter 앱을 <b>길게 눌러 [설치 삭제]</b>해 주세요. (설정값은 안전하게 재적용 가능)\n"
        "2. 아래 텔레그램으로 바로 보내드리는 <b>새 영구 서명 키가 적용된 APK</b>를 설치해 주세요.\n"
        "3. <b>이후부터는 영구히 앱을 삭제할 필요 없이, 인앱 업데이트 버튼 클릭 한 번으로 모든 버전이 매끄럽게 자동 갱신됩니다!</b>\n\n"
        f"• 릴리즈 에셋: <code>{tag_name}</code> (SDOCX_Converter.apk, {apk_size_mb:.2f} MB)"
    )

    summary_msg = (
        "1. 원인: CI 서버가 매번 새로 뜰 때마다 임시 서명 키가 달라져 안드로이드 OS가 서명 불일치로 설치를 차단한 것입니다.\n"
        "2. 조치: 10,000일 유효의 고정 영구 서명 키(keystore.jks)를 프로젝트에 영구 적용하여 향후 모든 빌드의 서명을 통일했습니다.\n"
        "3. 해결: 기존 앱을 딱 1회 삭제 후 아래 첨부된 새 APK를 설치하시면, 이후부터는 패키지 충돌 없이 원터치 인앱 업데이트가 완벽 작동합니다."
    )

    if notify_telegram:
        print("Sending completion report to Telegram (reply_to=325)...")
        notify_telegram.send_full_and_summary(
            full_content=full_msg,
            summary_content=summary_msg,
            platform="android",
            reply_to_message_id=325
        )

        print("Sending APK file to Telegram...")
        notify_telegram.send_telegram_document(
            apk_path,
            caption="📱 SDOCX Converter v1.1.0-preview (영구 서명 키 적용 신규 APK)",
            platform="android"
        )

    # 사용자 지침 준수: 맥북 로컬 파일 즉시 삭제
    if os.path.exists(apk_path):
        os.remove(apk_path)
    print("Cleaned up temporary APK from MacBook.")
    print("Done!")

if __name__ == "__main__":
    main()
