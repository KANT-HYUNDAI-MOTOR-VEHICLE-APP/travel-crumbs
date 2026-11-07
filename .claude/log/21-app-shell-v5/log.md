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

## 2단계 구현 끝 — 2026-11-07 16:20 · 노희주
- 산출물: aeb4a21 feat(app) — 레일 5칸 · 주행 중 레일 · 주행 화면 · 여행 종료 팝업 · TripStatus · CurrentTrip · UnfinishedInput · 홈 「설정」 · 「새 여행 시작」
- 에이전트: builder(app-builder) × 1
- 센서: gradle lint · 단위 테스트 106 · assembleRelease 녹색 · 매니페스트 · 비밀 · 위생 · 프런트매터 위반 0 · 가드 시험 23 통과
- 리뷰 지적: 해당 없음
- 막힘: 없음 · navigation 쪽 PAUSED 사용처는 D28 대로 rebase 때
- 사람 결정: 없음
- 다음: 리뷰 2관점
- 활동: 생략(PR 때 trace.md)

## 4단계 수정 끝 — 2026-11-07 17:05 · 노희주
- 산출물: cf5a416 fix(app) 레일 글자 · 아이콘 5mm 기준 · 에뮬레이터 실측 7단계(V16 · V45 포함) 통과 · 스크린샷 12장(개인 폴더)
- 에이전트: pleos-reviewer × 1 · structure-reviewer × 1 · app-builder 이어서 × 2(실측 · 레일 크기)
- 센서: 단위 테스트 107 · 나머지 같음 · 전부 녹색
- 리뷰 지적: 반려 0 · 구조 위반 0 · 확인 6(주행 중 팝업 · 주행 중 레일 · 가독성 실측 · 팝업 바깥 누름 실측 · 재생성 뒤 팝업 · ViewModel init 구독 예외) — 실측 2건 해소
- 막힘: 에뮬레이터 스냅샷 부팅 offline → 콜드 부팅으로 해결
- 사람 결정: 레일 글자 3.8mm → 이번 PR 에서 5mm 이상으로(5.4mm)
- 다음: impl PR
- 활동: 생략(PR 때 trace.md)

## 6단계 PR — 2026-11-07 17:10 · 노희주
- 산출물: impl PR(app-shell-v5/impl → app-shell-v5/main)
- 센서: 전부 녹색(위 4단계와 같음)
- 남은 결정: PR 본문 「남은 결정」
- 다음: 사람이 impl PR 확인 · 머지 → 7단계 develop PR(navigation/main 먼저 머지 · D28)
- 활동: 기간: 2026-11-07T15:16:42+09:00 → 2026-11-07T17:08:23+09:00 · 세션 1 · 도구 호출 213 · 실패 4 · guard 차단 0
