#!/usr/bin/env python3
"""FastAPI 라우트 검사 — 라우트마다 response_model= 또는 반환 타입 주석이 있나 (ORM 모델 · dict 그대로 반환 금지)."""
import argparse
import os
import re
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

DECO = re.compile(r"@(?:router|app)\.(get|post|put|patch|delete)\s*\(")
DEF = re.compile(r"^\s*(?:async\s+)?def\s+\w+\s*\((.*?)\)\s*(->\s*([^:]+))?:", re.S)
ALLOW_PATHS = ("/health",)


def balanced(src: str, start: int) -> int:
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return i
    return -1


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("app_dir")
    a = ap.parse_args()
    if not os.path.isdir(a.app_dir):
        print(f"{a.app_dir}:0 · 입력 · 폴더 없음")
        return 2
    bad = []
    routes = 0
    for d, _, fs in os.walk(a.app_dir):
        for f in fs:
            if not f.endswith(".py"):
                continue
            p = os.path.join(d, f)
            src = open(p, encoding="utf-8").read()
            for m in DECO.finditer(src):
                end = balanced(src, m.end() - 1)
                if end < 0:
                    continue
                routes += 1
                args = src[m.end():end]
                line = src.count("\n", 0, m.start()) + 1
                path_m = re.search(r"[\"']([^\"']*)[\"']", args)
                path = path_m.group(1) if path_m else ""
                if path in ALLOW_PATHS:
                    continue
                rest = src[end + 1:end + 2000]
                dm = DEF.search(rest)
                has_rm = "response_model" in args
                has_ret = bool(dm and dm.group(3) and dm.group(3).strip() not in ("None", "dict", "Any"))
                if not (has_rm or has_ret):
                    bad.append(f"{p}:{line} · 응답 · {m.group(1).upper()} {path} 에 response_model= 도 반환 타입 주석도 없음")
                if dm and re.search(r"\buser_?id\s*:", dm.group(1), re.I):
                    bad.append(f"{p}:{line} · 접근 · {m.group(1).upper()} {path} 파라미터에 user_id — Depends(current_user) 에서만")
    for b in bad:
        print(b)
    print(f"check_routes: 라우트 {routes}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
