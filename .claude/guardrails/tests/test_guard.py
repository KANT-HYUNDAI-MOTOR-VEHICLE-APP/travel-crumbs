"""guard.py 막기 · 통과 사례. 실행: python3 .claude/guardrails/tests/test_guard.py"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

GUARD = os.path.join(os.path.dirname(__file__), "..", "guard.py")
LOG_DIR = tempfile.mkdtemp()


def run(cmd, readonly=False, cwd=None, tool="Bash"):
    data = json.dumps({"tool_name": tool, "tool_input": {"command": cmd}, "cwd": cwd or os.getcwd()})
    args = [sys.executable, GUARD] + (["--readonly"] if readonly else [])
    env = dict(os.environ, CLAUDE_PROJECT_DIR=LOG_DIR)
    r = subprocess.run(args, input=data, capture_output=True, text=True, encoding="utf-8", env=env)
    return r.returncode


class Default(unittest.TestCase):
    BLOCK = [
        "git push origin main",
        "git push origin develop",
        "git push origin feat/x:develop",
        "git push origin --delete develop",
        "git push origin --delete login/main",
        "git push origin HEAD:main",
        "git push origin feat/x:main",
        "git push origin refs/heads/main",
        "git -C . push origin main",
        "git -c push.default=current push origin main",
        "git 'push' origin main",
        "git push origin +feat/x",
        "git push --force origin feat/x",
        "git push -f origin feat/x",
        "git push -uf origin feat/x",
        "git push origin --delete main",
        "git push origin :main",
        "git push --all origin",
        "cd server && git push origin main",
        "echo hi; git push origin main",
        "echo \"$(git push origin main)\"",
        "echo `git push origin main`",
        "bash -c 'git push origin main'",
        "FOO=1 git push origin main",
        "gh pr merge 12 --squash",
        "gh repo delete <owner>/<repo>",
        "gh api -X DELETE repos/x/y/git/refs/heads/feat",
        "gh api -X PUT repos/x/y/branches/main/protection --input -",
    ]
    PASS = [
        "git push origin feat/app-login",
        "git push -u origin login/spec",
        "git push -u origin login/impl",
        "git push -u origin bug/12-tts-silent",
        "git push -u origin feat/app-login",
        "git push --force-with-lease origin feat/app-login",
        "git push origin --delete feat/old",
        "git status && git log --oneline -3",
        "gh pr create --draft --title x --body-file y",
        "gh api repos/x/y/branches/main/protection",
        "echo main push",
        "grep -rn 'git push origin main' docs",
    ]

    def test_block(self):
        for c in self.BLOCK:
            with self.subTest(c=c):
                self.assertEqual(run(c), 2)

    def test_pass(self):
        for c in self.PASS:
            with self.subTest(c=c):
                self.assertEqual(run(c), 0)

    def test_bare_push_on_main(self):
        with tempfile.TemporaryDirectory() as d:
            subprocess.run(["git", "init", "-q", "-b", "main", d], check=True)
            self.assertEqual(run("git push", cwd=d), 2)
            subprocess.run(["git", "-C", d, "switch", "-q", "-c", "feat/x"], check=True)
            self.assertEqual(run("git push", cwd=d), 0)

    def test_feature_main(self):
        with tempfile.TemporaryDirectory() as d:
            remote, work = os.path.join(d, "remote.git"), os.path.join(d, "work")
            subprocess.run(["git", "init", "-q", "--bare", remote], check=True)
            subprocess.run(["git", "init", "-q", "-b", "develop", work], check=True)
            git = lambda *a: subprocess.run(["git", "-C", work, *a], check=True, capture_output=True)
            git("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "--allow-empty", "-m", "x")
            git("remote", "add", "origin", remote)
            # 처음 만들 땐 통과 · 원격에 생긴 뒤엔 막음
            self.assertEqual(run("git push -u origin login/main", cwd=work), 0)
            git("push", "-q", "origin", "develop:login/main")
            self.assertEqual(run("git push origin login/main", cwd=work), 2)
            self.assertEqual(run("git push origin HEAD:login/main", cwd=work), 2)
            git("switch", "-q", "-c", "login/main")
            self.assertEqual(run("git push", cwd=work), 2)
            git("switch", "-q", "-c", "login/spec")
            self.assertEqual(run("git push -u origin login/spec", cwd=work), 0)

    def test_issue_gate(self):
        with tempfile.TemporaryDirectory() as d:
            subprocess.run(["git", "init", "-q", "-b", "develop", d], check=True)
            sub = os.path.join(d, "app")
            os.makedirs(sub)
            create = [
                "git switch -c login/main origin/develop",
                "git checkout -b login/spec",
                "git switch -C login/impl",
                "git switch --create=login/sync",
                "git branch login/spec",
                "git branch -m old login/impl",
                "git worktree add -b login/impl ../wt",
                "git -C . switch -c bug/9-tts-silent",
                "git switch -c harness/9-log-rule origin/develop",
                "cd app && git switch -c login/spec",
            ]
            free = [
                "git switch -c harness/issue-gate",
                "git switch -c docs/PLAN-003",
                "git switch -c scratch",
                "git switch develop",
                "git checkout login/spec",
                "git branch -D login/spec",
                "git branch --show-current",
                "git branch -a",
            ]
            # 이슈 폴더 없음 → 막음
            for c in create:
                with self.subTest(c=c, state="없음"):
                    self.assertEqual(run(c, cwd=d), 2)
            for c in free:
                with self.subTest(c=c, state="없음"):
                    self.assertEqual(run(c, cwd=d), 0)
            # current 만 있고 state.md 없음 → 막음
            ws = os.path.join(d, "_workspace")
            os.makedirs(os.path.join(ws, "9"))
            with open(os.path.join(ws, "current"), "w", encoding="utf-8") as f:
                f.write("9\n")
            self.assertEqual(run("git switch -c login/spec", cwd=d), 2)
            # 0단계를 거침 → 통과
            with open(os.path.join(ws, "9", "state.md"), "w", encoding="utf-8") as f:
                f.write("# 9\n")
            for c in create:
                with self.subTest(c=c, state="있음"):
                    self.assertEqual(run(c, cwd=d), 0)
            # bug 브랜치 번호가 지금 이슈와 다르면 막음
            self.assertEqual(run("git switch -c bug/12-other", cwd=d), 2)
            self.assertEqual(run("git switch -c harness/12-other", cwd=d), 2)
            # 보관 폴더 이름(9_20261031-1200)도 같은 이슈로 봄
            os.makedirs(os.path.join(ws, "9_20261031-1200"))
            with open(os.path.join(ws, "9_20261031-1200", "state.md"), "w", encoding="utf-8") as f:
                f.write("# 9\n")
            with open(os.path.join(ws, "current"), "w", encoding="utf-8") as f:
                f.write("9_20261031-1200\n")
            self.assertEqual(run("git switch -c bug/9-tts-silent", cwd=d), 0)
            self.assertEqual(run("git switch -c bug/91-other", cwd=d), 2)

    def test_powershell(self):
        self.assertEqual(run("git push origin main", tool="PowerShell"), 2)


class Readonly(unittest.TestCase):
    BLOCK = [
        "echo x > a.txt",
        "cat a >> b",
        "git diff | tee out.txt",
        "rm -rf build",
        "mv a b",
        "sed -i '' 's/a/b/' f.kt",
        "git commit -m x",
        "git add .",
        "git switch -c x",
        "git stash",
        "gh pr comment 3 --body x",
        "gh issue create --title x",
        "gh api -X PATCH repos/x/y",
        "python3 -c 'open(\"a\",\"w\")'",
        "python3 tools/do_something.py",
        "find . -name '*.tmp' -delete",
        "xargs rm < list.txt",
        "./gradlew assembleDebug",
        "touch a",
    ]
    PASS = [
        "git diff origin/main...HEAD --stat",
        "git log --oneline -5 2>/dev/null",
        "git show HEAD:app/build.gradle.kts | head -40",
        "grep -rn 'CarUxRestrictions' app/src | wc -l",
        "python3 .claude/skills/sensors/scripts/check_manifest.py app/src/main/AndroidManifest.xml",
        "uv run .claude/skills/sensors/scripts/check_routes.py server/app",
        "gh pr view 5 --json files",
        "gh api repos/x/y/pulls/5/files",
        "find app -name '*.kt' | xargs grep -n Vehicle",
        "sed -n '1,40p' app/src/main/AndroidManifest.xml",
        "cd app && ls",
        "git branch --show-current",
    ]

    def test_block(self):
        for c in self.BLOCK:
            with self.subTest(c=c):
                self.assertEqual(run(c, readonly=True), 2)

    def test_pass(self):
        for c in self.PASS:
            with self.subTest(c=c):
                self.assertEqual(run(c, readonly=True), 0)


if __name__ == "__main__":
    unittest.main()
