#!/usr/bin/env python3
"""<앱 이름> AndroidManifest 검사 — 허용 밖 권한 · minSdk · cleartext · exported · allowBackup · 네트워크 보안 설정."""
import argparse
import re
import sys
import xml.etree.ElementTree as ET

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")

ANDROID = "{http://schemas.android.com/apk/res/android}"
# 허용 권한 → 왜 쓰는지. **이 표가 허용 목록의 원본**이고, 다른 문서는 목록을 베끼지 않고 여기를 가리킴
# (같은 목록이 여러 곳에 있으면 한 곳만 고쳐져 어긋남). 늘리는 길은 `sensors` 스킬 「권한 허용 목록을 늘리는 길」.
# 근거는 그 권한이 왜 필요한지 적힌 승인된 스펙(docs/spec/<기능명>/artifacts/) 또는 규칙 문서 — 지울지 말지
# 다음 사람이 판단할 수 있게 함께 적음.
ALLOWED = {
    "pleos.car.permission.CAR_DRIVE": "기어(DrivingMode) 읽기 · pleos-vehicle-sdk 2절",
    "android.permission.INTERNET": "서버 API 호출",
    "android.permission.ACCESS_NETWORK_STATE": "연결 유무 확인 · 오프라인 처리",
}
MIN_SDK = 28


def attr(el, name):
    return el.get(ANDROID + name)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("manifest")
    ap.add_argument("--gradle", help="app/build.gradle.kts — minSdk 를 여기서 읽음")
    ap.add_argument("--allow", action="append", default=[],
                    help="허용 목록에 넣기 전 임시 확인용(스펙에 적힌 것만). 커밋 · CI 명령에는 쓰지 않음 — "
                         "로컬과 CI 가 같은 명령이어야 함. 확정되면 ALLOWED 에 근거와 함께 넣을 것")
    a = ap.parse_args()
    allowed = set(ALLOWED) | set(a.allow)
    try:
        root = ET.parse(a.manifest).getroot()
    except (OSError, ET.ParseError) as e:
        print(f"{a.manifest}:0 · 입력 · 매니페스트를 읽지 못함: {e}")
        return 2
    bad = []
    perms = [attr(p, "name") for p in root.iter("uses-permission")]
    for p in perms:
        if p not in allowed:
            bad.append(f"{a.manifest}:0 · 권한 · 허용 밖 권한 {p} (원본 목록은 이 스크립트의 ALLOWED · 늘리려면 sensors 스킬 「권한 허용 목록을 늘리는 길」)")
    if "android.permission.ACCESS_BACKGROUND_LOCATION" in perms:
        bad.append(f"{a.manifest}:0 · 권한 · 백그라운드 위치 권한 금지")
    app = root.find("application")
    if app is None:
        bad.append(f"{a.manifest}:0 · 구조 · <application> 없음")
    else:
        if attr(app, "usesCleartextTraffic") is not None:
            bad.append(f"{a.manifest}:0 · 네트워크 · usesCleartextTraffic 사용 금지 — network_security_config 로 10.0.2.2 만")
        if "android.permission.INTERNET" in perms and attr(app, "networkSecurityConfig") is None:
            bad.append(f"{a.manifest}:0 · 네트워크 · INTERNET 권한이 있는데 networkSecurityConfig 없음")
        if attr(app, "allowBackup") not in (None, "false"):
            bad.append(f"{a.manifest}:0 · 백업 · allowBackup 은 false (샘플 앱 기준 · 세션 토큰 백업 금지)")
        launchers = []
        for comp in list(app):
            if comp.tag not in ("activity", "service", "receiver", "provider"):
                continue
            exported = attr(comp, "exported")
            is_launcher = any(
                c.get(ANDROID + "name") == "android.intent.category.LAUNCHER"
                for f in comp.iter("intent-filter") for c in f.iter("category")
            )
            if is_launcher:
                launchers.append(attr(comp, "name"))
            elif exported == "true":
                bad.append(f"{a.manifest}:0 · exported · {comp.tag} {attr(comp, 'name')} 가 exported=true — LAUNCHER 하나만 허용")
        if len(launchers) != 1:
            bad.append(f"{a.manifest}:0 · 구조 · LAUNCHER 액티비티는 하나 (지금 {len(launchers)}: {launchers})")
        for m in app.iter("meta-data"):
            if "distraction" in (attr(m, "name") or "").lower():
                bad.append(f"{a.manifest}:0 · 메타데이터 · 주행 중 사용 선언 meta-data 는 두지 않음 (제한은 코드가 — 샘플 앱 기준)")
    sdk = root.find("uses-sdk")
    min_sdk = None
    if sdk is not None and attr(sdk, "minSdkVersion"):
        min_sdk = int(attr(sdk, "minSdkVersion"))
    elif a.gradle:
        try:
            m = re.search(r"minSdk\s*=\s*(\d+)", open(a.gradle, encoding="utf-8").read())
            min_sdk = int(m.group(1)) if m else None
        except OSError as e:
            print(f"{a.gradle}:0 · 입력 · 읽지 못함: {e}")
            return 2
    if min_sdk is None:
        bad.append(f"{a.manifest}:0 · minSdk · 값을 찾지 못함 (--gradle 로 build.gradle.kts 를 줄 것)")
    elif min_sdk < MIN_SDK:
        bad.append(f"{a.manifest}:0 · minSdk · {min_sdk} < {MIN_SDK} (Pleos 요구)")
    for b in bad:
        print(b)
    print(f"check_manifest: 권한 {len(perms)}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
