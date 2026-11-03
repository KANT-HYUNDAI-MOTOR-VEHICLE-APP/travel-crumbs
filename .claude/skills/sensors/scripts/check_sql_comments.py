#!/usr/bin/env python3
"""SQLAlchemy 모델의 칸 설명 검사 — mapped_column(...) · Column(...) 마다 comment= 가 있나."""
import argparse
import os
import re
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

CALL = re.compile(r"\b(mapped_column|Column)\s*\(")


def balanced(src: str, start: int) -> int:
    depth = 0
    i = start
    while i < len(src):
        c = src[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("paths", nargs="+", help="파일 또는 폴더 (*.py 재귀)")
    a = ap.parse_args()
    files = []
    for p in a.paths:
        if os.path.isdir(p):
            for d, _, fs in os.walk(p):
                files += [os.path.join(d, f) for f in fs if f.endswith(".py")]
        elif os.path.isfile(p):
            files.append(p)
        else:
            print(f"{p}:0 · 입력 · 없음")
            return 2
    bad = []
    cols = 0
    for f in files:
        src = open(f, encoding="utf-8").read()
        for m in CALL.finditer(src):
            end = balanced(src, m.end() - 1)
            if end < 0:
                continue
            cols += 1
            body = src[m.end():end]
            if "comment=" not in body:
                line = src.count("\n", 0, m.start()) + 1
                bad.append(f"{f}:{line} · 설명 · {m.group(1)}( … ) 에 comment= 없음")
    for b in bad:
        print(b)
    print(f"check_sql_comments: 칸 {cols}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
