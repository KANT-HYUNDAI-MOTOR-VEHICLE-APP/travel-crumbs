"""여행 종료(A-08) 경로 계산 — 거리 · 간소화 경로(인코딩 폴리라인). DB 를 모르는 순수 함수."""

import math

Point = tuple[float, float]  # (위도, 경도)

EARTH_RADIUS_M = 6_371_000.0
# 간소화 허용 오차(미터) — 지도에 그릴 때 눈에 띄지 않는 정도
SIMPLIFY_TOLERANCE_M = 10.0


def distance_m(a: Point, b: Point) -> float:
    """두 점 사이 거리(하버사인 · 미터)."""
    lat1, lng1, lat2, lng2 = map(math.radians, (*a, *b))
    h = (
        math.sin((lat2 - lat1) / 2) ** 2
        + math.cos(lat1) * math.cos(lat2) * math.sin((lng2 - lng1) / 2) ** 2
    )
    return 2 * EARTH_RADIUS_M * math.asin(math.sqrt(h))


def total_distance_m(points: list[Point]) -> int:
    return round(
        sum(distance_m(a, b) for a, b in zip(points, points[1:], strict=False))
    )


def _offset_m(p: Point, a: Point, b: Point) -> float:
    """점 p 와 선분 a-b 사이 거리(미터) — 짧은 구간이라 평면 근사."""
    k = math.cos(math.radians(a[0]))
    ax, ay = a[1] * k, a[0]
    bx, by = b[1] * k, b[0]
    px, py = p[1] * k, p[0]
    dx, dy = bx - ax, by - ay
    if dx == 0 and dy == 0:
        return distance_m(p, a)
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
    return distance_m(p, (ay + t * dy, (ax + t * dx) / k))


def simplify(
    points: list[Point], tolerance_m: float = SIMPLIFY_TOLERANCE_M
) -> list[Point]:
    """더글러스-포이커 — 모양을 유지하며 점 수를 줄임."""
    if len(points) < 3:
        return list(points)
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        start, end = stack.pop()
        best, index = 0.0, -1
        for i in range(start + 1, end):
            d = _offset_m(points[i], points[start], points[end])
            if d > best:
                best, index = d, i
        if index != -1 and best > tolerance_m:
            keep[index] = True
            stack += [(start, index), (index, end)]
    return [p for p, k in zip(points, keep, strict=True) if k]


def encode_polyline(points: list[Point]) -> str:
    """구글 인코딩 폴리라인(정밀도 1e-5)."""
    out: list[str] = []
    prev_lat = prev_lng = 0
    for lat, lng in points:
        ilat, ilng = round(lat * 1e5), round(lng * 1e5)
        for delta in (ilat - prev_lat, ilng - prev_lng):
            value = ~(delta << 1) if delta < 0 else delta << 1
            while value >= 0x20:
                out.append(chr((0x20 | (value & 0x1F)) + 63))
                value >>= 5
            out.append(chr(value + 63))
        prev_lat, prev_lng = ilat, ilng
    return "".join(out)
