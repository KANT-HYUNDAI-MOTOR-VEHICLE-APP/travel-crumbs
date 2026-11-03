#!/usr/bin/env python3
"""Alembic 갈래 검사 — heads 가 하나인가 (두 사람 리비전 병합 누락)."""
import argparse
import os
import subprocess
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--server-dir", default="server")
    a = ap.parse_args()
    versions = os.path.join(a.server_dir, "alembic", "versions")
    if not os.path.isdir(versions) or not any(f.endswith(".py") for f in os.listdir(versions)):
        print("check_alembic_heads: 리비전 없음 · 통과")
        return 0
    try:
        r = subprocess.run(["uv", "run", "alembic", "heads"], cwd=a.server_dir, capture_output=True, text=True, check=True)
    except FileNotFoundError:
        print(f"{a.server_dir}:0 · 입력 · uv 가 없음")
        return 2
    except subprocess.CalledProcessError as e:
        print(f"{a.server_dir}:0 · 입력 · alembic heads 실패: {e.stderr.strip()[:300]}")
        return 2
    heads = [l for l in r.stdout.splitlines() if "(head)" in l]
    if len(heads) != 1:
        print(f"{versions}:0 · 리비전 · heads {len(heads)}개 — `uv run alembic merge -m merge <a> <b>` 로 합칠 것")
        for h in heads:
            print("   " + h.strip())
        return 1
    print(f"check_alembic_heads: head 1개 · {heads[0].strip()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
