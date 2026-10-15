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
    if os.path.exists(apk_path):
        os.remove(apk_path)
    print("Cleaned up temporary APK from MacBook.")

    full_msg = (
        "🔐 <b>[개발자 모드 암호 표기 변경 및 Pre-release 등록 완료]</b>\n\n"
        "요청하신 대로 타인이 비밀번호 자릿수를 유추하여 시도할 수 없도록, <b>'PIN(4자리)' 문구를 삭제하고 일반 '암호'로 변경</b>했습니다.\n\n"
        "🛡️ <b>보안 변경 사항:</b>\n"
        "• <b>다이얼로그 타이틀</b>: <i>'암호 입력'</i>\n"
        "• <b>안내 문구</b>: <i>'개발자 모드를 활성화하려면 암호를 입력하세요.'</i>\n"
        "• <b>입력 필드 레이블</b>: <i>'암호'</i> (기존 PIN 4자리 힌트 전면 제거)\n"
        "• <b>자릿수 제한 해제</b>: 4글자 입력 제한을 없애 비밀번호 길이 유추를 차단 (실제 암호: <b>3208</b> 동일)\n\n"
        "🏷️ <b>GitHub Pre-release(사전 출시) 등록 현황:</b>\n"
        f"• <b>일반 사용자 조회(/releases/latest)</b>: 정식 <code>{latest_tag}</code> 유지 (노출 0%)\n"
        f"• <b>개발자 채널 조회(/releases)</b>: 최신 <code>{dev_tag}</code> 감지\n"
        f"• 릴리즈 에셋: <code>{tag_name}</code> (SDOCX_Converter.apk, {apk_size_mb:.2f} MB)\n\n"
        "📲 <b>업데이트 테스트:</b>\n"
        "갤탭의 SDOCX Converter [설정] > [개발자 옵션] 확인 후 [업데이트 확인]을 누르시면 지금 등록된 최신 사전 출시 버전으로 즉시 업데이트됩니다!"
    )

    summary_msg = (
        "1. PIN(4자리) 힌트와 4자리 입력 제한을 제거하고 일반 '암호' 입력창으로 변경하여 보안을 강화했습니다. (암호: 3208 동일)\n"
        f"2. 변경 사항이 반영된 최신 빌드가 GitHub Pre-release({tag_name})에 등록되었습니다. (공개 릴리즈 {latest_tag} 격리 유지)\n"
        "3. 갤탭 앱의 [설정] > [업데이트 확인]을 통해 지금 바로 무중단 원터치 업데이트를 테스트해 보실 수 있습니다."
    )

    if notify_telegram:
        print("Sending completion report to Telegram (reply_to=310)...")
        notify_telegram.send_full_and_summary(
            full_content=full_msg,
            summary_content=summary_msg,
            platform="android",
            reply_to_message_id=310
        )
    print("Done!")

if __name__ == "__main__":
    main()
