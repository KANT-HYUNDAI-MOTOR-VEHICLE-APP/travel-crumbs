"""한국(남한) 지도 자산 korea-z12.pmtiles 만들기 — #59 추천 지도(N8 결정 2026-11-10).

표준 라이브러리만 씀. 지도 파일은 저장소에 넣지 않고 GitHub 릴리스에 올림(app/build.gradle.kts 의 지도 받기 작업이 SHA-256 으로 확인).

고정값(바꾸면 결과 파일 · 해시가 바뀌므로 릴리스 태그도 새로):
- 원본: Protomaps basemap 일일 빌드 PROTOMAPS_BUILD(OpenStreetMap · ODbL 1.0 · Natural Earth 퍼블릭 도메인)
- 도구: go-pmtiles GO_PMTILES_VERSION 공식 릴리스(https://github.com/protomaps/go-pmtiles/releases) 의 pmtiles 실행 파일
- 범위: 아래 REGION — 남한 본토 · 연안 섬 · 제주 · 울릉 · 독도 · 서해 5도를 덮고 북한 · 중국 · 쓰시마를 빼게 직접 그린 근사 다각형
  (타일 단위로 잘리므로 경계 근처 타일은 이웃 땅이 조금 들어올 수 있음). 좌표는 이 파일 안에서 직접 정한 값 — 외부 경계 자료를 복제하지 않음.
  --check-ne 로 Natural Earth 10m admin 0 국가 경계(퍼블릭 도메인)와 대조해 남한 점이 다 들어가고 북한 · 일본 점이 빠지는지 잴 수 있음.
- 최대 확대 단계: MAX_ZOOM

쓰는 법:
  python tools/map/build_korea_pmtiles.py --pmtiles <pmtiles 실행 파일> --out <폴더>/korea-z12.pmtiles
  python tools/map/build_korea_pmtiles.py --check-ne <ne_10m_admin_0_countries.geojson>
"""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
import tempfile
from pathlib import Path

GO_PMTILES_VERSION = "1.31.2"
PROTOMAPS_BUILD = "20261109"
SOURCE_URL = f"https://build.protomaps.com/{PROTOMAPS_BUILD}.pmtiles"
MAX_ZOOM = 12

# (경도, 위도) — 바깥 고리 하나씩. 본토는 휴전선 바로 북쪽 · 쓰시마 서쪽 바다로 그음.
MAINLAND = [
    (126.05, 37.85), (126.55, 37.85), (126.70, 38.00), (126.95, 38.25), (127.10, 38.34), (127.60, 38.36),
    (128.00, 38.36), (128.30, 38.63), (128.45, 38.66), (129.00, 38.50), (129.80, 37.60),
    (129.75, 36.00), (129.55, 35.30), (129.20, 35.02), (128.85, 34.70), (128.30, 34.52),
    (127.80, 34.35), (127.40, 33.90), (126.90, 34.10), (126.00, 34.00), (125.00, 33.95),
    (124.95, 34.75), (125.60, 35.60), (125.50, 36.70), (125.90, 37.10), (125.95, 37.50),
    (126.05, 37.85),
]


def _box(west: float, south: float, east: float, north: float) -> list[tuple[float, float]]:
    return [(west, south), (east, south), (east, north), (west, north), (west, south)]


REGION = [
    MAINLAND,
    _box(124.55, 37.62, 124.98, 38.02),  # 백령 · 대청 · 소청
    _box(125.58, 37.60, 125.80, 37.72),  # 연평
    _box(126.05, 33.05, 127.05, 34.02),  # 제주 · 마라 · 추자
    _box(130.75, 37.40, 130.97, 37.58),  # 울릉
    _box(131.84, 37.22, 131.89, 37.26),  # 독도
]


def region_geojson() -> dict:
    return {
        "type": "Feature",
        "properties": {"name": "travel-crumbs south korea approx"},
        "geometry": {
            "type": "MultiPolygon",
            "coordinates": [[[list(p) for p in ring]] for ring in REGION],
        },
    }


def _inside(point: tuple[float, float], ring: list[tuple[float, float]]) -> bool:
    x, y = point
    inside = False
    for (x1, y1), (x2, y2) in zip(ring, ring[1:]):
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


def in_region(point: tuple[float, float]) -> bool:
    return any(_inside(point, ring) for ring in REGION)


def check_natural_earth(path: Path) -> int:
    data = json.loads(path.read_text(encoding="utf-8"))
    report: dict[str, tuple[int, int]] = {}
    for feature in data["features"]:
        code = feature["properties"].get("ADM0_A3")
        if code not in ("KOR", "PRK", "JPN", "CHN"):
            continue
        geom = feature["geometry"]
        polys = geom["coordinates"] if geom["type"] == "MultiPolygon" else [geom["coordinates"]]
        points = [tuple(p) for poly in polys for ring in poly for p in ring]
        # 일본 · 중국은 이 범위 근처(경도 124 ~ 132 · 위도 33 ~ 39)만 셈
        if code in ("JPN", "CHN"):
            points = [p for p in points if 124 <= p[0] <= 132 and 33 <= p[1] <= 39]
        inside = sum(1 for p in points if in_region(p))
        report[code] = (inside, len(points))
    for code, (inside, total) in sorted(report.items()):
        print(f"{code}: 범위 안 {inside} / {total} 점")
    return 0


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build(pmtiles: Path, out: Path) -> int:
    version = subprocess.run([str(pmtiles), "version"], capture_output=True, text=True, check=True).stdout
    if GO_PMTILES_VERSION not in version:
        print(f"go-pmtiles {GO_PMTILES_VERSION} 이 필요함 — 지금: {version.strip()}", file=sys.stderr)
        return 2
    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        region_file = Path(tmp) / "region.geojson"
        region_file.write_text(json.dumps(region_geojson()), encoding="utf-8")
        subprocess.run(
            [str(pmtiles), "extract", SOURCE_URL, str(out), f"--region={region_file}", f"--maxzoom={MAX_ZOOM}"],
            check=True,
        )
    print(f"file   {out}")
    print(f"bytes  {out.stat().st_size}")
    print(f"sha256 {sha256_of(out)}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--pmtiles", type=Path, help="go-pmtiles 실행 파일 경로")
    parser.add_argument("--out", type=Path, help="만들 파일(korea-z12.pmtiles)")
    parser.add_argument("--check-ne", type=Path, help="Natural Earth ne_10m_admin_0_countries.geojson 으로 범위 대조만")
    parser.add_argument("--print-region", action="store_true", help="범위 GeoJSON 만 출력")
    args = parser.parse_args()
    if args.print_region:
        print(json.dumps(region_geojson(), ensure_ascii=False))
        return 0
    if args.check_ne:
        return check_natural_earth(args.check_ne)
    if not args.pmtiles or not args.out:
        parser.print_usage(sys.stderr)
        return 2
    return build(args.pmtiles, args.out)


if __name__ == "__main__":
    sys.exit(main())
