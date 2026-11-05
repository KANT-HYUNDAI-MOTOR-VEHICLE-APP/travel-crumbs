# #6 [기능] 앱 뼈대 — 화면 구조 · 주행 / 정차 모드
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/6
- 종류: 기능
- 담당: 노희주
- 브랜치: app-shell/main · spec · impl

## 기획
- 기획: `docs/spec/app-shell/` · 과정 `_workspace/_planning/app-shell/`

## 1단계 스펙 끝 — 2026-11-04 17:49 · 노희주
- 산출물: `docs/spec/app-shell/artifacts/app-shell.md` · spec PR(→ app-shell/main)
- 에이전트: spec-writer × 2(첫 작성 · 미확정 답 반영 재실행)
- 센서: 해당 없음(스펙 단계) · doctor FAIL 1(Docker — 서버 없음 결정으로 무관 · PYTHONUTF8=1 필요, 하네스 피드백 #3)
- 리뷰 지적: 없음
- 막힘: 선행 조건 P1 · P2(DO 선언 · NAVI_ROUTE 가 매니페스트 센서에 걸림) · P3(landscape ↔ SDK 스킬) — 하네스 피드백 #2 몫
- 사람 결정: 미확정 U1~U15 · P1~P3 「추천대로 확정」 · 새 미확정 N1 · N2 는 PR 「남은 결정」
- 다음: 사람이 spec PR 확인 · 머지 → 2단계 구현(하네스 피드백 #2 반영 뒤 센서 녹색 가능)
- 활동: 도구 호출 33 · 실패 0 · guard 차단 0

## 1단계 스펙 보완 — 2026-11-05 · 노희주
- 산출물: `docs/spec/app-shell/artifacts/app-shell.md`(D16 · D17 · T36 · T37) · PR #7 갱신
- 에이전트: spec-writer × 1(이웃 이슈 #4 와 겹치는 지점 반영)
- 센서: 해당 없음(스펙 단계)
- 리뷰 지적: PR #7 성호 코멘트 — 주행 중 길안내 · 주행 판정 · 홈 순서 · SDK 카드 · #4 서버
- 막힘: PR #7 `ci / changes` 실패 — app-shell/main 이 develop 의 CI 권한 수정(6fb9607 · PR #1) 전에 따짐 · 구현 PR 전 sync 필요
- 사람 결정: D16 주행 화면에 길안내 버튼 없음(주행 중 변경은 음성) · D17 정차 홈 맨 위 기록 · 아래 다른 기능 구역
- 다음: 사람이 PR #7 확인 · 머지 → 2단계 구현(하네스 #2 반영 · app-shell/main sync 필요)
- 활동: 기록 생략(훅 요약은 PR 때 trace.md)
