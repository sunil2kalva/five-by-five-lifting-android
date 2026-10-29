#!/usr/bin/env python3
"""
Pushes the current working tree to sunil2kalva/five-by-five-lifting-android via GitHub API,
polls the resulting GitHub Actions workflow run, and downloads the compiled APK artifact.
Requires GH_TOKEN environment variable to be set.
"""
import base64
import io
import json
import os
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile

OWNER = "sunil2kalva"
REPO = "five-by-five-lifting-android"
BRANCH = "main"
TOKEN = os.environ.get("GH_TOKEN", "").strip()
OUT_DIR = "/Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android"
APK_DEST = os.path.join(OUT_DIR, "FiveByFiveLifting-s26plus-debug.apk")

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


def api_request(method: str, path: str, payload=None):
    url = path if path.startswith("http") else f"https://api.github.com{path}"
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Accept": "application/vnd.github+json",
        "User-Agent": "FiveByFiveLifting-Builder",
    }
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, context=ctx) as resp:
        return json.loads(resp.read().decode("utf-8"))


def download_bytes(url: str) -> bytes:
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "User-Agent": "FiveByFiveLifting-Builder",
        },
    )
    with opener.open(req) as resp:
        return resp.read()


def push_via_github_api(commit_message: str) -> str:
    print(f"Pushing tracked files to {OWNER}/{REPO} ({BRANCH}) via GitHub Git Data API...")
    ref_data = api_request("GET", f"/repos/{OWNER}/{REPO}/git/ref/heads/{BRANCH}")
    parent_sha = ref_data["object"]["sha"]

    tracked = (
        subprocess.check_output(["git", "ls-files"], cwd=OUT_DIR)
        .decode("utf-8")
        .splitlines()
    )

    tree_items = []
    for rel_path in tracked:
        if not rel_path or rel_path.endswith(".apk"):
            continue
        abs_path = os.path.join(OUT_DIR, rel_path)
        if not os.path.isfile(abs_path):
            continue
        with open(abs_path, "rb") as f:
            raw = f.read()
        blob_resp = api_request(
            "POST",
            f"/repos/{OWNER}/{REPO}/git/blobs",
            {
                "content": base64.b64encode(raw).decode("ascii"),
                "encoding": "base64",
            },
        )
        mode = "100755" if os.access(abs_path, os.X_OK) else "100644"
        tree_items.append(
            {
                "path": rel_path,
                "mode": mode,
                "type": "blob",
                "sha": blob_resp["sha"],
            }
        )

    # Note: omit base_tree so deleted/renamed files from old tree are cleanly removed!
    tree_resp = api_request(
        "POST",
        f"/repos/{OWNER}/{REPO}/git/trees",
        {"tree": tree_items},
    )
    commit_resp = api_request(
        "POST",
        f"/repos/{OWNER}/{REPO}/git/commits",
        {
            "message": commit_message,
            "tree": tree_resp["sha"],
            "parents": [parent_sha],
        },
    )
    new_sha = commit_resp["sha"]
    api_request(
        "PATCH",
        f"/repos/{OWNER}/{REPO}/git/refs/heads/{BRANCH}",
        {"sha": new_sha, "force": True},
    )
    print(f"Pushed commit {new_sha[:7]} to {OWNER}/{REPO}:{BRANCH}")
    return new_sha


def main():
    commit_msg = (
        sys.argv[1]
        if len(sys.argv) > 1
        else subprocess.check_output(["git", "log", "-1", "--pretty=%B"], cwd=OUT_DIR)
        .decode("utf-8")
        .strip()
    )
    expected_sha = push_via_github_api(commit_msg)

    print(f"Polling GitHub Actions runs for {OWNER}/{REPO} (expecting SHA {expected_sha[:7]})...")
    start = time.time()
    run_id = None
    while time.time() - start < 900:
        data = api_request("GET", f"/repos/{OWNER}/{REPO}/actions/runs?per_page=5")
        runs = [r for r in data.get("workflow_runs", []) if r.get("head_sha") == expected_sha]
        if not runs:
            print(f"Waiting for workflow run for {expected_sha[:7]} to appear...")
            time.sleep(6)
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
                jobs_data = api_request("GET", f"/repos/{OWNER}/{REPO}/actions/runs/{run_id}/jobs")
                for job in jobs_data.get("jobs", []):
                    print(f"Job: {job['name']} -> {job['conclusion']}")
                    for step in job.get("steps", []):
                        print(f"  Step {step['number']}: {step['name']} -> {step['conclusion']}")
                sys.exit(2)

        time.sleep(10)
    else:
        print("Timed out waiting for GitHub Actions run.")
        sys.exit(3)

    artifacts_data = api_request("GET", f"/repos/{OWNER}/{REPO}/actions/runs/{run_id}/artifacts")
    artifacts = artifacts_data.get("artifacts", [])
    if not artifacts:
        print("No artifacts found on completed run!")
        sys.exit(4)

    artifact = artifacts[0]
    archive_url = artifact["archive_download_url"]
    zip_bytes = download_bytes(archive_url)
    found_apk = False
    signing_dir = os.path.join(OUT_DIR, "app", "signing")
    os.makedirs(signing_dir, exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zf:
        for name in zf.namelist():
            if name.endswith(".apk"):
                apk_data = zf.read(name)
                with open(APK_DEST, "wb") as f:
                    f.write(apk_data)
                print(f"SUCCESS! APK downloaded to: {APK_DEST} ({len(apk_data)} bytes)")
                found_apk = True
            elif name.endswith("fivebyfive.keystore"):
                ks_dest = os.path.join(signing_dir, "fivebyfive.keystore")
                with open(ks_dest, "wb") as f:
                    f.write(zf.read(name))
                print(f"Saved persistent keystore to: {ks_dest}")
            elif name.endswith("signing-info.txt"):
                info_text = zf.read(name).decode("utf-8", errors="replace")
                info_dest = os.path.join(signing_dir, "signing-info.txt")
                with open(info_dest, "w", encoding="utf-8") as f:
                    f.write(info_text)
                print("\n=== ANDROID SIGNING CERTIFICATE FINGERPRINTS ===")
                for line in info_text.splitlines():
                    if any(k in line for k in ("Alias name:", "Owner:", "SHA1:", "SHA256:", "SHA-1", "SHA-256")):
                        print(line.strip())
                print("================================================\n")

    if not found_apk:
        print("No .apk file found inside artifact zip!")
        sys.exit(5)


if __name__ == "__main__":
    main()
