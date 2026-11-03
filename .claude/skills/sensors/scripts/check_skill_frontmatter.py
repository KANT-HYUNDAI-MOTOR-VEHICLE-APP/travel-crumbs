#!/usr/bin/env python3
"""스킬 · 에이전트 frontmatter 검사.

재는 것:
- `---` 로 열고 닫는가 · `name` · `description` 이 있는가
- description 이 YAML 에서 잘리지 않는가 — 따옴표 없는 한 줄 값에 ` #`(주석 시작) 또는
  `: `(키 구분자)가 있으면 그 앞에서 잘려 트리거 문구가 실리지 않음.
  `「이슈 #n 진행해줘」` 처럼 트리거 예시에 `#` 를 쓰면 생기는 일이라 기계로 막음.

고치는 법: 그 줄을 작은따옴표로 감싸거나(`description: '...'`) 블록 스칼라(`>-`)로 바꾼다.
"""
import glob
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
TARGETS = [".claude/skills/*/SKILL.md", ".claude/agents/*.md"]


def quoted(v):
    return len(v) >= 2 and ((v[0] == "'" and v[-1] == "'") or (v[0] == '"' and v[-1] == '"'))


def check(path):
    rel = os.path.relpath(path, ROOT).replace("\\", "/")
    out = []
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    if not text.startswith("---\n"):
        return [f"{rel}: frontmatter 가 `---` 로 시작하지 않음"]
    try:
        end = text.index("\n---\n", 3)
    except ValueError:
        return [f"{rel}: frontmatter 를 닫는 `---` 없음"]
    lines = text[4:end + 1].splitlines()
    keys = [ln.split(":", 1)[0] for ln in lines if ln and not ln.startswith((" ", "\t", "#"))]
    for need in ("name", "description"):
        if need not in keys:
            out.append(f"{rel}: frontmatter 에 `{need}` 없음")
    for ln in lines:
        if not ln.startswith("description:"):
            continue
        value = ln.split("description:", 1)[1].strip()
        if not value or value.startswith((">", "|")):
            break  # 블록 스칼라는 잘리지 않음
        if quoted(value):
            break
        if " #" in value:
            out.append(f"{rel}: description 이 ` #` 에서 잘림(YAML 주석) — 작은따옴표로 감싸기")
        elif ": " in value:
            out.append(f"{rel}: description 이 `: ` 에서 잘림(YAML 키 구분자) — 작은따옴표로 감싸기")
        break
    return out


def main():
    files, bad = [], []
    for pattern in TARGETS:
        files += sorted(glob.glob(os.path.join(ROOT, pattern)))
    for path in files:
        bad += check(path)
    for line in bad:
        print("  " + line)
    print(f"check_skill_frontmatter: 파일 {len(files)}개 · 위반 {len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
