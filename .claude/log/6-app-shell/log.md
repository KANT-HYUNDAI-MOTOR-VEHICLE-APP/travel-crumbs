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
