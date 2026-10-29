#!/usr/bin/env python3
"""
Polls the latest GitHub Actions workflow run for sunil2kalva/stronglifts-5x5-android,
waits for completion, and downloads the compiled APK artifact to the project root.
Requires GH_TOKEN environment variable to be set.
"""
import io
import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.request
import zipfile

OWNER = "sunil2kalva"
REPO = "stronglifts-5x5-android"
TOKEN = os.environ.get("GH_TOKEN", "").strip()
OUT_DIR = "/Users/sunilkalva/.gemini/jetski/scratch/stronglifts-5x5-android"
APK_DEST = os.path.join(OUT_DIR, "StrongLifts5x5-s26plus-debug.apk")

if not TOKEN:
    print("ERROR: GH_TOKEN environment variable is required.", file=sys.stderr)
    sys.exit(1)

ctx = ssl._create_unverified_context()


class NoAuthRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new_req = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new_req is not None and "Authorization" in new_req.unredirected_hdrs:
            del new_req.unredirected_hdrs["Authorization"]
        if new_req is not None and "Authorization" in new_req.headers:
            del new_req.headers["Authorization"]
        return new_req


opener = urllib.request.build_opener(
    urllib.request.HTTPSHandler(context=ctx),
    NoAuthRedirectHandler(),
)


def api_get(path: str):
    url = path if path.startswith("http") else f"https://api.github.com{path}"
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "User-Agent": "StrongLifts5x5-Builder",
        },
    )
    with urllib.request.urlopen(req, context=ctx) as resp:
        return json.loads(resp.read().decode("utf-8"))


def download_bytes(url: str) -> bytes:
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "User-Agent": "StrongLifts5x5-Builder",
        },
    )
    with opener.open(req) as resp:
        return resp.read()


def main():
    print(f"Polling GitHub Actions runs for {OWNER}/{REPO}...")
    start = time.time()
    run_id = None
    while time.time() - start < 900:
        data = api_get(f"/repos/{OWNER}/{REPO}/actions/runs?per_page=1")
        runs = data.get("workflow_runs", [])
        if not runs:
            print("No workflow runs found yet, waiting 5s...")
            time.sleep(5)
            continue

        run = runs[0]
        run_id = run["id"]
        status = run["status"]
        conclusion = run.get("conclusion")
        run_number = run.get("run_number")
        elapsed = int(time.time() - start)
        print(f"[{elapsed}s] Run #{run_number} (id={run_id}): status={status}, conclusion={conclusion}")

        if status == "completed":
            if conclusion == "success":
                print("Workflow succeeded! Downloading APK artifact...")
                break
            else:
                print(f"Workflow failed with conclusion={conclusion}. Fetching failed job steps...")
                jobs_data = api_get(f"/repos/{OWNER}/{REPO}/actions/runs/{run_id}/jobs")
                for job in jobs_data.get("jobs", []):
                    print(f"Job: {job['name']} -> {job['conclusion']}")
                    for step in job.get("steps", []):
                        print(f"  Step {step['number']}: {step['name']} -> {step['conclusion']}")
                sys.exit(2)

        time.sleep(10)
    else:
        print("Timed out waiting for GitHub Actions run.")
        sys.exit(3)

    artifacts_data = api_get(f"/repos/{OWNER}/{REPO}/actions/runs/{run_id}/artifacts")
    artifacts = artifacts_data.get("artifacts", [])
    if not artifacts:
        print("No artifacts found on completed run!")
        sys.exit(4)

    artifact = artifacts[0]
    archive_url = artifact["archive_download_url"]
    zip_bytes = download_bytes(archive_url)
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zf:
        for name in zf.namelist():
            if name.endswith(".apk"):
                apk_data = zf.read(name)
                with open(APK_DEST, "wb") as f:
                    f.write(apk_data)
                print(f"SUCCESS! APK downloaded to: {APK_DEST} ({len(apk_data)} bytes)")
                return

    print("No .apk file found inside artifact zip!")
    sys.exit(5)


if __name__ == "__main__":
    main()
