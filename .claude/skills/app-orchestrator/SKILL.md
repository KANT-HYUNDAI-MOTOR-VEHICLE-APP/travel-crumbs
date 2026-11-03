---
name: app-orchestrator
description: '<앱 이름>(Pleos Connect 차량 앱 + FastAPI 서버 · 한 저장소 · 두 사람 협업) 이슈 하나를 기능 흐름(develop → <기능명>/main → spec PR → impl PR → develop PR) 또는 버그 흐름(bug/ → develop PR)으로 스펙 → 구현 → 리뷰 2관점 → 수정 → 센서 → PR 까지 조율하는 오케스트레이터. 「이슈 #n 진행해줘」 「<앱 이름> 티켓: …」(글로 티켓을 주면 이슈를 만들어 진행) 「#n 스펙만 써 줘」 「#n 구현해 줘」 「#n 리뷰 다시」 「#n 센서 돌려」 「#n PR 올려」 「#n 이어서」 「#n 마무리(머지 뒤)」 「스펙 수정 · 보완」 「재리뷰」 「다시 실행」 「이전 결과 개선」 같은 요청에 사용한다. 이슈 번호 전의 기획 요청 — 「<기능명> 같이 기획하자」 「기획 도와줘」 「후보 좀 보여 줘」 — 도 이 스킬의 「기획 흐름」으로 받는다(되묻지 않는다). 단순 질문 · 한 줄 수정 · 문서 오타에는 쓰지 않는다(직접 한다). 하네스 자체의 회고 · 피드백 반영은 harness:evolve.'
---

# <앱 이름> 오케스트레이터

**실행 모드: 혼합** — 아래 표. 브랜치 · PR 대상은 `team-workflow` 2 · 3절이 원본. 사람 승인은 PR 셋(spec → `<기능명>/main` · impl → `<기능명>/main` · `<기능명>/main` → develop)에서 사람이 머지로 함. 에이전트는 작업 브랜치 커밋 · 푸시 · PR 열기까지. 머지 · main · develop · 이미 있는 `<기능명>/main` 푸시 · 보호 규칙 변경 금지.

| 단계 | 실행 모드 | 누가 |
|---|---|---|
| 0 시작 | 메인 직접 | 티켓 읽기 · 종류(기능 · 버그) · git 상태 · `<기능명>/main` · `state.md` |
| 1 스펙 | 서브에이전트 1회 | `spec-writer` |
| 2 구현 | 지속형(영역마다 하나 · 병렬) | `builder` → `name: "app-builder"` · `name: "server-builder"` |
| 3 리뷰 | 서브에이전트 병렬(한 메시지) | `pleos-reviewer` 1 + `structure-reviewer` × 영역 수 |
| 4 수정 | `SendMessage` → builder → 지적 관점만 재리뷰 · 최대 2회 | 메인 ↔ builder |
| 5 센서 · rebase | 메인 직접 | `sensors` 스킬 명령 |
| 6 PR | 메인 직접 | 경로 지정 커밋 · 푸시 · impl → `<기능명>/main` PR(버그는 → develop) |
| 7 develop PR | 메인 직접 | impl 머지 확인 → `<기능명>/main` → develop PR |

| 에이전트 | 모델 | 읽는 스킬 | 산출물(`_workspace/<이슈>/`) |
|---|---|---|---|
| `spec-writer` | opus | pleos-vehicle-sdk · android-architecture · server-architecture | `01_spec-writer_spec.md` |
| `builder` | opus | team-workflow · sensors · 영역 스킬 | 코드 · `02_builder_<area>_report.md` |
| `pleos-reviewer` | sonnet | pleos-review-checklist | `03_pleos-reviewer_findings.md` |
| `structure-reviewer` | sonnet | android-architecture 또는 server-architecture | `03_structure-reviewer_<area>_findings.md` |

메인이 먼저 읽는 스킬: `team-workflow` · `sensors`. 규칙 본문을 에이전트 프롬프트에 되풀이하지 않음 — 에이전트 정의가 스킬을 읽게 되어 있음.

## 0단계 — 시작(처음 · 이어서 · 일부 재실행 구분)

1. **티켓 입력.**
   - 이슈 번호 → `gh issue view <n> --json number,title,body,assignees`.
   - 담당자에 `gh api user -q .login` 이 없으면 멈추고 보고(남의 이슈는 실행하지 않음 — `team-workflow` 1절).
   - 종류: 제목 앞말 `[기능]` → 기능 흐름 · `[버그]` → 버그 흐름(아래 「버그 흐름」 절) · `[하네스 피드백]` → 하네스 흐름(아래 「하네스 피드백 흐름」 절). 앞말이 없으면 본문 칸으로 판단 · 애매하면 한 줄로 확인.
   - 글로 받으면 → 이슈 템플릿(`feature.yml` · `bug.yml`의 칸) 꼴로 초안을 보여 주고 사용자가 「만들어」 하면 `gh issue create --title … --body-file …` → 번호 확정.
   - **이슈 게이트** — 번호가 없고 글이 템플릿 칸(영역 · 누가·언제·왜 · 사용 장면 · 완료 조건)을 못 채우는 막연한 요청(「공모전 찾아서 구현해줘」 · 「알아서 기능 하나」)이면 작업 브랜치 · 스펙 · 구현을 하지 않음. 「깃헙 이슈를 만들 수 있도록 도와드릴게요」로 답하고 빈 칸을 하나씩 물음. 칸의 내용은 사람의 답만으로 채움(AI 가 조사 · 추측으로 기능을 지어내지 않음 — 무엇을 만들지는 사람 결정 · 그래야 이슈가 사람의 의도를 담은 작업지시서가 됨). 이 게이트는 guard 가 기계로도 막음(이슈 작업 폴더 `_workspace/current` → `state.md` 없이 `<기능명>/…` · `bug/…` 브랜치 생성 금지).
     - 게이트가 막는 것은 **결정과 산출**이지 **선택지와 제약을 드러내는 일**이 아님 — 「같이 기획하자」 · 「기획 도와줘」 · 「후보 좀 보여 줘」는 되묻지 않고 아래 「기획 흐름」으로 받음(`team-workflow` 2절 「사람 ↔ AI 공동 기획」). 거기서도 무엇을 만들지는 사람이 정함.
   - 번호도 글도 없으면 `gh issue list --assignee @me --state open` 을 보여 주고 고르게 함.
2. **상태 확인.** `git status --porcelain` 비어 있음 · `git fetch origin` · `git switch develop && git pull --ff-only` · `python .claude/skills/sensors/scripts/doctor.py`(FAIL 이면 멈추고 보고 — 환경은 사람 몫).
3. **작업 폴더.** `_workspace/<이슈>/state.md` 를 봄.
   - 없음 → 처음. 폴더 · `state.md`(이슈 · 브랜치 · 단계 체크리스트 · PR 번호 칸) 생성.
   - 있음 + 「이어서」 → `state.md` 의 마지막 단계 다음부터. 열린 PR 이 있으면 `gh pr view <번호> --json state` — MERGED 면 따로 묻지 않고 다음 단계(spec 머지 → 2단계 · impl 머지 → 7단계) · OPEN 이면 「사람 확인 대기」 보고 후 종료 · CLOSED 면 이유를 묻고 멈춤.
   - 있음 + 「스펙만 다시」 · 「리뷰 다시」 · 「센서만」 → 그 단계만.
   - 있음 + 새 입력(이슈 본문이 바뀜 · 「처음부터」) → 폴더를 `_workspace/<이슈>_<yyyymmdd-HHMM>/` 로 옮기고 새로.
   - 처음이면 `.claude/log/<n>-<짧은이름>/log.md` 머리도 만듦(꼴 · 규칙 원본은 `.claude/log/README.md`). 짧은이름은 4번의 기능명 · 버그 · 하네스 브랜치의 짧은이름과 같게.
   - 어느 경우든 지금 폴더 이름을 `_workspace/current` 에 한 줄로 씀 — 활동 로그 훅이 이 폴더의 `trace.jsonl` 에 기록함(「기록」 절).
4. **기능명 · `<기능명>/main`(기능 흐름).** `team-workflow` 2절.
   - 기능명: 이슈 「참고」에 `기획: <기능명>/main · docs/spec/<폴더>/…` 가 있으면 그 폴더 이름(기획 브랜치 방식 — `team-workflow` 2절 「기능 브랜치에서 기획」). 없으면 이슈 제목에서 정함(영문 소문자 · 숫자 · `-` · 짧은 명사 — `login` · `navigation`).
   - **산출물 경로**(`docs/spec/README.md`): 이슈 「산출물」 칸에 있으면 그대로. 비어 있으면 `docs/spec/<기능명>/artifacts/<짧은이름>.md` 를 제안하고 사용자 확인을 받음(같은 폴더에 이미 있는 산출물 목록을 함께 보여 줌 — 고칠지 새로 만들지는 사람이 정함). 확인 전에는 1단계로 가지 않음. `state.md` · 이슈 댓글에 기록.
   - `git ls-remote --heads origin <기능명>/main` — 이 이슈의 것(`state.md` 에 같은 이름)이거나 이슈 「참고」가 기획 브랜치로 가리킨 것이면 그대로(`git fetch origin <기능명>/main` · 새로 만들지 않음) · 그 밖에 다른 이슈의 것이면 `<기능명>-<n>`.
   - 없으면 `git switch -c <기능명>/main origin/develop && git push -u origin <기능명>/main`(처음 한 번 · 이후 guard 가 막음).
   - 이슈 댓글로 기능명 · 브랜치 셋을 남김 · `state.md` 에 기록.
   - 두 영역(app · server)이어도 브랜치는 하나 — 폴더가 달라 충돌 없음.

## 1단계 — 스펙(`spec-writer` · 1회)

브랜치: `git switch -c <기능명>/spec origin/<기능명>/main`(있으면 `git switch` · `git rebase origin/<기능명>/main`).

프롬프트에 넣는 것(아홉 가지 · 스킬 규칙은 넣지 않음):

```
이슈 #<n> 스펙 작성.
[티켓] <gh issue view 본문 그대로>
[전제] <앱 이름> MVP — <한두 문장: 앱이 무엇을 하는지 · 주행 중 / 정차 중에 각각 무엇이 되는지>. 주행 여부는 Pleos Vehicle SDK 기어 + CarUxRestrictions. 로그인은 Pleos 계정(서버가 토큰 교환). 서버(FastAPI)는 <로그인 · 서버가 맡는 데이터 · 기능>. 좌표 · 기어 · VIN · Pleos 토큰은 서버에 없음.
[지금 상태] 화면: <ls app/src/main/java/**/ui> / 서버 패키지: <ls server/app> / 계약: docs/api/openapi.json <있음 · 없음> / 승인 스펙: <ls docs/spec/*/artifacts>
[기획] <이슈 「참고」의 기획 위치 · 없으면 없음> — 그 아래 문서 전부(artifacts/ 빼고 · spec 브랜치 작업 트리에 있음)
[산출물] <0단계 4번에서 정한 docs/spec/<기능명>/artifacts/<이름>.md> · <새 파일 · 기존 파일 고침>
[출력] _workspace/<n>/01_spec-writer_spec.md — 절: 요약 · 화면 · 상태 · 데이터 · API 계약 변경 · 시험 케이스 · 미확정 · 완료 조건(app · server). 명사형. 화면 문구는 「」.
[규칙] 이슈 밖 금지 · 구현 방법 안 씀 · 모르는 건 미확정 표.
[재실행이면] 기존: <경로> · 고칠 이유: <PR 코멘트 · 모순 요약>
```

호출: `Agent(subagent_type: "spec-writer", prompt: 위)`. 결과 파일을 읽고 —
- 「미확정」 표가 있으면 사용자에게 먼저 보여 줌(답하면 재실행 · 답 없으면 그대로 PR 에 「남은 결정」).
- 산출물 경로(`docs/spec/<기능명>/artifacts/<이름>.md`)로 복사(spec 브랜치의 커밋 대상 · 사람 문서는 건드리지 않음) · API 계약 변경이 있으면 `docs/api/openapi.json` 초안도.
- 커밋 `docs(spec): #<n> <제목>` — `git add <산출물 경로> [docs/api/openapi.json] && git commit -- <같은 파일들>` · `git push -u origin <기능명>/spec` · `gh pr create --base <기능명>/main --title "spec: #<n> <제목>" --body-file <본문>`(본문: 이슈 · 미확정 · 「확인 뒤 머지하면 구현을 바로 시작」).
- `state.md` 에 PR 번호 · 「spec PR 대기」 · 이슈 댓글.
- **머지 기다리기**: 사람이 머지하면 따로 지시가 없는 한 바로 2단계. 세션이 열려 있으면 백그라운드 명령으로 기다림 — `gh pr view <번호> --json state -q .state` 를 60초마다 · MERGED 또는 CLOSED 가 되면 끝(최대 8시간). 끝나면 MERGED → 2단계 · CLOSED → 이유를 묻고 멈춤. 세션이 닫히면 다음 「#n 이어서」가 0단계 3번에서 이어감. 사람이 「멈춰 · 기다려」라고 했으면 기다리지 않고 종료.
- 이슈에 「스펙 생략」 표시가 있으면 이 단계 전체를 건너뛰고 이슈 본문을 스펙으로 씀(2단계 바로).

## 2단계 — 구현(`builder` · 지속형 · 영역별 병렬)

전제: spec PR 머지됨(`gh pr view` state MERGED) 또는 스펙 생략.

브랜치: `git fetch origin && git switch -c <기능명>/impl origin/<기능명>/main`(버그 흐름이면 `bug/<n>-<짧은이름>` 을 `origin/develop` 에서). spec 브랜치는 로컬 · 원격 삭제(`git branch -D <기능명>/spec` · `git push origin --delete <기능명>/spec`).

- 영역 = 스펙 「완료 조건」의 app · server. 한 메시지에서 병렬로 띄움:
  - `Agent(name: "app-builder", subagent_type: "builder", prompt: "영역 app · 스펙 <산출물 경로> · 브랜치 <이름> · 작업 폴더 _workspace/<n>/ · 보고서 02_builder_app_report.md")`
  - `Agent(name: "server-builder", subagent_type: "builder", prompt: 같은 꼴 · 영역 server · 보고서 02_builder_server_report.md)`
  - 두 영역이면 서버가 계약(`openapi.json`)을 만들고 앱은 스펙의 계약 초안으로 짬 — 서버 보고가 먼저 오면 앱 builder 에 `SendMessage` 로 「계약 갱신됨 · `check_api_contract.py` 다시」.
- 첫 보고의 도구 목록을 확인(빠진 도구가 있으면 그 작업은 메인이 대신 · 보고서에 적음).
- 보고서 「상태: 완료」 → 다음. 「막힘」 → 원인이 사람 몫(플랫폼 설치 · Pleos 승인 · 비밀값)이면 사용자에게 할 일 명령과 함께 보고 후 종료 · 그 외는 한 번 더 지시.
- **센서 1차**: `sensors` 스킬 표의 명령을 영역에 맞게 돌려 `_workspace/<n>/04_sensors.md` 에 기록(리뷰어가 「센서가 재는 규칙」을 다시 세지 않게). 빨간 것은 리뷰 전에 builder 가 고침.
- **freeze**: 리뷰 전 변경 파일 목록과 해시를 고정 — `git diff --name-only origin/<대상 브랜치>...HEAD | xargs shasum -a 256 > _workspace/<n>/freeze.txt`(윈도우: `python -c` 로 같은 해시). 리뷰 뒤 다시 계산해 다르면 「리뷰 중 변경」으로 보고.

## 3단계 — 리뷰(서브에이전트 · 한 메시지 병렬)

- `Agent(subagent_type: "pleos-reviewer", prompt: "이슈 #<n> · 브랜치 · 대상 브랜치 · 영역 <app · server · 둘> · 스펙 <경로> · 작업 폴더 · 센서 결과 _workspace/<n>/04_sensors.md(있으면)")`
- 영역마다 `Agent(subagent_type: "structure-reviewer", prompt: "영역 <area> · 이슈 · 브랜치 · 대상 브랜치 · 작업 폴더 · 센서 결과")`
- 리뷰어는 결과 문서를 응답으로 돌려줌(읽기 전용 — 파일을 안 씀) → 메인이 각각 `03_<reviewer>[_<area>]_findings.md` 로 손대지 않고 저장.
- 결과 셋을 읽어 한 표로 합침(관점 열 추가) · 중복 행은 하나로(둘 다 지적 표시) · freeze 대조.
- 「스펙 모순」 발견 → 1단계로(스펙 수정 PR 작게 · 재승인) — 구현은 멈추지 않고 모순 부분만 보류.

## 4단계 — 수정(≤ 2회)

- 발견 0 → 5단계.
- 발견 있음 → `SendMessage(to: "<area>-builder", message: 표(파일:줄 · 규칙 · 고칠 방향))` → builder 의 답 표(고침 · 안 고침(이유)) → 지적이 있던 관점만 재리뷰(3단계 호출 중 해당 에이전트만).
- 2회 뒤 남은 행 → `_workspace/<n>/05_open_findings.md` → PR 본문 「남은 결정」. 더 돌지 않음.

## 5단계 — 센서 · rebase

1. `sensors` 스킬 표의 명령을 영역에 맞게 전부 → `_workspace/<n>/04_sensors.md`(명령 · 결과 · 첫 오류 줄).
2. 빨간 것 → 해당 builder 에 `SendMessage` → 녹색까지(센서를 끄거나 baseline 금지).
3. `git fetch origin && git rebase origin/<대상 브랜치>`(기능 = `<기능명>/main` · 버그 = develop) → 충돌이면 builder 에 `team-workflow` 5절로 해소 지시 → 센서 전부 다시.
4. 두 영역이면 통합 확인: 로컬 서버(`uv run uvicorn …`) + 에뮬레이터 앱 → 스펙 시험 케이스의 왕복 행을 app-builder 가 실행 · 보고.

## 6단계 — PR

1. 커밋 상태 확인: builder 가 경로 지정으로 커밋했는지(`git log origin/<대상 브랜치>..HEAD --stat`) · 생성물(openapi.json · uv.lock · 리비전)이 같은 변경에 있는지. 빠진 파일은 경로 지정으로 추가 커밋.
2. `git push --force-with-lease -u origin <브랜치>`.
3. `gh pr create --base <대상 브랜치> --title "<type>(<area>): #<n> <제목>" --body-file _workspace/<n>/06_pr_body.md` — 본문은 `.github/pull_request_template.md` 꼴로 채움: 이슈(버그 흐름이면 `Closes #<n>`) · 스펙 · 리뷰 표(합친 것) · 센서 결과 · 확인법(보고서에서 · 기능이면 「`git switch <기능명>/impl` → 빌드 · 에뮬레이터 설치 → 써 볼 순서」) · 남은 결정 · 활동 로그 요약(`python3 .claude/guardrails/trace_report.py <n> --flow 15` 출력을 `<details>` 로 접어서).
4. `state.md` 에 PR 번호 · 「impl PR 대기」(버그는 「develop PR 대기」) · 이슈 댓글. 기능 흐름이면 1단계와 같은 방법으로 머지를 기다림 → MERGED 면 7단계.

## 7단계 — develop PR(기능 흐름)

1. impl PR 머지 확인 → impl 브랜치 로컬 · 원격 삭제.
2. `gh pr create --base develop --head <기능명>/main --title "feat(<area>): #<n> <제목>" --body-file _workspace/<n>/07_pr_body.md` — 본문: `Closes #<n>` · 스펙 · 지금까지의 PR 둘 링크 · 확인법(`git switch <기능명>/main` → 빌드 · 설치) · 남은 결정.
3. 충돌(develop 이 앞서 나감)이면 `team-workflow` 3절 「공통」의 `<기능명>/sync` 로 해소.
4. 피드백으로 고칠 것이 오면 `<기능명>/impl` 을 다시 따서 2단계부터(리뷰 · 센서 · impl PR) — `<기능명>/main` 에 직접 푸시하지 않음.
5. `state.md` 「develop PR 대기」 · 이슈 댓글 → 종료(최종 머지는 사람).

## 기획 흐름(이슈 전 · 사람 ↔ AI 공동 기획)

「`<기능명>` 같이 기획하자」 · 「기획 도와줘」 · 「후보 좀 보여 줘」로 들어옴. 규칙 원본은 `team-workflow` 2절 「사람 ↔ AI 공동 기획」 · 산출물 자리는 `docs/spec/README.md`.

1. **기능명 · 브랜치.** 기능명을 사람과 한 줄로 정함(영문 소문자 · 숫자 · `-`). `<기능명>/main` 이 없으면 **사람이 만들게 안내하고 기다림** — 에이전트는 만들지 않음(이슈 번호 전이라 guard 가 막음 · 기획의 주인은 사람). 만드는 법 둘: GitHub 웹에서 develop 을 고른 채 브랜치 칸에 이름 치기 · 로컬에서 `git switch -c <기능명>/main origin/develop && git push -u origin <기능명>/main`.
2. **작업 폴더.** `_workspace/_planning/<기능명>/notes.md`(개인 · gitignore)에 오간 결정 · 후보 표 · 사람이 고른 것을 남김. `_workspace/current` 에 `_planning/<기능명>` 을 씀 — 활동 로그가 여기 쌓임.
3. **제약 먼저.** 후보를 내기 전에 이 기획에 걸리는 전제 · 규제를 한 번에 알림 — 주행 규제 · 차량 신호(`pleos-vehicle-sdk`) · 서버 경계 · LLM 경계 7 · 개인정보(`server-architecture`) · 심사 5항목(`pleos-review-checklist`) 중 해당하는 것만. 못 가는 길을 기획이 다 선 뒤에 알게 하지 않기 위함.
4. **후보 표.** 사람이 고를 지점마다 후보를 표로 냄(선택지 · 장단 · 이 저장소 전제와 맞는지 · 모르는 것은 「확인 필요」). 고르는 건 사람 — AI 가 하나를 골라 진행하지 않음.
5. **초안 받아쓰기.** 사람이 고른 것만으로 `docs/spec/<기능명>/` 아래 기획 문서 초안을 작업 트리에 씀(`artifacts/` 밖). 고르지 않은 곳은 「미정」으로 남김. 커밋 · 푸시는 사람 — 에이전트는 하지 않음.
6. **이슈로 넘김.** 기획이 서면 `feature.yml` 칸을 기획 문서에서 채워 초안을 보여 주고, 사람이 「만들어」 하면 `gh issue create`(「참고」에 `기획: <기능명>/main · docs/spec/<기능명>/` · 담당자 지정). 번호가 나오면 0단계로 들어가고, 그 이슈 `log.md` 첫 절에 「기획: `docs/spec/<기능명>/` · 과정 `_workspace/_planning/<기능명>/`」 한 줄을 적음.

- 이 흐름에서 하지 않는 것: 작업 브랜치(`<기능명>/spec` · `impl`) 생성 · 스펙 작성(`spec-writer`) · 코드. 전부 이슈 번호 뒤.
- 사람이 기획 없이 바로 가겠다면 막지 않음 — 이슈만 만들고 0단계로(기획 문서 없는 기능은 전처럼 에이전트가 `<기능명>/main` 을 0단계에서 만듦).

## 버그 흐름(`[버그]` 이슈)

- 0단계 1~3 같음 · 4번 대신 `git switch -c bug/<n>-<짧은이름> origin/develop`.
- 1단계(스펙) 없음 — 이슈의 재현 순서 · 기대 · 실제가 스펙. builder 프롬프트에 「재현 테스트를 먼저 쓰고 빨강 확인 → 고침 → 녹색」.
- 2 ~ 5단계 같음(대상 브랜치 = develop) → 6단계 PR → develop(`Closes #<n>` · 원인 · 고친 것 · 재현 순서대로 확인한 결과). 7단계 없음.

## 하네스 피드백 흐름(`[하네스 피드백]` 이슈)

하네스(`.claude/` · `CLAUDE.md` · 이슈 · PR 템플릿)를 바꾸는 길은 이것 하나. 직접 프롬프트로 바꾸면 이유가 이슈 · `.claude/log/` 에 남지 않아 다른 사람이 같은 방식을 따를 수 없음.

- 0단계 1~3 같음(담당자 · `state.md` · log 머리) · 4번 대신 `git switch -c harness/<n>-<짧은이름> origin/develop`.
- 스펙 · builder · 리뷰어 단계 없음. 메인이 `harness:evolve` 를 부르고 이슈의 「무슨 일이 있었나 · 바라는 동작 · 완료 조건」을 피드백으로 넘김(하네스 파일은 builder 대상이 아님).
- 검증: `uv run --no-project --with pytest pytest -q .claude/guardrails/tests` · `check_repo_hygiene.py` · `check_secrets.py` · 바꾼 YAML 파싱. guard 를 바꿨으면 막는 · 통과 사례를 시험에 추가.
- 결정 기록 `docs/architecture_decision_record/` 한 편 · `.claude/log/<n>-…/log.md` 「반영 끝」 절 · `trace.md`.
- PR → develop(`Closes #<n>` · 바뀐 파일 표 · 검증 결과 · 남은 결정) · 상대 승인 · 머지는 사람. 이슈 댓글.

## 머지 뒤 — 「#n 마무리」

`git switch develop && git pull --ff-only` → 센서 전부 → 빨간 것은 새 버그 이슈 → 남은 작업 브랜치 로컬 삭제(`git branch -D` · `<기능명>/main` 원격은 사람이 머지 때 삭제) → `_workspace/<n>/` 는 남김(회고 자료) · `_workspace/current` 지움 · 이슈에 `[하네스] 마무리` 댓글. 다른 이슈 브랜치가 있으면 그 대상 브랜치로 rebase 안내.

## 데이터 전달

| 무엇 | 어디 | 누가 쓰고 누가 읽나 |
|---|---|---|
| 티켓 | GitHub 이슈 | 사람 → 메인 → spec-writer |
| 스펙 | `_workspace/<n>/01_spec-writer_spec.md` → 승인 뒤 산출물 `docs/spec/<기능명>/artifacts/<이름>.md`(커밋) | spec-writer → 상대 · builder · 리뷰어 |
| 계약 | `docs/api/openapi.json`(서버가 생성 · 커밋) | server-builder → app-builder · 센서 |
| 구현 보고 | `_workspace/<n>/02_builder_<area>_report.md` | builder → 메인 · PR 본문 |
| 리뷰 표 | `_workspace/<n>/03_<reviewer>[_<area>]_findings.md` | 리뷰어 → 메인 → builder · PR 본문 |
| 센서 | `_workspace/<n>/04_sensors.md` | 메인 → 리뷰어 · PR 본문 |
| 남은 결정 | `_workspace/<n>/05_open_findings.md` → PR 「남은 결정」 | 메인 → 상대 사람 |
| 진행 상태 | `_workspace/<n>/state.md`(단계 · 브랜치 · PR 번호) | 메인(다음 세션의 0단계) |
| 지속형 메시지 | `SendMessage` 메인 ↔ `<area>-builder` | 리뷰 표 · 계약 갱신 · 센서 결과 |
| 활동 로그 | `_workspace/<n>/trace.jsonl`(훅이 자동) | 훅 → 메인(요약) · 회고 |
| 공유 기록 | `.claude/log/<n>-<짧은이름>/log.md` · `trace.md`(커밋) + 이슈 댓글 `[하네스] <단계> 끝`(같은 내용) | 메인 → 상대 사람 · `harness:evolve` |

`_workspace/` 는 개인(gitignore) · `docs/` 는 공유(커밋). 상대 사람은 PR 본문과 `docs/` 만 보고 판단할 수 있어야 함.

## 기록 — 활동 로그 · 작업 기록 · 이슈 댓글(모든 단계 공통)

- **활동 로그**: `.claude/settings.json` 의 훅이 도구 호출 · 하위 에이전트 시작/끝 · guard 차단을 `_workspace/<n>/trace.jsonl` 에 자동으로 남김. 메인은 쓰지도 고치지도 않음 — 모델이 쓰면 빠지고, 고치면 기록이 아님. 읽을 땐 `python3 .claude/guardrails/trace_report.py <n>`.
- **작업 기록(`.claude/log/`)**: 1 · 2 · 4 · 6단계(버그는 2 · 4 · 6 · 하네스는 반영 끝)가 끝날 때 아래 내용을 `.claude/log/<n>-<짧은이름>/log.md` 에 한 절 덧붙이고 그 단계의 작업 브랜치에 경로 지정 커밋 `docs(log): #<n> <단계>`. PR 을 열 때 `trace_report.py <폴더> --flow 15` 출력을 `trace.md` 로 저장 · 커밋. 꼴 · 규칙 원본은 `.claude/log/README.md`. 이유: AI 활용 기록 공유 · 작업 방식 단일화 — 저장소에 남아야 다음 사람 · 회고가 같은 자료를 봄(`_workspace` 는 개인 폴더).
- **이슈 댓글**: 같은 절을 `gh issue comment <n> --body-file _workspace/<n>/comment_<단계>.md` 로도 올림(알림용). 7단계 · 마무리는 커밋할 브랜치가 없어 댓글로만.

```
[하네스] <단계> 끝 — <사람 · 세션의 git user.name>
산출물: <파일 · 커밋 · PR>
센서: <명령별 녹색/빨강 한 줄씩>
리뷰 지적: <규칙 이름 × 개수 · 센서 후보 표시>
막힘: <무엇 · 원인 · 사람 몫인가> 또는 없음
다음: <다음 단계 · 누구 몫>
활동: <trace_report 요약 첫 줄 — 도구 호출 · 실패 · guard 차단 수>
```

- 기록 · 댓글에 비밀값 · 개인정보 · 로그 원문을 넣지 않음. 리뷰 표 전문은 PR 본문에만.

## 오류 처리

| 상황 | 대응 |
|---|---|
| 에이전트 결과 파일 없음 · 빈 파일 | 한 번 재호출. 또 실패 → 그 단계 없이 진행하되 PR 본문 · `state.md` 에 누락 명시. 메인이 대신 채우지 않음(추측 금지) |
| builder 「막힘」 | 원인이 사람 몫(플랫폼 설치 · Pleos 승인 · 비밀값 · gh 로그인)이면 명령과 함께 보고 후 종료 · 그 외 한 번 더 지시 · 또 막히면 보고 |
| 지속형 builder 응답 없음 | `SendMessage` 로 상태 확인 → 없으면 같은 `subagent_type: "builder"` 를 새 이름(`app-builder-2`)으로 띄우고 보고서 · 스펙 · 브랜치를 프롬프트로 넘김 |
| 리뷰어 발견에 근거 없음(파일:줄 없음) | 그 행은 표에서 빼고 「근거 없음」으로 기록 · 재호출하지 않음 |
| 센서 빨강 2회 연속 같은 오류 | 사용자에게 로그 첫 오류와 함께 보고 · 센서 끄기 금지 |
| rebase 충돌 해소 실패 | builder 2회 → 사용자에게 충돌 파일 목록 보고(사람이 결정) |
| 상대 PR 머지로 내 브랜치 깨짐 | 0단계부터(대상 브랜치 당김 → 센서 → builder) |
| `<기능명>/main` 이 이미 있음(다른 이슈) | `<기능명>-<n>` 으로 · 이슈 댓글에 이유 |
| 머지 기다리기가 8시간을 넘김 | 「사람 확인 대기」 보고 후 종료 · 다음 「#n 이어서」 |
| 사용량 한도 · 인증 만료 · 권한 거부 | 재시도하지 않음. 부분 산출물을 열어 어디까지 됐는지 `state.md` 에 적고 보고(한도면 풀리는 시각도) |
| `gh` 미로그인 · 저장소 권한 없음 | 0단계에서 멈춤 · `gh auth login` 안내 |
| guard 가 명령을 막음(`guard: …`) | 이유대로 다시(작업 브랜치로 푸시 · 머지는 사람). 같은 일을 다른 꼴의 명령으로 다시 시도하지 않음 — 우회는 규칙 위반. 사람 판단이 필요하면 막힌 명령 · 이유를 보고 |
| 활동 로그가 안 쌓임(`trace.jsonl` 없음) | 작업은 계속. 폴더 신뢰 · `uv` 를 `doctor.py` 로 확인하라고 보고 · PR 본문에 「활동 로그 빠짐」 |
| Pleos 계정 API 승인 전 | 서버는 `pleos_auth_stub` 으로 끝까지 · PR 본문에 「실 Pleos 미검증」 명시 |

## 테스트 시나리오

- **정상(기능 · 한 영역)**: 「이슈 #5 진행해줘」(`[기능]` app · <기능 요약>) → 0단계 → 기능명 `my-feature` · `my-feature/main` 생성 · 푸시 → `my-feature/spec` → spec-writer → PR(→ `my-feature/main`) → 머지 기다림. 사람 머지 → 바로 `my-feature/impl` → app-builder → 리뷰 2 → 발견 3 → SendMessage → 재리뷰 0 → 센서 녹색 → rebase → PR(→ `my-feature/main`) → 머지 기다림. 사람 머지 → 7단계 PR(`my-feature/main` → develop) → 종료. 기대: `_workspace/5/` 에 01 · 02_app · 03 ×2 · 04 · state · freeze · 06 · 07 · 원격에 spec · impl 브랜치 없음.
- **정상(버그)**: 「이슈 #9 진행해줘」(`[버그]` 소리 안 남) → `bug/9-no-sound`(develop 에서) → 재현 테스트 빨강 → 고침 → 리뷰 · 센서 → PR(→ develop · `Closes #9`) → 종료.
- **정상(기획 문서 · 산출물 미지정)**: 「이슈 #12 진행해줘」(참고 `기획: share/main · docs/spec/share/` · 산출물 칸 비어 있음 · 사람이 `share/main` 을 만들고 기획을 올려 둠) → 기능명 `share` · `share/main` 그대로 씀(새로 안 만듦) → 산출물 `docs/spec/share/artifacts/share-link.md` 제안 · 기존 산출물 목록과 함께 확인 요청 → 사람 「좋아」 → spec-writer 가 `docs/spec/share/` 아래 문서를 읽음 → spec PR 에 산출물 한 파일만. 기대: 사람 문서 변경 0 · 확인 전 spec-writer 호출 0.
- **정상(기획 흐름 · 이슈 전)**: 「<기능 설명> 같이 기획하자」 → 되묻지 않고 기획 흐름 → 기능명 `new-idea` 합의 → `new-idea/main` 은 사람이 만들게 안내 → 제약(주행 규제 · LLM 경계 7 · 개인정보) 먼저 → 온디바이스 LLM ↔ 서버 LLM 후보 표 → 사람이 고름 → `docs/spec/new-idea/overview.md` 초안을 작업 트리에 씀 → 사람이 커밋 → 이슈 초안 → `gh issue create` → 0단계. 기대: 에이전트의 `git switch -c` · `git push` 호출 0 · `spec-writer` 호출 0 · `_workspace/_planning/new-idea/notes.md` 있음.
- **오류(담당자 아님)**: 남이 담당인 이슈 → 0단계에서 멈춤 · 보고.
- **오류(이슈 없음)**: 「공모전 찾아서 기능 구현해줘 · 브랜치도 알아서」 → 조사 · 브랜치 없음 · 「깃헙 이슈를 만들 수 있도록 도와드릴게요」 + 템플릿 칸 질문. 기대: 웹 검색 · `git switch -c` 호출 0 · 실수로 브랜치를 따려 하면 guard 차단(trace 에 `GuardBlocked`). 「같이 기획하자」와 다름 — 거기서는 사람이 무엇을 만들지 이미 정했고 AI 는 후보 · 제약만 냄.
- **오류(guard · 기능명/main)**: builder 가 `git push origin my-feature/main` → guard 가 막음(이미 있음) → impl 브랜치로 푸시 · PR.
- **정상(두 영역 · 2인 병렬)**: A 「#3 진행」(server 로그인) · B 「#4 진행」(app 로그인 화면). 둘 다 스펙 PR → 상대가 서로 머지. A 의 서버 PR 먼저 머지 → B 「#4 이어서」 0단계가 develop 당김 → `check_api_contract.py` 가 계약 변경을 잡음 → app-builder 수정 → 녹색 → PR. 기대: 계약 파일을 손으로 고친 커밋 없음.
- **충돌**: 둘이 `libs.versions.toml` 을 올림 → rebase 충돌 → builder 가 둘 다 살리고 높은 판 → 빌드 · 센서 → PR. 기대: 충돌 해소 커밋이 보고서에 적힘.
- **두 OS**: 윈도우(B) 커밋에 CRLF → `check_repo_hygiene.py` 가 CI 에서 잡음 → `git add --renormalize` → 녹색.
- **재개**: 스펙 PR 대기 중 세션 종료 → 다음 날 「#5 이어서」 → `state.md` + `gh pr view` → 머지됐으면 2단계부터.
- **오류(스펙 모순)**: structure-reviewer 가 「스펙엔 좌표 전송 · 규칙엔 금지」 → 1단계 재실행(고칠 이유 전달) → 스펙 수정 PR → 재승인 → 구현 계속.
- **오류(사람 몫)**: builder 가 compileSdk 플랫폼 부재로 막힘 → 보고서 「남은 것」에 Android Studio 설치 절차 → 사용자 보고 · 종료 → 설치 뒤 「#5 이어서」.
- **오류(guard)**: builder 가 rebase 뒤 `git push -f` → guard 가 막음(trace 에 `GuardBlocked`) → builder 가 `--force-with-lease` 로 다시 → 통과. 기대: 이슈 댓글 「활동」 줄에 guard 차단 1.
- **기록**: 이슈 하나를 끝까지 → `trace.jsonl` 에 builder · 리뷰어 줄 · `.claude/log/<n>-…/log.md` 에 절 4개(1 · 2 · 4 · 6) + `trace.md` · 이슈에 `[하네스]` 댓글 5개(1 · 2 · 4 · 6 · 마무리) · PR 본문에 활동 로그 요약.
- **정상(하네스 피드백)**: 「이슈 #14 진행해줘」(`[하네스 피드백]`) → `harness/14-<짧은이름>` → `harness:evolve` → 시험 · 센서 → ADR · log → PR(→ develop · `Closes #14`).
- **오류(한도)**: 리뷰 중 사용량 한도 → 재시도 없이 `state.md` 에 「3단계 pleos-reviewer 미완 · 해제 HH:MM」 → 보고.

## 후속 요청

- 「#n 스펙만 다시 · 보완」 → 1단계만(기존 파일 + 고칠 이유). 「#n 리뷰 다시 · 재리뷰」 → 3단계만. 「#n 센서 돌려」 → 5단계 1번만. 「#n PR 올려」 → 6단계만. 「#n 이어서」 → `state.md` 다음 단계. 「#n 처음부터」 → 폴더 보관 뒤 0단계.
- 「이전 결과 개선 · 업데이트」 → 어느 산출물인지 되묻지 않고 `state.md` 의 마지막 산출물로 판단 · 애매하면 한 줄로 확인.

## 첫 이슈 권장(#1 「환경 · 빌드 · CI 녹색」 · 스펙 생략 가능)

builder 가 하는 것: 앱 — core 판 결정(걸림 ①) · Compose · Hilt(KSP) · `MainActivity` · 매니페스트(센서 통과) · Konsist 테스트 · `assembleRelease`. 서버 — `uv init` · FastAPI `/health` · Alembic 초기화 · `docker-compose.yml` · `export_openapi` · `pleos_auth_stub` 뼈대 · import-linter 계약. 공통 — `.github/workflows/ci.yml`(`team-workflow` 9절) · `docs/setup.md` 빈칸 채움 · `doctor.py` 두 OS 녹색. 성공 기준: CI 세 잡 녹색 · 서명 없는 APK 아티팩트.
