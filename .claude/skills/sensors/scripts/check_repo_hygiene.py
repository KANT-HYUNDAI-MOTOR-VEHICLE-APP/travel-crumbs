#!/usr/bin/env python3
"""저장소 위생 — CRLF · 대소문자만 다른 파일 쌍 · 긴 경로 · gradlew 실행 비트 · 심볼릭 링크 (맥 ↔ 윈도우)."""
import argparse
import os
import subprocess
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

CRLF_OK_EXT = {".bat", ".cmd", ".ps1"}
BINARY_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".jar", ".keystore", ".jks", ".ttf", ".otf", ".woff", ".woff2", ".mp3", ".wav", ".pdf", ".zip", ".apk", ".aab", ".so", ".bin"}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("root", nargs="?", default=".")
    ap.add_argument("--max-path", type=int, default=200)
    a = ap.parse_args()
    try:
        out = subprocess.run(["git", "-C", a.root, "ls-files", "-s", "-z"], capture_output=True, check=True).stdout.decode()
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(f".:0 · 입력 · git ls-files 실패: {e}")
        return 2
    entries = []
    for rec in out.split("\0"):
        if not rec:
            continue
        meta, path = rec.split("\t", 1)
        mode = meta.split()[0]
        entries.append((mode, path))
    bad = []
    lower = {}
    for mode, path in entries:
        key = path.lower()
        if key in lower and lower[key] != path:
            bad.append(f"{path}:0 · 대소문자 · {lower[key]} 와 대소문자만 다름 (윈도우는 구분 못 함)")
        lower.setdefault(key, path)
        if len(path) > a.max_path:
            bad.append(f"{path}:0 · 경로 길이 · {len(path)} > {a.max_path}")
        if mode == "120000":
            bad.append(f"{path}:0 · 심볼릭 링크 · 금지 (윈도우 · Gradle)")
        if os.path.basename(path) == "gradlew" and mode != "100755":
            bad.append(f"{path}:0 · 실행 비트 · gradlew 모드 {mode} ≠ 100755 (git update-index --chmod=+x gradlew)")
        ext = os.path.splitext(path)[1].lower()
        if ext in BINARY_EXT or ext in CRLF_OK_EXT or mode == "120000":
            continue
        full = os.path.join(a.root, path)
        try:
            with open(full, "rb") as f:
                head = f.read(1 << 20)
        except OSError:
            continue
        if b"\0" in head:
            continue
        if b"\r\n" in head:
            bad.append(f"{path}:0 · 줄바꿈 · CRLF 포함 (.gitattributes eol=lf · git add --renormalize)")
    for b in bad:
        print(b)
    print(f"check_repo_hygiene: 파일 {len(entries)}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
