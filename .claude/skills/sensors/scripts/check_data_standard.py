#!/usr/bin/env python3
"""데이터 표준 대조 — 테이블 · 컬럼 이름이 docs/data/standard.md 의 사전과 규칙에 맞나.

재는 규칙(정의서 1 · 2절):
  T02 유형 단어   테이블명 끝이 _mst · _dtl · _hist · _log · _rel
  R02 기본키      PK 이름 = 테이블명 − 유형 단어 + _no (이력 · 로그는 유형 단어 유지)
  R01 표기 방식   테이블 · 컬럼은 소문자 snake_case
  R06-1 단어 등록 컬럼이 표준 용어 사전에 있음
  R07 공통 컬럼   created_at · created_user_no · updated_at · updated_user_no · deleted_yn
                 (로그 테이블은 created_at · created_user_no 둘만)

심각도는 정의서가 정함 — 상태가 「결정」인 규칙만 위반(종료 코드 1)이고 「제안 · 미결 · 가안」은
경고로만 찍음. 제안인 채로 빌드를 막으면 정의서와 센서가 어긋나기 때문. 정의서에서 상태를
「결정」으로 올리면 이 스크립트를 고치지 않아도 위반으로 바뀜.
"""
import argparse
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

PY_TABLE = re.compile(r"""__tablename__\s*=\s*["']([^"']+)["']""")
PY_CLASS = re.compile(r"^class\s+(\w+)")
PY_COL = re.compile(r"^\s{1,}(\w+)\s*(?::\s*Mapped\[[^\]]*\]\s*)?=\s*(?:mapped_column|Column)\(")
KT_TABLE = re.compile(r"""@Entity\s*\([^)]*tableName\s*=\s*["']([^"']+)["']""")
KT_COL = re.compile(r"""@ColumnInfo\s*\([^)]*name\s*=\s*["']([^"']+)["']""")
SNAKE = re.compile(r"^[a-z][a-z0-9_]*$")
RULE_ID = re.compile(r"^[RT]\d{2}(-\d+)?$")

COMMON = ("created_at", "created_user_no", "updated_at", "updated_user_no", "deleted_yn")
COMMON_LOG = ("created_at", "created_user_no")


def rows(lines):
    """마크다운 표의 데이터 행을 셀 목록으로."""
    for i, line in enumerate(lines):
        s = line.strip()
        if not s.startswith("|") or set(s) <= set("|-: "):
            continue
        yield i, [c.strip() for c in s.strip("|").split("|")]


def section(text, needle):
    """제목에 needle 이 든 절의 본문 줄들."""
    out, inside, level = [], False, 0
    for line in text.split("\n"):
        m = re.match(r"^(#{2,3})\s+(.*)$", line)
        if m:
            if inside and len(m.group(1)) <= level:
                break
            if needle in m.group(2):
                inside, level = True, len(m.group(1))
                continue
        if inside:
            out.append(line)
    return out


def load_standard(path):
    try:
        text = open(path, encoding="utf-8").read()
    except OSError as e:
        print(f"{path}:0 · 입력 · 열지 못함: {e}")
        return None
    std = {"status": {}, "words": set(), "terms": set(), "types": []}

    for needle in ("명명 규칙", "테이블 규칙"):
        for _, cells in rows(section(text, needle)):
            if len(cells) >= 5 and RULE_ID.match(cells[0]):
                std["status"][cells[0]] = cells[-1]

    for _, cells in rows(section(text, "표준 단어 사전")):
        if len(cells) >= 3 and SNAKE.match(cells[2].lower()):
            std["words"].add(cells[2].lower())

    for _, cells in rows(section(text, "표준 용어 사전")):
        if len(cells) >= 2 and SNAKE.match(cells[1].lower()):
            std["terms"].add(cells[1].lower())

    for _, cells in rows(section(text, "테이블 유형 정의")):
        if len(cells) >= 2 and SNAKE.match(cells[1].lower()):
            std["types"].append(cells[1].lower())

    missing = [k for k in ("words", "terms", "types") if not std[k]]
    if missing:
        print(f"{path}:0 · 입력 · 사전을 읽지 못함({' · '.join(missing)}) — 절 제목이 바뀌었는지 확인")
        return None
    return std


def collect(paths):
    """{테이블명: {"file","line","cols":[(이름, 줄)], "pk":[이름]}}"""
    tables, skipped = {}, []
    for root in paths:
        if not os.path.isdir(root):
            skipped.append(root)
            continue
        for d, _, fs in os.walk(root):
            for f in fs:
                if not f.endswith((".py", ".kt")):
                    continue
                p = os.path.join(d, f).replace("\\", "/")  # 두 OS 에서 같은 줄이 나오게
                try:
                    lines = open(p, encoding="utf-8").read().split("\n")
                except OSError:
                    continue
                cur = None
                for i, line in enumerate(lines, 1):
                    if f.endswith(".py") and PY_CLASS.match(line):
                        cur = None
                    m = PY_TABLE.search(line) if f.endswith(".py") else KT_TABLE.search(line)
                    if m:
                        cur = m.group(1)
                        tables.setdefault(cur, {"file": p, "line": i, "cols": [], "pk": []})
                        continue
                    if cur is None:
                        continue
                    if f.endswith(".py"):
                        c = PY_COL.match(line)
                        if c and not c.group(1).startswith("__"):
                            tables[cur]["cols"].append((c.group(1), i))
                            if "primary_key=True" in line.replace(" ", ""):
                                tables[cur]["pk"].append(c.group(1))
                    else:
                        c = KT_COL.search(line)
                        if c:
                            tables[cur]["cols"].append((c.group(1), i))
    return tables, skipped


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="*", default=["server/app", "app/src/main/java"],
                    help="훑을 폴더(기본: server/app · app/src/main/java) — 없는 폴더는 건너뜀")
    ap.add_argument("--standard", default="docs/data/standard.md", help="데이터 표준 정의서(최신판)")
    a = ap.parse_args()

    std = load_standard(a.standard)
    if std is None:
        return 2

    def report(rule, where, msg, out):
        decided = std["status"].get(rule, "").startswith("결정")
        out[0 if decided else 1].append(f"{where} · {rule} · {msg}")

    bad, warn = [], []
    out = (bad, warn)
    tables, skipped = collect(a.paths or ["server/app", "app/src/main/java"])

    for name, t in sorted(tables.items()):
        where = f"{t['file']}:{t['line']}"
        suffix = next((s for s in std["types"] if name.endswith("_" + s)), None)
        if suffix is None:
            report("T02", where, f"테이블 {name} — 끝에 유형 단어 없음({' · '.join('_' + s for s in std['types'])})", out)
        if not SNAKE.match(name):
            report("R01", where, f"테이블 {name} — 소문자 snake_case 아님", out)

        names = [c for c, _ in t["cols"]]
        if suffix and t["pk"]:
            stem = name if suffix in ("hist", "log") else name[: -(len(suffix) + 1)]
            want = stem + "_no"
            if t["pk"] != [want]:
                report("R02", where, f"테이블 {name} — PK 는 {want} 여야 함(지금 {' · '.join(t['pk']) or '없음'})", out)

        for col, line in t["cols"]:
            if not SNAKE.match(col):
                report("R01", f"{t['file']}:{line}", f"컬럼 {col} — 소문자 snake_case 아님", out)
            elif col not in std["terms"]:
                report("R06-1", f"{t['file']}:{line}",
                       f"컬럼 {col} — 표준 용어 사전에 없음(정의서에 먼저 등록)", out)

        if names:
            need = COMMON_LOG if suffix == "log" else COMMON
            miss = [c for c in need if c not in names]
            if miss:
                report("R07", where, f"테이블 {name} — 공통 컬럼 없음: {' · '.join(miss)}", out)

    for b in bad:
        print(b)
    for w in warn:
        print(f"{w} [경고 · 정의서 상태 「{std['status'].get(w.split(' · ')[1], '미등록')}」]")
    if skipped:
        print(f"check_data_standard: 건너뜀 {' · '.join(skipped)}(폴더 없음)")
    print(f"check_data_standard: 테이블 {len(tables)}개 · 용어 {len(std['terms'])}개 · 위반 {len(bad)} · 경고 {len(warn)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
