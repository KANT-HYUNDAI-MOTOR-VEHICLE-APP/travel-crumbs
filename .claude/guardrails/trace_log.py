#!/usr/bin/env python3
"""활동 로그 — 훅 입력(stdin JSON) 한 건을 JSONL 한 줄로 남김. 실패해도 작업을 막지 않음(항상 종료 코드 0).

위치: _workspace/<_workspace/current 의 폴더>/trace.jsonl · current 가 없으면 _workspace/_sessions/<날짜>.jsonl
남기지 않음: 도구 결과 본문 · 파일 내용. 비밀값은 check_secrets.py 패턴으로 가림.
"""
import datetime
import importlib.util
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TARGET_MAX = 200
GENERIC_SECRET = re.compile(r"(?i)((?:token|secret|password|passwd|api[_-]?key|authorization)\s*[=:]\s*)(\S+)")


def secret_patterns():
    path = os.path.join(HERE, "..", "skills", "sensors", "scripts", "check_secrets.py")
    try:
        spec = importlib.util.spec_from_file_location("check_secrets", path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        return [p for p, _ in mod.VALUE_PATTERNS]
    except Exception:
        return []


def redact(text):
    if not text:
        return text
    for p in secret_patterns():
        text = p.sub(lambda m: m.group(0)[:4] + "***", text)
    return GENERIC_SECRET.sub(lambda m: m.group(1) + "***", text)


def project_dir(data):
    return os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or os.getcwd()


def log_path(proj):
    ws = os.path.join(proj, "_workspace")
    try:
        with open(os.path.join(ws, "current"), encoding="utf-8") as f:
            cur = f.read().strip()
        if re.fullmatch(r"[\w.-]+", cur):
            return os.path.join(ws, cur, "trace.jsonl")
    except OSError:
        pass
    return os.path.join(ws, "_sessions", datetime.date.today().isoformat() + ".jsonl")


def rel(path, proj):
    if not path:
        return ""
    p = path.replace("\\", "/")
    root = proj.replace("\\", "/").rstrip("/") + "/"
    return p[len(root):] if p.startswith(root) else p


def target(tool, inp, proj):
    inp = inp or {}
    if tool in ("Bash", "PowerShell"):
        return inp.get("command", "")
    if tool in ("Read", "Edit", "Write", "NotebookEdit"):
        return rel(inp.get("file_path") or inp.get("notebook_path"), proj)
    if tool in ("Grep", "Glob"):
        return f"{inp.get('pattern', '')} @ {rel(inp.get('path'), proj) or '.'}"
    if tool == "Agent":
        return " · ".join(x for x in (inp.get("subagent_type"), inp.get("name"), inp.get("description")) if x)
    if tool == "Skill":
        return inp.get("skill", "")
    if tool == "SendMessage":
        return f"→ {inp.get('to', '')}"
    if tool == "WebFetch":
        return inp.get("url", "")
    return ""


def record(data):
    proj = project_dir(data)
    event = data.get("hook_event_name", "")
    tool = data.get("tool_name", "")
    rec = {
        "ts": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "session": (data.get("session_id") or "")[:8],
        "event": event,
        "agent": data.get("agent_type") or "main",
        "agent_id": (data.get("agent_id") or "")[:8],
    }
    if tool:
        rec["tool"] = tool
        rec["target"] = redact(target(tool, data.get("tool_input"), proj))[:TARGET_MAX]
    if "duration_ms" in data:
        rec["ms"] = data["duration_ms"]
    if event == "PostToolUse":
        rec["ok"] = True
        resp = data.get("tool_response")
        if tool == "Agent" and isinstance(resp, dict):
            for k in ("totalTokens", "totalDurationMs", "totalToolUseCount", "status"):
                if k in resp:
                    rec[k] = resp[k]
    elif event == "PostToolUseFailure":
        rec["ok"] = False
        rec["error"] = redact((data.get("error") or "").splitlines()[0] if data.get("error") else "")[:TARGET_MAX]
    elif event == "SubagentStop":
        rec["last"] = redact((data.get("last_assistant_message") or "")[:TARGET_MAX])
    elif event == "SessionStart":
        rec["source"] = data.get("source", "")
    elif event == "GuardBlocked":
        rec["ok"] = False
        rec["error"] = data.get("reason", "")
    return proj, rec


def write(data):
    proj, rec = record(data)
    path = log_path(proj)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "a", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(rec, ensure_ascii=False) + "\n")


def main():
    try:
        write(json.load(sys.stdin))
    except Exception:
        pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
