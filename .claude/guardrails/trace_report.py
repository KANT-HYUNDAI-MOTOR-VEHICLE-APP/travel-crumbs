#!/usr/bin/env python3
"""활동 로그 요약 — trace.jsonl 을 마크다운 표로. PR 본문 · 이슈 댓글에 붙임.

사용: python3 .claude/guardrails/trace_report.py <이슈 폴더 이름 | trace.jsonl 경로> [--flow N]
"""
import collections
import json
import os
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

FLOW_DEFAULT = 30
FAIL_MAX = 20


def load(arg):
    path = arg if arg.endswith(".jsonl") else os.path.join("_workspace", arg, "trace.jsonl")
    rows = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            try:
                rows.append(json.loads(line))
            except ValueError:
                continue
    return path, rows


def seconds(ms):
    return f"{ms / 1000:.1f}s"


def report(path, rows, flow_n):
    tools = [r for r in rows if r.get("event") in ("PostToolUse", "PostToolUseFailure")]
    fails = [r for r in rows if r.get("ok") is False]
    blocked = [r for r in rows if r.get("event") == "GuardBlocked"]
    sessions = {r.get("session") for r in rows}
    out = [f"### 활동 로그 요약 — `{path}`", ""]
    if rows:
        out.append(f"- 기간: {rows[0]['ts']} → {rows[-1]['ts']} · 세션 {len(sessions)} · 도구 호출 {len(tools)} · 실패 {len(fails)} · guard 차단 {len(blocked)}")
    out += ["", "| 에이전트 | 도구 호출 | 실패 | 도구 시간 합 |", "|---|---|---|---|"]
    by = collections.defaultdict(lambda: [0, 0, 0])
    for r in tools + blocked:
        a = by[r.get("agent", "main")]
        a[0] += r.get("event") != "GuardBlocked"
        a[1] += r.get("ok") is False
        a[2] += r.get("ms", 0) or 0
    for name, (n, f, ms) in sorted(by.items(), key=lambda x: -x[1][0]):
        out.append(f"| {name} | {n} | {f} | {seconds(ms)} |")
    agents = [r for r in tools if r.get("tool") == "Agent" and r.get("totalTokens")]
    if agents:
        out += ["", "| 부른 에이전트 | 토큰 | 시간 | 도구 수 |", "|---|---|---|---|"]
        for r in agents:
            out.append(f"| {r.get('target', '')} | {r['totalTokens']} | {seconds(r.get('totalDurationMs', 0))} | {r.get('totalToolUseCount', '')} |")
    if fails:
        out += ["", "| 시각 | 에이전트 | 도구 | 대상 | 오류 |", "|---|---|---|---|---|"]
        for r in fails[:FAIL_MAX]:
            tgt = (r.get("target") or "").replace("|", "\\|")[:80]
            err = (r.get("error") or "").replace("|", "\\|")[:100]
            out.append(f"| {r['ts'][11:19]} | {r.get('agent')} | {r.get('tool', '')} | `{tgt}` | {err} |")
        if len(fails) > FAIL_MAX:
            out.append(f"| … | 나머지 {len(fails) - FAIL_MAX}건 | | | |")
    flow = [r for r in rows if r.get("event") in ("SubagentStart", "SubagentStop", "SessionStart")
            or r.get("tool") in ("Agent", "Skill", "SendMessage")]
    if flow:
        out += ["", "흐름(최근):", ""]
        for r in flow[-flow_n:]:
            what = r.get("target") or r.get("source") or ""
            out.append(f"- {r['ts'][11:19]} · {r.get('agent')} · {r.get('event')} {r.get('tool', '')} {what}".rstrip())
    return "\n".join(out)


def main():
    if len(sys.argv) < 2:
        print(__doc__.strip().splitlines()[-1], file=sys.stderr)
        return 2
    flow_n = FLOW_DEFAULT
    if "--flow" in sys.argv:
        flow_n = int(sys.argv[sys.argv.index("--flow") + 1])
    try:
        path, rows = load(sys.argv[1])
    except OSError as e:
        print(f"trace_report: 읽기 실패 — {e}", file=sys.stderr)
        return 2
    print(report(path, rows, flow_n))
    return 0


if __name__ == "__main__":
    sys.exit(main())
