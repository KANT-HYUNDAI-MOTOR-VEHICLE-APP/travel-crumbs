# #21 [기능] 앱 뼈대를 와이어프레임 v5 · 데이터 표준 v0.3 에 맞춤
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/21
- 종류: 기능
- 담당: 노희주
- 브랜치: app-shell-v5/main · spec · impl

## 기획
- 기획: `docs/spec/app-shell/` · 과정 `_workspace/_planning/app-shell/`
- 기준 문서: 데이터 표준정의서 v0.3 · ERD v0.3 · 통합 와이어프레임 A4 v5(2026-11-06 팀 미팅)

## 1단계 스펙 끝 — 2026-11-07 15:40 · 노희주
- 산출물: `docs/spec/app-shell/artifacts/app-shell-v5-align.md` · spec PR(→ app-shell-v5/main)
- 에이전트: spec-writer × 1(첫 작성 뒤 사람 결정 반영 · 주행 중 레일 정정을 이어서 받음)
- 센서: 해당 없음(스펙 단계) · doctor FAIL 0
- 리뷰 지적: 없음
- 막힘: 없음 · 이웃 작업 겹침(navigation/main · PR #20 이 RecordingStatus.PAUSED 사용) — D28 로 머지 순서 정함
- 사람 결정: 이슈 작성 때 4개(D20 ~ D25) · 스펙 뒤 D26 임시 「새 여행 시작」 · D27 `UnfinishedInput` · D28 navigation/main 먼저 머지 · D29 주행 중 레일 보임 · 눌림(처음 「없음 유지」에서 바꿈) · 남은 미확정 N3 · N4 · N7 · N8 · N10 · N11 · N12 · N14 ~ N16 · N18 ~ N20 은 PR 「남은 결정」
- 다음: 사람이 spec PR 확인 · 머지 → 2단계 구현(navigation/main develop 머지 뒤 rebase)
- 활동: 도구 호출 54 · 실패 0 · guard 차단 0
