#!/usr/bin/env python3
"""환경 검사 — 맥 · 윈도우에서 travel-crumbs 개발에 필요한 것이 같게 있나 (JDK · SDK · Pleos 이미지 · AVD · adb · Docker · uv · gh · git 설정)."""
import os
import platform
import re
import shutil
import subprocess
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

AVD_NAME = "Pleos_Connect_v2.1.0"
IS_WIN = platform.system() == "Windows"
IS_MAC = platform.system() == "Darwin"
rows = []


def add(status, item, detail):
    rows.append((status, item, detail))


def run(cmd, **kw):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=60, **kw)
        return r.returncode, (r.stdout + r.stderr).strip()
    except (FileNotFoundError, subprocess.TimeoutExpired) as e:
        return 127, str(e)


def sdk_root():
    for k in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(k):
            return os.environ[k]
    if IS_MAC:
        return os.path.expanduser("~/Library/Android/sdk")
    if IS_WIN:
        return os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk")
    return os.path.expanduser("~/Android/Sdk")


def java_home():
    if os.environ.get("JAVA_HOME"):
        return os.environ["JAVA_HOME"]
    if IS_MAC:
        return "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    if IS_WIN:
        return r"C:\Program Files\Android\Android Studio\jbr"
    return ""


def main() -> int:
    add("OK", "OS", f"{platform.system()} {platform.machine()}")
    v = sys.version_info
    add("OK" if (v.major, v.minor) >= (3, 12) else "WARN", "Python", f"{v.major}.{v.minor} (서버는 uv 가 3.12 를 따로 깔음)")

    jh = java_home()
    java = os.path.join(jh, "bin", "java.exe" if IS_WIN else "java")
    rc, out = run([java, "-version"]) if os.path.exists(java) else (127, "")
    add("OK" if rc == 0 else "FAIL", "JDK", f"{jh} → {out.splitlines()[0] if out else '없음 (Android Studio JBR 을 JAVA_HOME 으로)'}")

    sdk = sdk_root()
    plats = sorted(os.listdir(os.path.join(sdk, "platforms"))) if os.path.isdir(os.path.join(sdk, "platforms")) else []
    add("OK" if "android-34" in plats else "FAIL", "SDK 플랫폼", f"{sdk} → {plats or '없음'} (34 필수 · compileSdk 플랫폼은 app/build.gradle.kts 참조)")
    img = os.path.join(sdk, "system-images", "android-34")
    pleos = [d for d in (os.listdir(img) if os.path.isdir(img) else []) if "pleos" in d.lower()]
    add("OK" if pleos else "FAIL", "Pleos 이미지", f"{img} → {pleos or '없음 (SDK Update Sites 에 Pleos 주소 · OS 별 URL)'}")
    avd_dir = os.path.join(os.path.expanduser("~"), ".android", "avd")
    has_avd = os.path.exists(os.path.join(avd_dir, AVD_NAME + ".ini"))
    add("OK" if has_avd else "FAIL", "AVD", f"{AVD_NAME} {'있음' if has_avd else '없음 (이름을 이것으로 통일)'}")
    for tool, path in (("adb", os.path.join(sdk, "platform-tools", "adb")), ("emulator", os.path.join(sdk, "emulator", "emulator"))):
        found = shutil.which(tool) or (path if os.path.exists(path) or os.path.exists(path + ".exe") else None)
        add("OK" if found else "WARN", tool, found or "PATH 에 없음 (sdk 안 경로로 실행 가능)")

    rc, out = run(["docker", "info", "--format", "{{.ServerVersion}}"])
    add("OK" if rc == 0 else "FAIL", "Docker", out.splitlines()[0] if rc == 0 else "데몬 없음 (Docker Desktop · 윈도우는 WSL2)")
    rc, out = run(["uv", "--version"])
    add("OK" if rc == 0 else "FAIL", "uv", out if rc == 0 else "없음 (https://docs.astral.sh/uv/)")
    if not shutil.which("gh"):
        add("FAIL", "gh", "없음 (맥 brew install gh · 윈도우 winget install GitHub.cli → gh auth login)")
    else:
        rc, out = run(["gh", "auth", "status"])
        add("OK" if rc == 0 else "FAIL", "gh", "로그인됨" if rc == 0 else "로그인 안 됨 (gh auth login)")

    rc, out = run(["git", "config", "--get", "core.autocrlf"])
    add("OK" if out in ("", "false", "input") else "FAIL", "git autocrlf", out or "(미설정 = false)")
    if IS_WIN:
        rc, out = run(["git", "config", "--get", "core.longpaths"])
        add("OK" if out == "true" else "FAIL", "git longpaths", out or "false (git config core.longpaths true)")
        cwd = os.getcwd()
        add("OK" if len(cwd) <= 40 else "WARN", "저장소 경로", f"{cwd} ({len(cwd)}자 · 짧게)")
    rc, out = run(["git", "rev-parse", "--show-toplevel"])
    add("OK" if rc == 0 else "FAIL", "git 저장소", out if rc == 0 else "저장소 아님")

    width = max(len(i) for _, i, _ in rows)
    for st, item, detail in rows:
        print(f"[{st:4}] {item.ljust(width)}  {detail}")
    fails = sum(1 for st, _, _ in rows if st == "FAIL")
    print(f"doctor: FAIL {fails} · WARN {sum(1 for st, _, _ in rows if st == 'WARN')}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
