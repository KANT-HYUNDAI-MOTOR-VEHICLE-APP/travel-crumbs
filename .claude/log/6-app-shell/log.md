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

## 1단계 스펙 수정 — 2026-11-05 · 노희주
- 산출물: `docs/spec/app-shell/artifacts/app-shell.md`(D18 · D19 · T38) · 스펙 수정 PR(→ app-shell/main)
- 에이전트: spec-writer × 1(구현 리뷰에서 나온 스펙 모순 · 팀 결정 변경)
- 센서: 해당 없음(스펙 단계)
- 리뷰 지적: 심사 #1 — landscape 고정(P3) ↔ 창 1개 영역 요구(D8 · T26 · T27) 모순(실기기에서 Pleos 가 「창 크기를 넓게」 안내)
- 막힘: 없음
- 사람 결정: D18 landscape 유지 · 창 2개 영역 전제 · D19 팀 서버 사용(#4 부터) · 이 이슈는 서버 안 씀 · INTERNET 유지
- 다음: 스펙 수정 PR 머지 → impl rebase · 재리뷰
- 활동: 생략(PR 때 trace.md)

## 2단계 구현 끝 — 2026-11-05 · 노희주
- 산출물: app-shell/impl c8a09bf(파일 63 · 앱 틀 · 주행 전용 화면 · 홈 구역 자리 · 기록 상태 · 임시 저장 · 주행 판정 합성) · sync PR #11(develop 받기)
- 에이전트: builder(app-builder) × 1
- 센서: gradle lint · 단위 테스트 70 · assembleRelease 녹색 · check_manifest 빨강 2(하네스 #2) · secrets · hygiene · frontmatter 녹색 · 에뮬레이터 T1 · T5 · T8 · T14 · T17 · T19 · T29 · T33 · T32(CPU ~0.02% · PSS 77MB)
- 리뷰 지적: 해당 없음(3단계에서)
- 막힘: 스펙 모순 — landscape 고정 ↔ 창 1개 영역 요구(실기기 Pleos 안내) · 사람 결정 필요 · 매니페스트 센서(하네스 #2)
- 사람 결정: 없음(다음 절)
- 다음: 3단계 리뷰
- 활동: 아래 6단계 절

## 4단계 수정 끝 — 2026-11-05 · 노희주
- 산출물: b0b1ede(1바퀴) · 9d82138(2바퀴) · 스펙 수정 PR #12(D18 · D19)
- 에이전트: pleos-reviewer × 2 · structure-reviewer × 2 · builder × 2바퀴 · spec-writer × 1
- 센서: 각 바퀴 gradle 녹색(테스트 71) · check_manifest 빨강 2 그대로
- 리뷰 지적: 심사 확인 4 + 2(해소 3 · 사람 몫 3) · 구조 확인 4 + 2(해소 3 · 범위 밖 3) · 반려 · 위반 0 · 센서 후보 없음(규칙 6 STOP_TIMEOUT 은 한 바퀴에 해소)
- 막힘: 없음
- 사람 결정: landscape 유지 · 창 2개 영역 전제(D18) · 팀 서버 사용 · 이 이슈는 서버 안 씀 · INTERNET 유지(D19)
- 다음: 센서 · impl PR
- 활동: 아래 6단계 절

## 6단계 PR — 2026-11-05 · 노희주
- 산출물: impl PR(app-shell/impl → app-shell/main)
- 에이전트: 없음(메인)
- 센서: gradle lint · 테스트 71 · assembleRelease 녹색 · check_manifest 빨강 2(NAVI_ROUTE · distractionOptimized — 하네스 #2) · secrets · hygiene · frontmatter · guardrails 시험 19 녹색
- 리뷰 지적: 남은 결정 7건(_workspace/6/05_open_findings.md → PR 본문)
- 막힘: 매니페스트 센서 — 하네스 #2(성호) · sync PR #11 · 스펙 수정 PR #12 머지 뒤 rebase 필요
- 사람 결정: 없음
- 다음: 사람이 #11 · #12 머지 → rebase · 센서 → impl PR 확인 · 머지 → 7단계 develop PR
- 활동: 세션 1 · 도구 호출 236 · 실패 3 · guard 차단 0
