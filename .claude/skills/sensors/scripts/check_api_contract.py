#!/usr/bin/env python3
"""API 계약 대조 — 앱 Retrofit 인터페이스의 경로 · 메서드가 openapi.json 에 있나 · DTO 에 좌표 칸 · 요청에 userId 칸."""
import argparse
import json
import os
import re
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

RETROFIT = re.compile(r"@(GET|POST|PUT|PATCH|DELETE|HEAD)\s*\(\s*\"([^\"]+)\"\s*\)")
HTTP_ANN = re.compile(r"@HTTP\s*\(\s*method\s*=\s*\"(\w+)\"\s*,\s*path\s*=\s*\"([^\"]+)\"")
COORD = re.compile(r"\b(lat|lng|lon|latitude|longitude)\s*:", re.IGNORECASE)
USER_ID = re.compile(r"\buser_?id\b", re.IGNORECASE)


def norm(path: str) -> str:
    path = "/" + path.strip("/").split("?")[0]
    return re.sub(r"\{[^}]+\}", "{}", path)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("openapi")
    ap.add_argument("kotlin_dir", help="앱 소스 루트(예: app/src/main/java) — *.kt 를 재귀로 봄")
    a = ap.parse_args()
    try:
        spec = json.load(open(a.openapi, encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as e:
        print(f"{a.openapi}:0 · 입력 · 열지 못함: {e}")
        return 2
    contract = {}
    for path, ops in spec.get("paths", {}).items():
        for method in ops:
            if method.upper() in ("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"):
                contract[(method.upper(), norm(path))] = path
    bad = []
    # 계약 쪽: 요청 스키마 · 파라미터에 userId
    for path, ops in spec.get("paths", {}).items():
        for method, op in ops.items():
            if not isinstance(op, dict):
                continue
            for prm in op.get("parameters", []):
                if USER_ID.search(prm.get("name", "")):
                    bad.append(f"{a.openapi}:0 · 계약 · {method.upper()} {path} 파라미터 {prm['name']} — user_id 는 토큰에서만")
            body = op.get("requestBody", {}).get("content", {})
            for media in body.values():
                ref = media.get("schema", {}).get("$ref", "")
                name = ref.rsplit("/", 1)[-1] if ref else None
                props = spec.get("components", {}).get("schemas", {}).get(name, {}).get("properties", {}) if name else media.get("schema", {}).get("properties", {})
                for p in props:
                    if USER_ID.search(p):
                        bad.append(f"{a.openapi}:0 · 계약 · {method.upper()} {path} 요청 칸 {p} — user_id 는 토큰에서만")
                    if COORD.search(p + ":"):
                        bad.append(f"{a.openapi}:0 · 최소 수집 · {method.upper()} {path} 요청 칸 {p} — 좌표는 서버로 보내지 않음")
    # 앱 쪽
    calls = 0
    for d, _, fs in os.walk(a.kotlin_dir):
        for f in fs:
            if not f.endswith(".kt"):
                continue
            p = os.path.join(d, f)
            try:
                lines = open(p, encoding="utf-8").read().splitlines()
            except OSError:
                continue
            for i, line in enumerate(lines, 1):
                for rx in (RETROFIT, HTTP_ANN):
                    for m in rx.finditer(line):
                        calls += 1
                        key = (m.group(1).upper(), norm(m.group(2)))
                        if key not in contract:
                            bad.append(f"{p}:{i} · 계약 · {key[0]} {m.group(2)} 가 openapi.json 에 없음")
                if "/remote/" in p.replace("\\", "/") and COORD.search(line) and "val " in line:
                    bad.append(f"{p}:{i} · 최소 수집 · DTO 에 좌표 칸 — 서버로 보내지 않음")
    for b in bad:
        print(b)
    print(f"check_api_contract: 계약 {len(contract)}개 · 앱 호출 {calls}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
