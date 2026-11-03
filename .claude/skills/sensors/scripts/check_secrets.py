#!/usr/bin/env python3
"""비밀 리터럴 검사 — 키 · 토큰 · 비밀번호 값, 그리고 앱 폴더의 clientSecret · Pleos 토큰 엔드포인트."""
import argparse
import os
import re
import subprocess
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

VALUE_PATTERNS = [
    (re.compile(r"clientSecret\s*[:=]\s*[\"'][^\"']{8,}[\"']"), "clientSecret 값 리터럴"),
    (re.compile(r"PLEOS_CLIENT_SECRET\s*=\s*\S{8,}"), "PLEOS_CLIENT_SECRET 값"),
    (re.compile(r"(OPENAI|LLM)_API_KEY\s*=\s*\S{8,}"), "LLM API 키 값"),
    (re.compile(r"\bsk-[A-Za-z0-9_-]{20,}"), "sk- 꼴 API 키"),
    (re.compile(r"\bAKIA[0-9A-Z]{16}\b"), "AWS 액세스 키"),
    (re.compile(r"\bghp_[A-Za-z0-9]{30,}"), "GitHub 토큰"),
    (re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"), "개인 키"),
    (re.compile(r"JWT_SECRET\s*=\s*\S{8,}"), "JWT 비밀 값"),
]
APP_FORBIDDEN = [
    (re.compile(r"clientSecret"), "앱에 clientSecret 식별자 — 토큰 교환은 서버만"),
    (re.compile(r"api\.pleos\.ai/v1/auth/token"), "앱에서 Pleos 토큰 엔드포인트 호출 금지"),
]
SKIP_DIRS = ("_workspace/", ".git/", "build/", ".gradle/", "node_modules/", ".venv/")
EXAMPLE_FILES = (".env.example",)
TEXT_EXT = {".kt", ".kts", ".java", ".py", ".toml", ".json", ".yaml", ".yml", ".xml", ".md", ".env", ".ini", ".cfg", ".txt", ".properties", ".sh", ".bat", ".ps1", ".gradle", ".ts", ".js"}


def tracked_files(root):
    try:
        out = subprocess.run(["git", "-C", root, "ls-files", "-z"], capture_output=True, check=True).stdout
        return [p for p in out.decode().split("\0") if p]
    except (subprocess.CalledProcessError, FileNotFoundError):
        files = []
        for d, _, fs in os.walk(root):
            for f in fs:
                files.append(os.path.relpath(os.path.join(d, f), root))
        return files


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("root", nargs="?", default=".")
    ap.add_argument("--app-dir", default="app/src/main", help="앱 제품 코드 폴더(여기엔 식별자만 있어도 위반)")
    a = ap.parse_args()
    me = os.path.relpath(os.path.abspath(__file__), os.path.abspath(a.root))
    bad = []
    n = 0
    for rel in tracked_files(a.root):
        if rel == me or rel.startswith(SKIP_DIRS) or os.path.basename(rel) in EXAMPLE_FILES:
            continue
        ext = os.path.splitext(rel)[1]
        if ext and ext not in TEXT_EXT and not rel.startswith(".env"):
            continue
        path = os.path.join(a.root, rel)
        try:
            with open(path, encoding="utf-8", errors="ignore") as f:
                lines = f.readlines()
        except OSError:
            continue
        n += 1
        in_app = rel.startswith(a.app_dir.rstrip("/") + "/")
        for i, line in enumerate(lines, 1):
            for rx, why in VALUE_PATTERNS:
                if rx.search(line):
                    bad.append(f"{rel}:{i} · 비밀 · {why}")
            if in_app:
                for rx, why in APP_FORBIDDEN:
                    if rx.search(line):
                        bad.append(f"{rel}:{i} · 경계 · {why}")
    for b in bad:
        print(b)
    print(f"check_secrets: 파일 {n}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
