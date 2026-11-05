# .claude — travel-crumbs 하네스

하네스 설정은 전부 이 폴더 아래. 이 파일은 사람이 읽는 안내서 — 규칙의 원본은 각 스킬. 진입점은 저장소 루트의 `CLAUDE.md`.

## 1. 폴더 역할

| 경로 | 역할 |
|---|---|
| `agents/` | 에이전트 정의 넷. 누가 무엇을 하고 무엇을 못 하는지 |
| `agents/spec-writer.md` | 이슈 → 스펙(화면 · 상태 · 데이터 · API 계약 · 시험 케이스). 코드는 안 건드림 |
| `agents/builder.md` | 코드를 고치는 유일한 역할(app · server 영역마다 하나) |
| `agents/pleos-reviewer.md` | Pleos 심사 가이드 5항목 점검 · 읽기 전용 |
| `agents/structure-reviewer.md` | 구조 규칙(MVVM · FastAPI) 점검 · 읽기 전용 |
| `skills/` | 규칙 · 절차의 원본. 에이전트가 일 시작 전에 읽음 |
| `skills/app-orchestrator/` | 이슈 하나를 스펙 → 구현 → 리뷰 → 센서 → PR 까지 끌고 가는 순서 |
| `skills/team-workflow/` | 이슈 · 브랜치 · 커밋 · PR · 충돌 · 두 사람 협업 규칙 |
| `skills/android-architecture/` · `server-architecture/` | 앱 · 서버 구조 규칙 |
| `skills/pleos-vehicle-sdk/` | 차량 신호 · 주행 규제 · TTS 다루는 법 |
| `skills/pleos-review-checklist/` | 심사 점검 질문 |
| `skills/sensors/` | 계산형 검사(린트 · 테스트 · 위생 · 계약 대조). `scripts/` 에 검사 스크립트 |
| `guardrails/` | AI 작업의 울타리 — 규칙 밖으로 못 나가게 막고(guard) 무엇을 했는지 남김(trace). Claude 가 도구를 쓸 때 자동으로 돎 |
| `guardrails/guard.py` | 명령 실행 직전 차단기 — main · develop 푸시 · 강제 푸시 · 머지 · 이슈 없는 작업 브랜치 금지. 리뷰어에게는 읽기 전용 모드로 붙음 |
| `guardrails/trace_log.py` · `trace_report.py` | 활동 로그 자동 기록(`_workspace/<이슈>/trace.jsonl`) · 요약표(→ `log/<이슈>/trace.md`) |
| `guardrails/tests/` | 위 스크립트 시험 |
| `log/` | 이슈별 AI 작업 기록(커밋 · 공유). 규칙은 `log/README.md` |
| `settings.json` | 팀 공용 설정 — 권한 금지 목록(deny) · guardrails 등록. 스크립트 경로를 바꾸면 여기도 함께(안 그러면 모든 명령이 막힘) |
| `settings.local.json` | 개인 설정(gitignore) — 사람마다 다른 허용 항목. 공용 deny 는 못 풂 |

**금지 규칙은 두 겹**: `settings.json` 의 deny = 정해진 글자 꼴의 명령 · 파일 접근을 단순하고 확실하게 막음. `guardrails/guard.py` = 명령을 뜯어보고 상황까지 따져 우회 꼴 · 조건부 금지를 막음. 금지를 더할 땐 파일 접근은 deny, 우회 꼴 · 상황 판단이 필요하면 guard.

**실패 방향은 장치마다 고정**: 차단기(`guard.py`)는 **자기가 터져도 닫힘** — 검사에 실패하면 종료 코드 2 로 막음. 기록기(`trace_log.py`)는 터져도 작업을 막지 않음. Claude Code 는 종료 코드 2 만 차단으로 읽고 나머지는 「비차단 오류」로 보아 명령을 그대로 실행하므로, 차단기가 0 이나 1 을 돌려주면 울타리가 조용히 비게 됨(ADR 0012). `settings.json` 의 `onFailure: block` 은 Claude Code 2.1.295 이상에서만 도는 바깥쪽 한 겹일 뿐, 보장은 `guard.py` 쪽에 있음.

**이 폴더 밖의 관련 위치**

| 경로 | 무엇 |
|---|---|
| `CLAUDE.md` | 진입점 — 어떤 요청에 어떤 스킬을 쓰나 |
| `.github/ISSUE_TEMPLATE/` | 이슈 템플릿(기능 · 버그 · 하네스 피드백) = 하네스의 입력 |
| `docs/spec/<기능명>/` | 사람 기획 + AI 산출물(`artifacts/`) — 2절 |
| `docs/api/openapi.json` | API 계약(서버가 생성) |
| `docs/architecture_decision_record/` | 하네스 · 구조 결정 기록 = 하네스 변경 이력 |
| `_workspace/` | 실행 중간 산출물 · 개인용(gitignore) |

**하네스를 바꾸는 법**: `[하네스 피드백]` 이슈 → 「이슈 #n 진행해줘」 → `harness/<n>-<짧은이름>` 브랜치(develop 에서) → `harness:evolve` → PR → 상대 승인 · 머지. 결정은 `docs/architecture_decision_record/` 에 한 편 · 작업 기록은 `log/`. 직접 프롬프트로 바꾸지 않음(처음 환경 세팅 기간의 세팅 담당자만 예외).

## 2. 기획 · 스펙 문서

기능 하나 = `docs/spec/<기능명>/` 폴더 하나. 사람이 기획을 쓰고(폴더 안 아무 파일 · AI 와 같이 써도 됨 · 커밋은 사람), AI 는 이슈를 받아 `artifacts/` 아래에 스펙을 씀.

- 이슈 「참고」에 `기획: <기능명>/main · docs/spec/<기능명>/`(또는 그 아래 위치) → spec-writer 가 그 아래 문서를 전부 읽음.
- 이슈 「산출물」에 파일 경로 → 그 파일에 씀. 비우면 AI 가 이름을 제안하고 사람이 확인.
- 기능명 · 파일 이름은 영문 소문자 · 숫자 · `-`. 기획 문서는 사람이 `<기능명>/main` 브랜치를 만들어 이슈보다 먼저 올림(`team-workflow` 2절).
- 이슈 번호 전의 기획도 AI 와 같이 할 수 있음 — 「같이 기획하자」로 들어가는 기획 흐름(`team-workflow` 2절 「사람 ↔ AI 공동 기획」 · 오케스트레이터 「기획 흐름」). AI 는 제약 알리기 · 후보 표 · 초안 받아쓰기까지, 고르기 · 커밋은 사람. 과정은 `_workspace/_planning/<기능명>/`.
- 형식(md · PDF · 그림 · pptx 는 PDF 로)과 자세한 규칙의 원본: `docs/spec/README.md`.
