---
name: team-workflow
description: travel-crumbs 저장소(<owner>/<repo> · 앱 + 서버 + 문서 + 하네스 한 저장소)에서 두 사람 이상이 같은 하네스로 작업하고 푸시 · 병합하는 규칙 — GitHub 이슈 티켓 · 브랜치 전략(develop · <기능명>/main · /spec · /impl · bug/) · 커밋 메시지 · 스펙 PR → 구현 PR → develop PR · rebase · squash 머지 · 충돌 핫스팟(버전 카탈로그 · Alembic · openapi.json · uv.lock · 매니페스트 · CI) 해소법 · 머지 뒤 할 일 · 맥 ↔ 윈도우 위생(LF · 대소문자 · 경로 길이 · Python 스크립트) · doctor · main 지키는 약속 · CI 잡 정의. 브랜치를 만들거나 커밋 · 푸시 · PR · rebase · 충돌 해소를 하기 전, 새 사람이 합류할 때, CI 를 쓰거나 고칠 때 반드시 읽는다. 코드 구조 규칙은 android-architecture · server-architecture.
---

# 두 사람이 한 저장소에서 — 협업 규칙

목표: 상대가 머지한 것이 내 작업을 깨지 않고, 내가 올린 것이 사람 눈을 거쳐 develop 에 들어가는 길이 늘 같음.

## 1. 티켓 = GitHub 이슈

- 이슈 템플릿 셋(`.github/ISSUE_TEMPLATE/`) — `harness.yml`(하네스 피드백: 대상 · 무슨 일이 있었나 · 바라는 동작 · 완료 조건) · `feature.yml`(새 기능 · 아이디어: 영역 · 누가 · 언제 · 왜 · 사용 장면 · 완료 조건 · 하지 않는 것 · 화면 · 데이터 · 스펙 생략) · `bug.yml`(버그 수정: 영역 · 재현 순서 · 기대 · 실제 · 빈도 · 환경 · 로그 · 심각도). 빈 이슈는 막음(`config.yml`).
- 만드는 길: GitHub 웹 · `gh issue create --template feature.yml` · `bug.yml` · 오케스트레이터에 글로 주면 초안을 보여 주고 `gh issue create`.
- 종류는 템플릿(제목 앞말)으로 가름 — `[기능]` → 기능 흐름(3절) · `[버그]` → 버그 흐름(3절) · `[하네스 피드백]` → 하네스 흐름(오케스트레이터 「하네스 피드백 흐름」).
- 하네스 변경도 이슈로: 하네스를 바꾸는 요청은 `[하네스 피드백]` 이슈로 함(직접 프롬프트 금지). 예외는 처음 환경 세팅 기간의 세팅 담당자뿐 — 그 작업도 `docs/architecture_decision_record/` 에 남김.
- 작업 기록: 이슈마다 `.claude/log/<n>-<짧은이름>/`(규칙 `.claude/log/README.md`). AI 활용 기록 공유 · 작업 방식 단일화가 이 하네스의 목적.
- 실행 조건: 담당자(Assignees)가 자기인 이슈만 자기 세션에서 진행(약속). 라벨은 쓰지 않음.
- 이슈 게이트: 이슈 없이 기능 · 버그 작업을 시작하지 않음. 무엇을 만들지는 사람이 이슈로 정하고, AI 는 칸 채우기를 도울 뿐 내용을 지어내지 않음. guard 가 `_workspace/current` → `state.md`(오케스트레이터 0단계가 만듦) 없이 `<기능명>/main · spec · impl · sync` · `bug/<n>-…` · `harness/<n>-…` 브랜치를 만드는 명령을 막음(번호 없는 `harness/<짧은이름>` 은 환경 세팅 기간에만 통과 · 세팅 담당자가 끝나면 guard 에서 지움). guard 는 흔적(`state.md` · 번호 일치)만 봄 — 이슈가 실제로 열려 있는지 · 담당자가 자기인지는 오케스트레이터 0단계의 `gh issue view` 확인과 약속으로 지킴(브랜치마다 네트워크 호출 · 오프라인에서 막힘을 피하려고 기계로 넣지 않음).

## 2. 브랜치 · 커밋

```
main ◀─ 릴리스 PR(사람) ─ develop ◀─ PR(사람) ─ <기능명>/main ◀─ 사람 기획 직접 커밋(docs/spec/<기능명>/ · 이슈보다 먼저)
                                │                                ◀─ PR(사람) ─ <기능명>/spec
                                │                                ◀─ PR(사람) ─ <기능명>/impl
                                │                                ◀─ PR(사람) ─ <기능명>/sync (develop 과 충돌할 때)
                                ◀─ PR(사람) ─ bug/<이슈번호>-<짧은이름>
                                ◀─ PR(사람) ─ harness/<이슈번호>-<짧은이름>
```

| 브랜치 | 어디서 따나 | 누가 푸시 | PR · 머지 |
|---|---|---|---|
| `main` | — | 아무도 | develop → main 릴리스 PR · 사람 |
| `develop` | main(처음 한 번) | 아무도 | 아래 브랜치들의 PR · 사람 |
| `<기능명>/main` | develop | 기획이 있으면 사람이 만들고 기획 문서만 직접 커밋 · 없으면 에이전트가 0단계에서 한 번만 — 에이전트는 그 뒤 guard 가 막음 | → develop · 여러 사람 피드백 뒤 사람 머지 |
| `<기능명>/spec` | `<기능명>/main` | 에이전트 | → `<기능명>/main` · 사람이 스펙 확인 · 머지 |
| `<기능명>/impl` | 스펙 머지 뒤의 `<기능명>/main` | 에이전트 | → `<기능명>/main` · 사람이 이 브랜치로 앱을 써 보고 구조 리뷰 · 머지 |
| `bug/<이슈번호>-<짧은이름>` | develop | 에이전트 | → develop · 사람이 재현 순서로 확인 · 머지 |
| `harness/<이슈번호>-<짧은이름>` | develop | 사람 · 에이전트 | → develop · 상대 승인(`.claude/` · `CLAUDE.md` 수정 · `[하네스 피드백]` 이슈). 번호 없는 `harness/<짧은이름>` 은 환경 세팅 기간에만 |

- **기능명**: 영문 소문자 · 숫자 · `-`. 기능 하나를 가리키는 짧은 명사 — `login` · `navigation` · `my-feature`. 에이전트가 이슈 제목에서 정하고 0단계 이슈 댓글에 남김. 같은 이름의 `<기능명>/main` 이 원격에 이미 있으면(다른 이슈) `<기능명>-<이슈번호>`.
- `<기능명>/main` 에는 spec · impl 이 PR 로만 들어옴. 예외는 사람 기획 문서 하나(아래 「기능 브랜치에서 기획」).

**기능 브랜치에서 기획** — 기획 · 스펙 · 코드가 한 기능 브랜치에서 함께 움직이고 기능이 끝날 때 함께 develop 으로 들어가게 함(기능 없는 기획이 develop 에 먼저 쌓이지 않게).

```
develop
 └─ login/main              ← 사람이 만들고 docs/spec/login/ 에 기획을 올림(AI 와 같이 써도 됨 · 커밋은 사람)
     ├─ login/spec          ← AI: 기획을 읽고 artifacts/ 에 스펙 → PR → login/main
     └─ login/impl          ← AI: 구현 → PR → login/main
login/main → develop        ← 기획 · 스펙 · 코드가 한꺼번에 develop 으로
```

1. 사람이 `<기능명>/main` 을 만듦 — GitHub 웹에서 develop 을 고른 채 브랜치 칸에 이름을 쳐도 되고(git 명령 없이 됨), 로컬에서 `git switch -c <기능명>/main origin/develop && git push -u origin <기능명>/main` 해도 됨. AI 는 이 브랜치를 만들지 않음 — 이슈 번호 전이라 guard 가 막고, 기획의 주인은 사람.
2. 그 브랜치에서 기획을 씀. 혼자 써도 되고 AI 와 같이 해도 됨(아래 「사람 ↔ AI 공동 기획」). 자리는 `docs/spec/<기능명>/` 아래 · `artifacts/` 밖만. 형식 · 이름 규칙은 `docs/spec/README.md`.
3. 사람이 커밋 · 푸시(웹이면 「Add file」 → 「Commit directly to the `<기능명>/main` branch」 · 로컬이면 경로 지정 커밋).
4. 이슈 「참고」에 `기획: <기능명>/main · docs/spec/<기능명>/`(브랜치 주소 링크를 같이 걸면 사람도 바로 봄).
5. 에이전트는 0단계에서 이 브랜치를 그대로 씀(새로 만들지 않음) → spec · impl 은 이 브랜치에서 따므로 기획 문서가 작업 트리에 함께 있음.
6. 기획이 바뀌면 같은 브랜치에서 같은 파일을 고침. 이미 따 둔 spec · impl 은 `git rebase origin/<기능명>/main` 으로 받음.

**사람 ↔ AI 공동 기획** — 이슈 게이트가 막는 것은 **결정과 산출**(무엇을 만들지 · 작업 브랜치 · 스펙 · 구현)이지 **선택지와 제약을 드러내는 일**이 아님. 그래서 기획은 이슈 번호 전에도 AI 와 같이 할 수 있음. 사람이 혼자 조사해 정하는 것보다, 이 저장소의 전제 · 규제를 먼저 알고 정하는 편이 낫기 때문.

| | 기획 단계에서 |
|---|---|
| AI 가 함 | 기술 수단 후보를 표로 냄(장단 · 이 저장소 전제와 맞는지) · 규칙 · 전제에서 오는 제약을 미리 알림(주행 규제 `pleos-vehicle-sdk` · LLM 경계 7 `server-architecture` · 개인정보) · 사람이 쓴 기획의 빈 곳 · 모순을 짚음 · 사람이 고른 내용을 기획 문서 꼴로 받아씀(작업 트리에 초안) |
| 사람이 함 | 무엇을 만들지 · 어느 후보를 고를지 · 화면 구성 결정 · 초안을 읽고 고쳐 커밋 · 푸시 |

- 들어가는 말: 「`<기능명>` 같이 기획하자」 · 「기획 도와줘」 · 「후보 좀 보여 줘」. 여기에는 이슈 번호를 되묻지 않음 — 되묻는 건 구현 · 작업 브랜치 · 스펙을 시켰을 때(1절 「이슈 게이트」).
- 사람이 고르지 않은 내용을 초안에 넣지 않음(빈 곳은 「미정」으로 남김) — 기획의 주인이 사람이어야 이슈가 사람의 의도를 담은 작업지시서가 됨.
- 기록: 결과는 `docs/spec/<기능명>/` 에 남음. 이슈 번호가 아직 없어 `.claude/log/` 를 쓰지 못하므로 그 전 과정은 `_workspace/_planning/<기능명>/`(개인 폴더 · gitignore)에 둠. 이슈가 생기면 그 이슈 `log.md` 첫 절에 「기획: `docs/spec/<기능명>/` · 과정 `_workspace/_planning/<기능명>/`」 한 줄로 이어 붙임.

- 에이전트는 `<기능명>/main` 에 직접 커밋 · 푸시하지 않음(기획 초안을 작업 트리에 쓰는 데까지만 · guard 가 푸시를 막음).
- 기획 없이 시작하는 기능은 전처럼 에이전트가 0단계에서 `<기능명>/main` 을 만듦.
- 지우기: spec · impl · bug 는 머지 뒤 에이전트가 로컬 · 원격 삭제. `<기능명>/main` 은 develop 머지 때 사람이 PR 화면의 「Delete branch」.

| 항목 | 규칙 |
|---|---|
| 커밋 메시지 | `type(area): 한국어 요약` — type = feat · fix · docs · test · refactor · chore / area = app · server · docs · harness · log. 본문에 이슈 `#n` |
| 커밋 범위 | 경로 지정 `git add <files> && git commit -- <files>` · 폴더째 add 금지 · 생성물(openapi.json · uv.lock · 리비전)은 그 변경과 같은 커밋 |
| 푸시 | 자기 작업 브랜치(spec · impl · bug · harness)만 · `--force-with-lease` 만(rebase 뒤) |
| 머지 | 사람만 · GitHub PR 화면의 「Squash and merge」. 에이전트는 `gh pr merge` 금지(deny · guard) |

## 3. PR 흐름

**기능(`[기능]` 이슈)**

1. 사람이 새 기능 · 아이디어 이슈를 만들고 담당자를 지정.
2. 담당자가 자기 세션에서 「이슈 #n 진행해줘」 → 에이전트가 시작.
3. `<기능명>/main` — 사람이 기획과 함께 이미 만들었으면 그대로 · 없으면 에이전트가 develop 에서 따서 푸시(처음 한 번).
4. `<기능명>/main` 에서 `<기능명>/spec` → 스펙 작성(`docs/spec/<기능명>/artifacts/<이름>.md` · 계약 초안 `docs/api/`) → PR → `<기능명>/main`.
5. 사람이 확인 · 머지 → **따로 지시가 없으면 에이전트가 바로** `<기능명>/main` 에서 `<기능명>/impl` 을 따서 구현 시작.
6. 구현 · 리뷰 · 센서가 끝나면 PR → `<기능명>/main`. 사람은 `<기능명>/impl` 을 받아 앱을 직접 써 보고(`git switch <기능명>/impl` → 빌드 · 설치) 구조를 리뷰.
7. 사람이 머지 → 에이전트가 `<기능명>/main` → develop PR 을 엶.
8. 여러 사람이 피드백 → 고칠 것은 새 `<기능명>/impl`(같은 이름 다시) → `<기능명>/main` PR. 최종 develop 머지(사람)로 끝.
9. 기획 · 스펙은 `docs/spec/<기능명>/` 한 폴더에 모음 — 사람 기획은 폴더 안 아무 파일 · AI 산출물은 `artifacts/` 아래만. 산출물 경로는 이슈 「산출물」 칸이 정하고, 비어 있으면 AI 가 제안해 사람이 확인(규칙 원본 `docs/spec/README.md`).

- 「스펙 생략」 표시가 있으면 4~5 를 건너뛰고 이슈 본문을 스펙으로 씀(`<기능명>/main` → impl 바로).
- 5 · 7 의 「바로」: 세션이 열려 있으면 PR 머지를 백그라운드로 기다렸다가 이어감. 세션이 닫혀 있으면 다음에 「#n 이어서」 → PR 상태를 보고 이어감.

**버그(`[버그]` 이슈)**

1. develop 에서 `bug/<이슈번호>-<짧은이름>` → 재현(테스트로 먼저) → 원인 → 수정 → 센서.
2. PR → develop. 본문 = 원인 · 고친 것 · 재현 순서대로 확인한 결과. 사람이 확인 · 머지.

**공통**

- PR 은 에이전트가 엶(`gh pr create --base <대상> --template`). 본문 = 이슈 링크(`Closes #n` 은 develop 으로 가는 PR 에만) · 스펙 링크 · 리뷰 표(두 관점) · 센서 결과(명령 · 결과) · 확인법(adb · curl 순서) · 「남은 결정」. 템플릿 `.github/pull_request_template.md`.
- PR 열기 전: `git fetch origin && git rebase origin/<대상 브랜치>` → 센서 전부 다시 → `git push --force-with-lease`.
- develop 이 앞서 나가 `<기능명>/main` → develop PR 이 충돌하면: `<기능명>/main` 에서 `<기능명>/sync` → `git merge origin/develop` → 충돌 해소 · 센서 → PR → `<기능명>/main`.
- 자기 PR 을 자기가 머지하지 않음(둘 뿐이라 한 명 부재 시엔 이슈에 적고 24시간 뒤 자기 머지 허용).
- 리뷰 코멘트는 PR 에서. 에이전트 리뷰 표는 본문에 붙여 투명하게.

## 4. 머지 뒤(다음 0단계)

`git switch develop && git pull --ff-only` → `python .claude/skills/sensors/scripts/doctor.py` → 센서 전부 → 빨간 것은 버그 이슈로. 작업 중 브랜치가 있으면 그 대상 브랜치로 `git rebase` → 센서.

## 5. 충돌 핫스팟과 해소법

| 핫스팟 | 피하는 법 | 충돌 났을 때 |
|---|---|---|
| `gradle/libs.versions.toml` | 한 PR 에 라이브러리 묶음 하나 · 판은 올리기만 | 둘 다 살리고 높은 판 → 빌드 |
| `server/alembic/versions/` | 스키마 PR 먼저 · 작게 · 머지된 리비전 수정 금지 | rebase 뒤 `uv run alembic heads` 둘 → `uv run alembic merge -m "merge" <a> <b>` → `upgrade head` → `alembic check` |
| `docs/api/openapi.json` | 서버만 만듦 · 앱은 읽기만 · 손으로 안 고침 | rebase 뒤 `uv run python -m app.tools.export_openapi ../docs/api/openapi.json` 재생성 |
| `server/uv.lock` | 의존성 추가는 작은 PR 로 | 손으로 안 고침 · rebase 뒤 `uv lock` 재생성 |
| `AndroidManifest.xml` · `strings.xml` | 권한은 스펙에 적힌 것만 · 문구는 화면별로 묶어 추가 | 둘 다 살림 → `check_manifest.py` |
| `di/*Module.kt` | 도메인별 파일로 쪼갬 | 둘 다 살림 → 빌드 |
| `.github/workflows/ci.yml` · `.claude/` · `CLAUDE.md` | `harness/…` 브랜치(develop 에서)로 따로 · 기능 PR 에 섞지 않음 | 상대와 이슈에서 조율 |
| `.claude/log/` | 예외 — 기록이라 각 이슈의 작업 브랜치에 함께 · 이슈마다 폴더 하나 | 없음(폴더가 다름) |
| `docs/spec/<기능명>/` | 사람 기획은 `<기능명>/main` 에만 · 산출물은 이슈마다 경로를 정해 spec 브랜치에서만 | 같은 산출물을 두 이슈가 고치면 뒤에 머지하는 쪽이 rebase 뒤 앞의 내용을 살려 다시 씀 |

## 6. 맥 ↔ 윈도우 위생

| 규칙 | 왜 |
|---|---|
| `.gitattributes`: `* text=auto eol=lf` · `*.bat text eol=crlf` · 이미지 · 폰트 · jar binary | 윈도우 커밋의 CRLF 가 맥에 가짜 diff 를 만듦 |
| 윈도우: `git config core.autocrlf false` · `git config core.longpaths true` · 저장소를 짧은 경로(`C:\dev\<repo>`) | 줄바꿈 이중 변환 · 260자 경로 |
| 스크립트는 전부 Python(`.sh` · `.ps1` 없음) · 명령은 Gradle(앱) · uv(서버) · Python | 두 셸에서 같은 글자 |
| 파일 이름은 대소문자만 다른 둘 금지 | 윈도우는 구분 못 함 |
| 심볼릭 링크 금지 · 경로 200자 안 | 윈도우 · Gradle · KSP |
| `gradlew` 실행 비트(100755) 유지 | 윈도우 커밋이 지우기 쉬움 → CI 가 잼(`check_repo_hygiene.py`) |
| `.editorconfig`: UTF-8 · LF · 들여쓰기(kt 4 · py 4 · json/yaml/md 2) | 편집기 차이 |
| Docker: 윈도우는 WSL2 백엔드 · 서버 폴더를 WSL 안에 두면 빠름 | 볼륨 성능 |

## 7. 환경 — 합류 절차

1. `docs/setup.md` 의 자기 OS 열 따라 설치(JDK = Android Studio JBR · SDK 플랫폼 · Pleos 이미지(OS 별 Update Site) · AVD `Pleos_Connect_v2.1.0` · Docker Desktop · uv · gh).
2. `python .claude/skills/sensors/scripts/doctor.py` 녹색(맥은 `python3`).
3. 저장소 폴더에서 Claude Code 세션 시작 → 폴더 신뢰 한 번(훅이 켜짐) → `docs/setup.md` 끝의 「훅 확인」.
4. 첫 이슈를 맡음 · 「이슈 #n 진행해줘」.

### 권한 장치(`.claude/settings.json` · 둘 다 같은 규칙)

| 겹 | 무엇 | 막는 것 |
|---|---|---|
| deny 규칙 | Claude Code 권한 | main · develop 푸시 · 강제 푸시 · `gh pr merge` · 저장소 삭제 · `.env` · 서명 키 읽기/쓰기 · `docs/api/openapi.json` 손 수정 |
| guard 훅 | `.claude/guardrails/guard.py` | deny 가 못 잡는 다른 꼴(`git -C . push origin main` · `+main` · `sh -c '…'` 안) · 이미 있는 `<기능명>/main` 푸시 · `<기능명>/main` 삭제 · `gh api` DELETE · 보호 설정 변경 |
| 리뷰어 읽기 전용 | 리뷰어 머리말 훅 `guard.py --readonly` | 파일 쓰기 · 커밋 · 브랜치 이동 · 센서 밖 스크립트 실행 |

- 사람이 직접 터미널에서 치는 명령은 막지 않음 — 사람 몫은 8절 약속.
- 막혔을 때 다른 꼴로 우회하지 않음. 규칙이 틀렸으면 `harness/…` PR 로 규칙을 고침.
- 규칙 · guard 를 고치면 `python3 .claude/guardrails/tests/test_guard.py` 통과까지.

## 8. main · develop 지키는 약속(보호 규칙 없음)

비공개 저장소 + 무료 플랜이라 GitHub 브랜치 보호 · 룰셋을 못 씀(API 403). 기계가 막지 않으니 사람과 에이전트가 같은 약속을 지킴.

| 약속 | 누가 확인 |
|---|---|
| main · develop · 이미 있는 `<기능명>/main` 에 직접 푸시 안 함 — 사람 · 에이전트 모두. 작업은 항상 작업 브랜치 → PR(2절). 예외: 사람이 `<기능명>/main` 에 기획 문서(`docs/spec/<기능명>/` 의 `artifacts/` 밖)를 올리는 것 | 푸시하는 사람 · 에이전트(guard 가 막음) |
| 머지는 GitHub PR 화면의 「Squash and merge」로만 | 머지하는 사람 |
| 머지 버튼 전 PR 화면에서 두 가지 확인 — CI 체크 녹색 · 상대 「Approved」 | 머지하는 사람 |
| 자기 PR 을 자기가 승인 · 머지하지 않음 | 둘 다 |
| main · develop 에 force-push · 삭제 안 함 | 둘 다 |

- 어겼을 때: 잘못 들어간 커밋은 `git revert` 커밋을 PR 로 올려 되돌림. force-push 로 지우지 않음 — 상대 로컬과 갈라짐.
- 다시 기계로 막으려면: 저장소 공개 전환 또는 GitHub Pro. 그때 저장소 관리자가 한 번 —

```bash
gh api -X PUT repos/<owner>/<repo>/branches/main/protection --input - <<'JSON'
{"required_status_checks":{"strict":true,"contexts":["app","server","common"]},"enforce_admins":false,"required_pull_request_reviews":{"required_approving_review_count":1,"dismiss_stale_reviews":true},"restrictions":null,"allow_force_pushes":false,"allow_deletions":false}
JSON
```
`contexts` 는 CI 잡 이름과 같게.

## 9. CI 잡 정의(`.github/workflows/ci.yml` — builder 가 첫 이슈에서 만듦 · 로컬 센서와 같은 명령)

| 잡 | 트리거(경로 필터) | 명령 |
|---|---|---|
| `app` | `app/**` · `gradle/**` · `*.gradle.kts` | JDK(JBR 과 같은 메이저) · `./gradlew lint testDebugUnitTest assembleRelease` · `python .claude/skills/sensors/scripts/check_manifest.py app/src/main/AndroidManifest.xml --gradle app/build.gradle.kts` · APK 아티팩트 |
| `server` | `server/**` | uv · `uv sync --frozen` · ruff · mypy · lint-imports · pytest(testcontainers — ubuntu 러너 Docker) · `alembic check` · `check_alembic_heads.py` · `check_sql_comments.py` · `check_routes.py` · `export_openapi` → `git diff --exit-code docs/api/openapi.json` |
| `common` | 항상 | `check_repo_hygiene.py` · `check_secrets.py` · `check_skill_frontmatter.py` · `check_api_contract.py docs/api/openapi.json app/src/main/java` · `python .claude/guardrails/tests/test_guard.py` · `python .claude/guardrails/tests/test_trace.py` · `python .claude/guardrails/tests/test_check_manifest.py` |

## 10. 하네스 자체의 변경

- `.claude/` · `CLAUDE.md` 수정은 develop 에서 딴 `harness/<짧은이름>` 브랜치 · PR(→ develop) · 상대 승인 — 둘이 같은 판을 써야 하므로.
- 회고(`harness:evolve`) 결과도 같은 길. 무엇을 왜 바꿨나는 `docs/architecture_decision_record/<번호>-<제목>.md` 하나로 남김 — `CLAUDE.md` 에는 변경 이력을 두지 않음(호출 조건 · 저장소 규칙만).
- 회고 자료: `.claude/log/<n>-…/`(저장소 · 모두 같은 자료) + 이슈 댓글 `[하네스]`(두 사람 공통 · `gh search issues --repo <owner>/<repo> "[하네스]" --json number` → `gh issue view <n> --comments`) + 각자의 `_workspace/<n>/trace.jsonl` 요약(`trace_report.py`).
