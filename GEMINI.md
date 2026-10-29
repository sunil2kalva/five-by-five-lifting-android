# Workspace Instructions — Five By Five Lifting Native Android App

Always read [`PROJECT_CONTEXT.md`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/PROJECT_CONTEXT.md) before making changes or building the app.

## Critical Rules for This Project
1. **No Local Java Execution:** Corporate macOS Santa blocks local `java`/`./gradlew` execution. All unit tests and APK builds MUST be run via GitHub Actions (`https://github.com/sunil2kalva/five-by-five-lifting-android`) by pushing to `main` and running [`scripts/wait_and_download_apk.py`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/scripts/wait_and_download_apk.py).
2. **Plain-Text Copyable APK URLs:** Always provide `.apk` download URLs inside fenced ` ```text ... ``` ` blocks so clicking to copy on macOS does not trigger an automatic browser download.
3. **Never Commit GitHub PATs:** When pushing to `origin`, temporarily set the remote URL with `GH_TOKEN`, push, and immediately reset `origin` to `https://github.com/sunil2kalva/five-by-five-lifting-android.git`.
