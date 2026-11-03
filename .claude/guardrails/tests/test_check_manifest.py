"""check_manifest.py 막기 · 통과 사례. 실행: python .claude/guardrails/tests/test_check_manifest.py

재는 것: 허용 목록(ALLOWED)이 근거를 갖추었는가 · 허용 밖 권한을 잡는가 ·
금지 권한(백그라운드 위치)은 예외 없이 빨강인가 · --allow 가 임시로만 듣는가.
규칙 원본은 `sensors` 스킬 「권한 허용 목록을 늘리는 길」.
"""
import os
import subprocess
import sys
import tempfile
import unittest

sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "..", "..", "skills", "sensors", "scripts", "check_manifest.py")

HEAD = '<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
TAIL = """  <uses-sdk android:minSdkVersion="28" />
  <application android:allowBackup="false" android:networkSecurityConfig="@xml/nsc">
    <activity android:name=".MainActivity" android:exported="true">
      <intent-filter><category android:name="android.intent.category.LAUNCHER" /></intent-filter>
    </activity>
  </application>
</manifest>
"""


def manifest(perms):
    lines = [HEAD]
    lines += [f'  <uses-permission android:name="{p}" />' for p in perms]
    lines.append(TAIL)
    fd, path = tempfile.mkstemp(suffix=".xml")
    with os.fdopen(fd, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    return path


def run(perms, allow=()):
    path = manifest(perms)
    try:
        args = [sys.executable, SCRIPT, path]
        for a in allow:
            args += ["--allow", a]
        return subprocess.run(args, capture_output=True, text=True, encoding="utf-8").returncode
    finally:
        os.unlink(path)


def allowed_table():
    ns = {}
    with open(SCRIPT, encoding="utf-8") as fh:
        src = fh.read()
    exec(compile(src.split("def attr(")[0], SCRIPT, "exec"), ns)
    return ns["ALLOWED"]


class Allowlist(unittest.TestCase):
    def test_every_entry_has_a_reason(self):
        """목록의 모든 줄에 근거가 있어야 함 — 없으면 다음 사람이 지울지 말지 못 정함."""
        for perm, reason in allowed_table().items():
            with self.subTest(perm=perm):
                self.assertTrue(isinstance(reason, str) and reason.strip(), f"{perm} 에 근거 없음")

    def test_allowed_permissions_pass(self):
        self.assertEqual(run(list(allowed_table())), 0)


class Blocked(unittest.TestCase):
    def test_permission_outside_the_list_fails(self):
        self.assertEqual(run(["android.permission.CAMERA", "android.permission.INTERNET"]), 1)

    def test_background_location_fails(self):
        """허용 목록에 넣더라도 막혀야 하는 권한."""
        self.assertEqual(run(["android.permission.ACCESS_BACKGROUND_LOCATION",
                              "android.permission.INTERNET"]), 1)

    def test_background_location_fails_even_with_allow(self):
        self.assertEqual(run(["android.permission.ACCESS_BACKGROUND_LOCATION",
                              "android.permission.INTERNET"],
                             allow=["android.permission.ACCESS_BACKGROUND_LOCATION"]), 1)


class TemporaryAllow(unittest.TestCase):
    def test_allow_lets_an_unlisted_permission_through(self):
        """--allow 는 스펙 승인 전 손으로 확인할 때만 — 커밋 · CI 명령에는 쓰지 않음."""
        self.assertEqual(run(["android.permission.CAMERA", "android.permission.INTERNET"],
                             allow=["android.permission.CAMERA"]), 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
