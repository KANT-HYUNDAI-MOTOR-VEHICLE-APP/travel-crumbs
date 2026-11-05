#!/usr/bin/env python3
"""Bash · PowerShell 명령 검사 — PreToolUse 훅. 막으면 종료 코드 2 + 이유(stderr).

기본: main · develop 푸시 · 이미 있는 <기능명>/main 푸시 · 강제 푸시 · 머지 · 저장소/브랜치 삭제 · 보호 설정 변경을 막음(deny 규칙이 못 잡는 다른 꼴 포함).
      이슈 게이트 — 이슈 작업 폴더(_workspace/current → state.md) 없이 이슈 작업 브랜치(<기능명>/main·spec·impl·sync · bug/<n>-… · harness/<n>-…)를 만드는 명령을 막음.
--readonly: 리뷰어용. 읽기 · 검사 명령만 통과(허용 목록).

검사 중 예외가 나도 통과시키지 않음(fail-closed) — Claude Code 는 종료 코드 2 만 차단으로 읽고
나머지는 「비차단 오류」로 보아 명령을 그대로 실행하기 때문.

입력: 훅 JSON(stdin) — tool_name · tool_input.command · cwd.
"""
import json
import os
import re
import shlex
import subprocess
import sys

# 윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 한글 출력이 터지지 않게 — 두 OS 에서 같은 명령이 돌아야 함
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

PROTECTED = {"main", "develop"}
FEATURE_MAIN = re.compile(r"[a-z0-9][a-z0-9-]*/main")
ISSUE_BRANCH = re.compile(r"(?:bug|harness)/(\d+)-[a-z0-9-]+|[a-z0-9][a-z0-9-]*/(?:main|spec|impl|sync)")
NO_ISSUE_PREFIX = ("docs/",)  # 이슈 없이 따는 브랜치 · 번호 없는 harness/<이름> 도 통과(환경 세팅 기간)
SEPARATORS = {"&&", "||", ";", "|", "|&", "&", "(", ")", "\n"}
WRAPPERS = {"sudo", "env", "command", "nohup", "time", "exec", "builtin", "nice"}
SHELLS = {"sh", "bash", "zsh", "pwsh", "powershell"}
GIT_OPTS_WITH_VALUE = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--exec-path"}

# 리뷰어 허용 목록
READ_CMDS = {
    "cat", "head", "tail", "grep", "egrep", "rg", "wc", "ls", "pwd", "cd", "sort", "uniq", "cut", "tr",
    "diff", "jq", "echo", "printf", "test", "[", "true", "false", "file", "stat", "basename", "dirname",
    "realpath", "shasum", "sha256sum", "md5", "md5sum", "tree", "column", "nl", "comm", "which", "date",
    "awk", "sed", "find", "xargs", "less", "more", "type", "Get-Content", "Select-String", "Get-ChildItem",
}
GIT_READ = {"diff", "log", "show", "status", "blame", "ls-files", "ls-tree", "rev-parse", "grep", "merge-base",
            "cat-file", "describe", "shortlog", "rev-list", "name-rev", "whatchanged", "fetch"}
GH_READ = {("pr", "view"), ("pr", "diff"), ("pr", "list"), ("pr", "checks"), ("pr", "status"),
           ("issue", "view"), ("issue", "list"), ("run", "view"), ("run", "list"), ("repo", "view")}
READ_SCRIPT_DIRS = (".claude/skills/sensors/scripts/", ".claude/guardrails/trace_report.py")


class Blocked(Exception):
    pass


def tokenize(cmd):
    cmd = cmd.replace("`", " ; ")
    lex = shlex.shlex(cmd, posix=True, punctuation_chars=True)
    lex.whitespace_split = True
    lex.commenters = ""
    return list(lex)


def split_commands(tokens):
    cur, out = [], []
    for t in tokens:
        if t in SEPARATORS or t == "$":
            if cur:
                out.append(cur)
            cur = []
        else:
            cur.append(t)
    if cur:
        out.append(cur)
    return out


def strip_prefix(argv):
    i = 0
    while i < len(argv) and (re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", argv[i]) or argv[i] in WRAPPERS):
        i += 1
    return argv[i:]


def nested(argv):
    """sh -c '…' · eval '…' · "$(…)" 안의 명령 문자열."""
    inner = []
    name = os.path.basename(argv[0]).lower() if argv else ""
    if name.replace(".exe", "") in SHELLS:
        for j, a in enumerate(argv[1:], 1):
            if a in ("-c", "-Command", "-command") and j + 1 < len(argv):
                inner.append(argv[j + 1])
    if name == "eval":
        inner.append(" ".join(argv[1:]))
    for a in argv:
        for m in re.finditer(r"\$\((.*)\)", a):
            inner.append(m.group(1))
    return inner


def git_sub(argv):
    """git 전역 옵션을 건너뛴 (하위 명령, 나머지 인자, -C 경로)."""
    i, cpath = 1, None
    while i < len(argv):
        a = argv[i]
        if a in GIT_OPTS_WITH_VALUE:
            if a == "-C" and i + 1 < len(argv):
                cpath = argv[i + 1]
            i += 2
            continue
        if a.startswith("--") and "=" in a or a in ("--no-pager", "-p", "--paginate", "--bare", "--no-replace-objects"):
            i += 1
            continue
        return a, argv[i + 1:], cpath
    return None, [], cpath


def current_branch(cwd):
    try:
        r = subprocess.run(["git", "symbolic-ref", "--short", "HEAD"], cwd=cwd or None,
                           capture_output=True, text=True, timeout=5)
        return r.stdout.strip()
    except (OSError, subprocess.TimeoutExpired):
        return ""


def remote_has(remote, branch, cwd):
    try:
        r = subprocess.run(["git", "ls-remote", "--exit-code", "--heads", remote, branch], cwd=cwd or None,
                           capture_output=True, text=True, timeout=8)
        return r.returncode == 0
    except (OSError, subprocess.TimeoutExpired):
        return True


def check_dest(dst, remote, cwd, deleting, label):
    if dst in PROTECTED:
        raise Blocked(f"{dst} 직접 푸시 · 삭제 금지({label}) — 작업 브랜치 → PR → 사람이 머지")
    if FEATURE_MAIN.fullmatch(dst):
        if deleting:
            raise Blocked(f"{dst} 삭제 금지 — develop 머지 때 사람이 지움")
        if remote_has(remote, dst, cwd):
            raise Blocked(f"{dst} 는 이미 있음 — 직접 푸시 금지 · <기능명>/spec · <기능명>/impl → PR 로만")


def check_push(args, cwd):
    flags = [a for a in args if a.startswith("-")]
    pos = [a for a in args if not a.startswith("-")]
    if any(f in ("-f", "--force") or re.fullmatch(r"-[a-zA-Z]*f[a-zA-Z]*", f) for f in flags):
        raise Blocked("강제 푸시 금지 — rebase 뒤엔 --force-with-lease 만")
    if any(f in ("--all", "--mirror") for f in flags):
        raise Blocked("--all · --mirror 푸시 금지 — main 이 함께 나갈 수 있음")
    deleting = any(f in ("-d", "--delete") for f in flags)
    remote = pos[0] if pos else "origin"
    refspecs = pos[1:]
    for r in refspecs:
        if r.startswith("+"):
            raise Blocked(f"강제 푸시 금지(+ refspec: {r})")
        dst = r.split(":", 1)[1] if ":" in r else r
        if dst == "HEAD":
            dst = current_branch(cwd)
        check_dest(dst.removeprefix("refs/heads/"), remote, cwd, deleting or r.startswith(":"), r)
    if deleting and not refspecs:
        return
    if not refspecs:
        branch = current_branch(cwd)
        if branch:
            check_dest(branch, remote, cwd, False, f"지금 브랜치 {branch} · 인자 없는 git push")


def new_branch_name(sub, rest):
    """브랜치를 새로 만드는 git 명령이면 그 이름 · 아니면 None."""
    if sub in ("switch", "checkout"):
        for i, a in enumerate(rest):
            if a in ("-c", "-C", "-b", "-B", "--create", "--force-create") and i + 1 < len(rest):
                return rest[i + 1]
            m = re.fullmatch(r"--(?:create|force-create)=(.+)", a)
            if m:
                return m.group(1)
        return None
    if sub == "worktree" and rest[:1] == ["add"]:
        for i, a in enumerate(rest):
            if a in ("-b", "-B") and i + 1 < len(rest):
                return rest[i + 1]
        return None
    if sub == "branch":
        if any(a in ("-d", "-D", "--delete", "--list", "-l", "-a", "-r", "--show-current", "-v", "-vv",
                     "--contains", "--merged", "--no-merged", "-u", "--set-upstream-to", "--unset-upstream")
               for a in rest):
            return None
        if any(a in ("-m", "-M", "-c", "-C", "--move", "--copy") for a in rest):
            pos = [a for a in rest if not a.startswith("-")]
            return pos[-1] if pos else None
        pos = [a for a in rest if not a.startswith("-")]
        return pos[0] if pos else None
    return None


def repo_root(cwd):
    try:
        r = subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=cwd or None,
                           capture_output=True, text=True, timeout=5)
        return r.stdout.strip() or None
    except (OSError, subprocess.TimeoutExpired):
        return None


def check_issue_gate(branch, cwd):
    if branch.startswith(NO_ISSUE_PREFIX):
        return
    m = ISSUE_BRANCH.fullmatch(branch)
    if not m:
        return
    root = repo_root(cwd) or cwd or "."
    hint = "이슈 없이 작업 브랜치 금지 — 「깃헙 이슈를 만들 수 있도록 도와드릴게요」 → 이슈 번호 → 오케스트레이터 0단계(_workspace/<이슈>/state.md · _workspace/current)"
    try:
        with open(os.path.join(root, "_workspace", "current"), encoding="utf-8") as f:
            folder = f.read().strip()
    except OSError:
        raise Blocked(f"{branch}: _workspace/current 없음. {hint}")
    if not folder or not os.path.isfile(os.path.join(root, "_workspace", folder, "state.md")):
        raise Blocked(f"{branch}: _workspace/{folder or '?'}/state.md 없음. {hint}")
    issue = m.group(1)
    if issue and not re.match(rf"{issue}(?:_|$)", folder):
        raise Blocked(f"{branch}: 지금 이슈 폴더({folder})와 브랜치 이슈 번호(#{issue})가 다름 — 「#{issue} 진행」으로 0단계부터")


def check_default(argv, cwd):
    name = os.path.basename(argv[0]).lower().removesuffix(".exe")
    if name == "git":
        sub, rest, cpath = git_sub(argv)
        gcwd = os.path.join(cwd or "", cpath) if cpath else cwd
        if sub == "push":
            check_push(rest, gcwd)
        nb = new_branch_name(sub, rest)
        if nb:
            check_issue_gate(nb.removeprefix("refs/heads/"), gcwd)
    elif name == "gh" and len(argv) >= 3:
        if argv[1:3] == ["pr", "merge"]:
            raise Blocked("머지는 사람 몫 — GitHub PR 화면의 Squash and merge")
        if argv[1:3] == ["repo", "delete"]:
            raise Blocked("저장소 삭제 금지")
    if name == "gh" and len(argv) >= 2 and argv[1] == "api":
        joined = " ".join(argv)
        method = re.search(r"(?:-X|--method)[ =]?(\w+)", joined)
        m = method.group(1).upper() if method else "GET"
        if m == "DELETE":
            raise Blocked("gh api DELETE 금지")
        if "protection" in joined and m != "GET":
            raise Blocked("브랜치 보호 설정 변경은 사람 몫")


def check_readonly(argv, raw_tokens):
    name = os.path.basename(argv[0]).removesuffix(".exe")
    low = name.lower()
    if low == "git":
        sub, rest, _ = git_sub(argv)
        if sub == "branch" and all(a in ("-a", "-r", "--list", "--show-current", "-v", "-vv") for a in rest):
            return
        if sub == "remote" and rest in ([], ["-v"]):
            return
        if sub == "config" and rest[:1] in (["--get"], ["--list"], ["-l"]):
            return
        if sub not in GIT_READ:
            raise Blocked(f"읽기 전용 — git {sub} 금지")
        return
    if low == "gh":
        if tuple(argv[1:3]) in GH_READ:
            return
        if argv[1:2] == ["api"] and not re.search(r"(-X|--method)[ =]?(?!GET)\w+|-f |-F |--field|--input", " ".join(argv)):
            return
        raise Blocked(f"읽기 전용 — gh {' '.join(argv[1:3])} 금지")
    if low in ("python", "python3", "py", "uv"):
        script = next((a for a in argv[1:] if a.endswith(".py")), "")
        norm = script.replace("\\", "/")
        if script and any(d in norm for d in READ_SCRIPT_DIRS) and "-c" not in argv:
            return
        raise Blocked("읽기 전용 — 센서 스크립트(.claude/skills/sensors/scripts/) 밖의 실행 금지")
    if low == "sed" and any(a == "-i" or a.startswith("-i") or a.startswith("--in-place") for a in argv[1:]):
        raise Blocked("읽기 전용 — sed -i 금지")
    if low == "find" and any(a in ("-delete", "-exec", "-execdir", "-ok", "-fprint") for a in argv[1:]):
        raise Blocked("읽기 전용 — find -delete · -exec 금지")
    if low == "xargs":
        rest = [a for a in argv[1:] if not a.startswith("-")]
        if rest:
            check_readonly(rest, raw_tokens)
        return
    if name not in READ_CMDS and low not in READ_CMDS:
        raise Blocked(f"읽기 전용 — 허용 목록 밖 명령({name})")


def redirect_targets(tokens):
    out = []
    for i, t in enumerate(tokens):
        if t in (">", ">>", ">|", "&>", "&>>", "1>", "2>") and i + 1 < len(tokens):
            out.append(tokens[i + 1])
        elif re.fullmatch(r"\d?>>?", t) and i + 1 < len(tokens):
            out.append(tokens[i + 1])
    return [o for o in out if o not in ("/dev/null", "$null", "NUL") and not o.startswith("&")]


def inspect(cmd, cwd, readonly, depth=0):
    if depth > 3:
        raise Blocked("중첩 명령이 너무 깊음")
    try:
        tokens = tokenize(cmd)
    except ValueError:
        if readonly or re.search(r"\bpush\b.*\bmain\b|--force\b|\bpr\s+merge\b", cmd):
            raise Blocked("명령을 해석하지 못함(따옴표 짝) — 다시 써서 실행")
        return
    if readonly:
        targets = redirect_targets(tokens)
        if targets:
            raise Blocked(f"읽기 전용 — 파일로 쓰기 금지({targets[0]})")
    for part in split_commands(tokens):
        part = [t for t in part if not re.fullmatch(r"\d?(>>?|<<?|>&|<&)\d*|&>>?|>\|", t)]
        argv = strip_prefix(part)
        if not argv:
            continue
        for inner in nested(argv):
            inspect(inner, cwd, readonly, depth + 1)
        if readonly:
            check_readonly(argv, tokens)
        else:
            check_default(argv, cwd)


def block(data, reason):
    """막는 길 한 곳 — 이유는 stderr(나에게 보임) · 사실은 활동 로그 · 종료 코드 2."""
    print(f"guard: {reason}", file=sys.stderr)
    log_block(data, reason)
    return 2


def log_block(data, reason):
    """막은 사실을 활동 로그(trace_log.py)에 남김. 실패해도 막기 결과는 그대로."""
    try:
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import trace_log as hook_trace
        hook_trace.write(dict(data, hook_event_name="GuardBlocked", reason=reason))
    except Exception:
        pass


def main():
    readonly = "--readonly" in sys.argv[1:]
    data = {}
    try:
        data = json.load(sys.stdin)
        cmd = (data.get("tool_input") or {}).get("command") or ""
        if not cmd:
            return 0
        inspect(cmd, data.get("cwd"), readonly)
    except Blocked as e:
        return block(data, str(e))
    except Exception as e:
        # 훅 입력이 깨졌거나 guard 자신이 터진 경우. 0 을 돌려주면 울타리 없이 명령이 나가고
        # 아무 표시도 남지 않음 — settings.json 의 onFailure: block 은 Claude Code 2.1.295
        # 이상에서만 도므로 버전과 무관하게 여기서 스스로 닫음.
        return block(data, f"검사하지 못함({type(e).__name__}: {e}) — 사람이 확인한 뒤 다시 실행")
    return 0


if __name__ == "__main__":
    sys.exit(main())
