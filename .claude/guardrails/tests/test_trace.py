"""trace_log.py · trace_report.py 사례. 실행: python3 .claude/guardrails/tests/test_trace.py"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

HOOKS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")


def send(proj, data):
    env = dict(os.environ, CLAUDE_PROJECT_DIR=proj)
    r = subprocess.run([sys.executable, os.path.join(HOOKS, "trace_log.py")], input=json.dumps(data),
                       capture_output=True, text=True, encoding="utf-8", env=env)
    return r.returncode


class Trace(unittest.TestCase):
    def setUp(self):
        self.proj = tempfile.mkdtemp()
        os.makedirs(os.path.join(self.proj, "_workspace"))
        with open(os.path.join(self.proj, "_workspace", "current"), "w") as f:
            f.write("7-login\n")
        self.log = os.path.join(self.proj, "_workspace", "7-login", "trace.jsonl")

    def rows(self):
        with open(self.log, encoding="utf-8") as f:
            return [json.loads(l) for l in f]

    def test_subagent_tool_call(self):
        send(self.proj, {"hook_event_name": "PostToolUse", "session_id": "abcdef123456", "agent_type": "builder",
                         "agent_id": "a1b2c3d4e5", "tool_name": "Edit", "duration_ms": 12,
                         "tool_input": {"file_path": os.path.join(self.proj, "app", "Main.kt")},
                         "tool_response": {"content": "SHOULD NOT BE LOGGED"}})
        r = self.rows()[0]
        self.assertEqual((r["agent"], r["tool"], r["target"], r["ok"], r["ms"]), ("builder", "Edit", "app/Main.kt", True, 12))
        self.assertNotIn("SHOULD NOT BE LOGGED", json.dumps(r))

    def test_secret_redacted(self):
        send(self.proj, {"hook_event_name": "PostToolUseFailure", "tool_name": "Bash",
                         "tool_input": {"command": "curl -H 'Authorization: token ghp_" + "a" * 36 + "' x"},
                         "error": "Exit code 1\nmore"})
        r = self.rows()[0]
        self.assertNotIn("a" * 36, r["target"])
        self.assertEqual((r["agent"], r["ok"], r["error"]), ("main", False, "Exit code 1"))

    def test_no_current_goes_to_sessions(self):
        os.remove(os.path.join(self.proj, "_workspace", "current"))
        send(self.proj, {"hook_event_name": "SessionStart", "source": "startup"})
        files = os.listdir(os.path.join(self.proj, "_workspace", "_sessions"))
        self.assertEqual(len(files), 1)

    def test_bad_input_never_fails(self):
        env = dict(os.environ, CLAUDE_PROJECT_DIR=self.proj)
        r = subprocess.run([sys.executable, os.path.join(HOOKS, "trace_log.py")], input="not json",
                           capture_output=True, text=True, encoding="utf-8", env=env)
        self.assertEqual(r.returncode, 0)

    def test_report(self):
        send(self.proj, {"hook_event_name": "PostToolUse", "tool_name": "Agent", "agent_type": "",
                         "tool_input": {"subagent_type": "pleos-reviewer"},
                         "tool_response": {"totalTokens": 5000, "totalDurationMs": 4200, "totalToolUseCount": 9}})
        send(self.proj, {"hook_event_name": "PostToolUseFailure", "tool_name": "Bash", "agent_type": "builder",
                         "tool_input": {"command": "./gradlew build"}, "error": "Exit code 1"})
        r = subprocess.run([sys.executable, os.path.join(HOOKS, "trace_report.py"), self.log],
                           capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(r.returncode, 0)
        self.assertIn("| builder | 1 | 1 |", r.stdout)
        self.assertIn("pleos-reviewer | 5000", r.stdout)


if __name__ == "__main__":
    unittest.main()
