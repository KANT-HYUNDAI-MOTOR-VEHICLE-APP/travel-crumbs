# ADR 0002 — 활동 로그 · 공유 기록 · 권한 장치(2026-10-30 승인 · 구현)

> 템플릿 주: 하네스 원본 저장소에서 내린 결정을 근거로 옮겨 온 기록. 본문의 이슈 · PR 번호와 날짜는 원본 저장소 것이며, 이 저장소의 새 결정은 다음 번호부터 `0000-template.md` 꼴로 씀.

> 책 「하네스 엔지니어링 백과사전」 1.10절의 틀(메모리 · 스킬 · 프로토콜 + 중재 장치)로 하네스를 대조한 결과 빈 곳 셋을 채우는 설계. 구현은 승인 뒤 `harness/trace-permissions` 브랜치 · PR.

## 빈 곳

| 빈 곳 | 지금 | 생기는 문제 |
|---|---|---|
| 관찰(로그) | `_workspace` 산출물 · git 커밋뿐. 누가 · 언제 · 어떤 도구로 · 결과는 — 기록 없음 | 막힌 지점 · 시간 · 비용을 다시 볼 수 없음. 하네스를 고칠 근거가 없음 |
| 경험 기록 공유 | 리뷰 지적 · 막힘 · 결정 이유가 `_workspace`(gitignore)에만 | 상대 사람이 못 봄. `harness:evolve` 가 두 사람의 경험을 못 모음 |
| 권한 장치 | 리뷰어 「읽기 전용」은 글뿐(Bash 로 쓰기 가능) · `.claude/settings.json` 없음 · main 푸시 금지는 약속뿐 | 모델이 실수하면 막을 것이 없음 |

## 조사로 확인한 사실(공식 문서)

- 훅: `SubagentStart` · `SubagentStop` · `PostToolUse` · `PostToolUseFailure` · `SessionStart` · `Stop` 등. 하위 에이전트 안의 도구 호출에도 같은 훅이 돎 — 입력에 `agent_id` · `agent_type` 이 붙어 어느 에이전트인지 구분됨. `PostToolUse` 에 `duration_ms`.
- `SubagentStop` 입력: `agent_type` · `agent_transcript_path`(하위 에이전트 대화 원본) · `last_assistant_message`.
- 훅 `async: true` = 기다리지 않고 뒤에서 돎(작업 속도 영향 없음). `PreToolUse` 훅이 종료 코드 2 를 내면 그 호출을 막음 — allow 규칙보다 먼저.
- 권한 규칙 순서: deny → ask → allow, 먼저 맞은 것. deny 는 하위 에이전트에도 적용.
- Bash deny 는 **명령 글자**만 봄 — `Bash(git push *)` 는 `git -C . push origin main` 을 못 막음. 문서 권장: 글자 밖 검사는 `PreToolUse` 훅, OS 수준은 sandbox.
- `Read(./.env)` deny 는 Read 도구와 `cat` · `head` 같은 Bash 명령까지 막음. Python 스크립트가 직접 여는 것은 못 막음.
- 하위 에이전트 머리말에 `hooks` 를 두면 그 에이전트가 돌 때만 적용.
- 저장소의 `.claude/settings.json` 훅 · allow 는 사람마다 폴더 「신뢰」를 한 번 해야 켜짐. deny 는 바로 적용.
- 윈도우: Git Bash 가 없으면 훅이 PowerShell 로 돎 · 경로가 역슬래시로 옴.
- 출처: https://code.claude.com/docs/en/hooks · https://code.claude.com/docs/en/permissions · https://code.claude.com/docs/en/settings · https://code.claude.com/docs/en/sub-agents

## ① 활동 로그 — 훅이 남기는 원본

모델이 기록하게 하면 빠뜨림. 훅은 Claude Code 가 매번 부르므로 빠지지 않음.

| 항목 | 설계 |
|---|---|
| 걸 훅 | `SessionStart` · `PostToolUse` · `PostToolUseFailure` · `SubagentStart` · `SubagentStop` · `Stop` — 모두 `async: true` |
| 스크립트 | `.claude/hooks/trace_log.py` 하나(Python 표준 라이브러리만 · 맥 · 윈도우 같은 코드) |
| 저장 위치 | `_workspace/<이슈>/trace.jsonl`(개인 · gitignore). 오케스트레이터가 0단계에 `_workspace/current` 에 이슈 폴더 이름을 적음 → 훅이 그걸 읽음. 없으면 `_workspace/_sessions/<날짜>.jsonl` |
| 한 줄 내용 | 시각 · 세션 · 사건 · 에이전트(`agent_type` · 없으면 `main`) · 도구 · 대상 요약 · 성공/실패(오류 첫 줄) · 걸린 시간 |
| 대상 요약 | Bash = 명령 앞 200자 · Read/Edit/Write = 파일 경로 · Agent = 부른 에이전트 이름 · Skill = 스킬 이름 · 나머지 = 도구 이름만 |
| 남기지 않는 것 | 도구 결과 본문(크기만) · 파일 내용 · 비밀값 — `check_secrets.py` 의 패턴으로 가린 뒤 기록 |
| 읽는 법 | `.claude/hooks/trace_report.py <이슈>` → 표: 에이전트별 호출 수 · 시간 · 실패 · 센서 실행 · 시간순 흐름. 6단계 PR 본문 · ② 이슈 댓글에 요약을 붙임 |

## ② 공유 기록 — 이슈 댓글

| 항목 | 설계 |
|---|---|
| 언제 | 단계가 끝날 때마다(스펙 PR · 구현 · 리뷰 · 고침 · PR) |
| 누가 | 오케스트레이터가 `gh issue comment <n> --body-file …` |
| 꼴 | 머리 `[하네스] <단계> 끝` · 사람(누구의 세션) · 산출물 · 센서 결과 · 리뷰 지적(규칙 이름별 개수) · 막힘과 원인 · 다음 할 일 · ① 요약 |
| 회고 | `harness:evolve` 가 `gh` 로 `[하네스]` 댓글을 모아 반복 지적 → 센서 후보를 뽑음 |

저장소 파일(예: `docs/harness/lessons.md`)에 쌓지 않음 — 두 사람이 같은 파일에 덧붙이면 병합 충돌이 남. 하위 에이전트 머리말 `memory: project` 도 쓰지 않음 — 리뷰어에게 쓰기 도구가 자동으로 붙고, 에이전트별 기억 파일이 커밋돼 같은 충돌이 남.

## ③ 권한 장치 — 세 겹

**겹 1. `.claude/settings.json` deny(저장소에 커밋 · 둘 다 같은 규칙)**

| 규칙 | 막는 것 |
|---|---|
| `Bash(git push * main)` · `Bash(git push * main:*)` · `Bash(git push * HEAD:main)` | main 직접 푸시 |
| `Bash(git push --force *)` · `Bash(git push -f *)` | 강제 푸시(`--force-with-lease` 는 허용 — 자기 브랜치 rebase 뒤) |
| `Bash(gh pr merge *)` | 머지(사람 몫) |
| `Bash(gh api -X DELETE *)` · `Bash(gh repo delete *)` | 저장소 · 브랜치 삭제 |
| `Read(./**/.env)` · `Read(./**/.env.*)`(`.env.example` 은 예외) · `Read(./**/*.jks)` · `Read(./**/*.keystore)` | 비밀값 · 서명 키 읽기 |
| `Edit(./docs/api/openapi.json)` | 생성물 손 수정 — 서버 export 스크립트로만 |

**겹 2. `PreToolUse` 훅 `.claude/hooks/guard.py`(Bash 만)** — 겹 1 이 못 잡는 다른 꼴을 명령을 쪼개 봄. `git -C <경로> push … main` · `git push origin +main` · 따옴표로 감싼 `'push'` · `&&` 뒤에 숨은 푸시. 걸리면 종료 코드 2 + 이유.

**겹 3. 리뷰어 읽기 전용** — `pleos-reviewer` · `structure-reviewer` 머리말에 `hooks.PreToolUse`(Bash) → `guard.py --readonly`. 쓰기 명령(`>` · `>>` · `tee` · `rm` · `mv` · `cp` · `sed -i` · `git commit/push/switch/checkout/reset/stash/add` · `gh pr/issue create`)이면 막음. 읽기 · 검사 명령(`git diff/log/show` · `grep` · 센서 스크립트)은 통과.

**못 막는 것(알고 감)**
- Python · Node 스크립트가 파일을 직접 여는 것 — OS 수준 sandbox 의 몫. 윈도우 지원 · 에뮬레이터 · Docker 와의 충돌을 확인한 뒤 따로 정함.
- 사람이 GitHub 웹에서 하는 머지 — `team-workflow` 8절 약속.

## 바뀌는 파일

| 파일 | 변경 |
|---|---|
| `.claude/settings.json` | 새로 — deny 규칙 · 훅 설정 |
| `.claude/hooks/trace_log.py` · `trace_report.py` · `guard.py` | 새로 — Python 표준 라이브러리 |
| `.claude/hooks/tests/` | 새로 — guard 막기/통과 사례 · trace 비밀값 가림 사례 |
| `.claude/agents/pleos-reviewer.md` · `structure-reviewer.md` | 머리말 `hooks` |
| `.claude/skills/app-orchestrator/SKILL.md` | 0단계 `_workspace/current` · 단계 끝 이슈 댓글 · 6단계 trace 요약 · 막힘 표에 「guard 가 막음」 |
| `.claude/skills/team-workflow/SKILL.md` · `docs/setup.md` | 합류 절차에 「폴더 신뢰 한 번」 · 권한 장치 목록 |

## 구현 전 확인 결과

| 번호 | 확인 | 결과 |
|---|---|---|
| 1 | 훅 실행 명령(맥 `python3` · 윈도우 `python`) | exec 꼴 `uv run --no-project --quiet <스크립트>` 로 통일. 문서: exec 꼴은 셸 없이 실행 · `${CLAUDE_PROJECT_DIR}` 는 `command` · `args` 안에서 치환 · 윈도우는 `.exe` 여야 함(`uv.exe` 해당). 맥 실측 30ms(캐시 뒤) |
| 2 | 백그라운드 하위 에이전트의 토큰 · 시간 | **미확인.** 실제 세션이 필요함. `trace_log.py` 는 `PostToolUse`(Agent) 응답에 `totalTokens` 가 있으면 남기고 없으면 비움 — 첫 이슈에서 확인 · 없으면 `SubagentStop` 의 `agent_transcript_path` 로 보강(evolve) |
| 3 | `.env.example` 예외 | 문서: 같은 파일의 deny 목록 안에서 앞 규칙 뒤에 `!` 규칙을 두면 예외. 그 꼴로 씀. 실제 동작은 첫 세션의 「훅 확인」에서 |
| — | `onFailure` | Claude Code 2.1.295 이상(이 맥 2.1.309). 공개 JSON 스키마에는 아직 없음 — 스키마 검사에서 이 칸만 경고 · 나머지 오류 0 |
| — | 발견 | 다른 폴더에서 시작한 세션은 이 저장소의 `.claude/settings.json` 을 안 읽음(deny 미적용을 실측) → 합류 절차에 「저장소 폴더에서 세션 시작」 |
| — | 발견 | 리뷰어는 결과 파일을 써야 하는데 쓰기 도구가 없었음(Bash 리다이렉트 말고 길이 없음) → 리뷰어는 결과를 응답으로 돌려주고 메인이 저장하도록 바꿈 |


## 시험 시나리오

| 번호 | 상황 | 기대 |
|---|---|---|
| 1 | builder 가 `git push origin main` | deny 로 막힘 · trace 에 실패 한 줄 |
| 2 | builder 가 `git -C . push origin main` | guard 가 막음(종료 코드 2) |
| 3 | builder 가 `git push --force-with-lease origin feat/app-x` | 통과 |
| 4 | structure-reviewer 가 `echo x > a.txt` | guard --readonly 가 막음 |
| 5 | 아무 에이전트가 `cat server/.env` | deny 로 막힘 |
| 6 | 이슈 하나 끝까지 | `trace.jsonl` 에 에이전트별 줄 · 이슈에 `[하네스]` 댓글 단계 수만큼 · PR 본문에 trace 요약 |
| 7 | Bash 출력에 `ghp_…` 토큰 | trace 에 `ghp_***` 로 가려짐 |
