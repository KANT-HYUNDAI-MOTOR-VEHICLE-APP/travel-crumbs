# ADR 0007 — `.claude/hooks` → `.claude/guardrails`(2026-10-31 제안)

> 템플릿 주: 하네스 원본 저장소에서 내린 결정을 근거로 옮겨 온 기록. 본문의 이슈 · PR 번호와 날짜는 원본 저장소 것이며, 이 저장소의 새 결정은 다음 번호부터 `0000-template.md` 꼴로 씀.

## 결정

- 자동 실행 스크립트 폴더 이름을 `hooks` 에서 `guardrails` 로 바꿈. 안의 파일(`guard.py` · `trace_log.py` · `trace_report.py` · `tests/`)은 그대로.
- `settings.json` · 리뷰어 에이전트 둘 · 오케스트레이터 · sensors · team-workflow · `.claude/log/README.md` 의 경로를 함께 고침.
- 이미 머지된 ADR 0002 · 0003 의 옛 경로는 당시 기록이라 고치지 않음.

## 이유

| 무엇 | 이유 |
|---|---|
| 이름 변경 | `hooks` 는 「언제 도는가」(Claude Code 의 훅 장치)만 말하고 목적을 말하지 않음. agents · skills 처럼 이름만으로 무엇이 들었는지 알게 함(사용자 피드백) |
| `guardrails` | 안의 스크립트는 AI 가 규칙 밖으로 못 나가게 막고(guard) 무엇을 했는지 남기는(trace) 장치 — 둘 다 AI 작업의 울타리. 목적별로 `guard/` · `trace/` 로 나누는 안도 있었으나 한 폴더 유지를 택함 |

## 옮길 때 주의

- Claude Code 는 세션 시작 때 훅 설정을 읽어 둠. 세션 중에 폴더만 옮기면 옛 경로의 guard 를 못 찾아 모든 명령이 막힘(`onFailure: block`). 옮긴 세션에서는 옛 경로에 새 경로로 넘기는 임시 파일을 두고, 세션을 다시 시작한 뒤 지움(커밋하지 않음).
