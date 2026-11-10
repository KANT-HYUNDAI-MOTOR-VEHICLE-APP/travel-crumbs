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

## 1단계 스펙 수정 — 2026-11-09 · 노희주
- 산출물: `docs/spec/app-shell/artifacts/app-shell-v5-align.md`(D30 ~ D32 · V47 ~ V66 · N21 ~ N25) · 스펙 수정 PR(→ app-shell-v5/main)
- 에이전트: spec-writer 이어서 × 2(v0.4 반영 · 출처 경로와 팀 공지 반영)
- 센서: 해당 없음(스펙 단계)
- 리뷰 지적: 없음
- 막힘: 첫 구현(PR #23) 머지 뒤 데이터 표준 v0.4(김지수 · 11-08 · gleo-ai/main)가 TRIP_STATUS 에 PAUSED 를 다시 넣음 · develop docs/data 는 아직 v0.35
- 사람 결정: v0.4 따라 PAUSED 되살림(D30) · 주행 큰 버튼 4개 「이 장소 기록 · 음성 · 기록 일시정지 · 여행 종료」(D31 · 디자인 시스템 9절) · 레일은 이번에 안 바꿈(D32) · 레일 기준은 지수님께 문의 중(N21 · 공유 방식은 #16 대로라는 팀 공지 반영)
- 다음: 스펙 수정 PR 머지 → app-shell-v5/impl 다시 따서 구현
- 활동: 생략(PR 때 trace.md)

## 2단계 구현(수정분) 끝 — 2026-11-09 · 노희주
- 산출물: c2f5cfd feat(app) — TripStatus 다섯 값(PAUSED) · 주행 큰 버튼 4개 · 일시정지 중 기록 시간 멈춤 · togglePause · 이전 저장 값 읽기 · 에뮬레이터 실측(screens 13a ~ 19)
- 에이전트: builder(app-builder-v04) × 1
- 센서: gradle lint · 단위 테스트 136 · assembleRelease 녹색 · 매니페스트 · 비밀 · 위생 · 프런트매터 위반 0
- 리뷰 지적: 해당 없음
- 막힘: 없음
- 사람 결정: 없음
- 다음: 리뷰 2관점
- 활동: 생략(PR 때 trace.md)

## 4단계 수정(수정분) 끝 — 2026-11-09 · 노희주
- 산출물: 코드 수정 없음
- 에이전트: pleos-reviewer × 1 · structure-reviewer × 1
- 센서: 위와 같음
- 리뷰 지적: 반려 0 · 구조 위반 0 · 확인 7(주행 버튼 4개 · 일시정지 즉시 적용 · 주행 중 팝업 2회 · 대비 · 응답 미측정 2회 · 재생성 뒤 팝업 · 주행 중 레일 · 옛 저장 키 지울 시점) — 센서 후보 2(주행 중 팝업 모양 UI 시험 · 테마 색 대비 계산)
- 막힘: 없음
- 사람 결정: 없음
- 다음: impl PR
- 활동: 생략(PR 때 trace.md)

## 6단계 PR(수정분) — 2026-11-09 · 노희주
- 산출물: impl PR(app-shell-v5/impl → app-shell-v5/main)
- 다음: 사람이 impl PR 확인 · 머지 → 레일(N21) 결정 → 7단계 develop PR(navigation/main 먼저 · D28)
- 활동: 기간: 2026-11-07T15:16:42+09:00 → 2026-11-09T10:09:38+09:00 · 세션 1 · 도구 호출 334 · 실패 6 · guard 차단 0

## 1단계 스펙 수정(레일 4칸) — 2026-11-09 · 노희주
- 산출물: `docs/spec/app-shell/artifacts/app-shell-v5-align.md`(D33 ~ D38 · V67 ~ V76 · N26 ~ N29) · 스펙 수정 PR(→ app-shell-v5/main)
- 에이전트: spec-writer 이어서 × 1
- 센서: 해당 없음(스펙 단계)
- 리뷰 지적: 없음
- 막힘: 없음
- 사람 결정: 사용자 흐름 두 개(① 기록 → 로드맵 · 태그 → 저장 · 공개 ② 추천 → 지역 → 안내) · 레일 4칸 「기록 · 내 여행 · 추천 · 설정」(팀 슬랙 공유) · 「기록」이 첫 화면 · 홈 칸 · 받은함 · 공유 메뉴 없음 · 설정은 레일 칸 · 디자인 시스템은 이번에 레일 시각 규칙만(전체 적용은 별도 이슈) · 지수님 답 「디자인 시스템 레일 목록은 임시」
- 다음: 스펙 수정 PR 머지 → app-shell-v5/impl 다시 따서 구현
- 활동: 생략(PR 때 trace.md)

## 2단계 구현(레일 4칸) 끝 — 2026-11-09 · 노희주
- 산출물: 47c2ea5 feat(app) — 레일 4칸 「기록 · 내 여행 · 추천 · 설정」 · 기록 칸 두 모양(「여행 시작」 · 「최근 여행이 없어요」 · 「추천 보러 가기」 / 진행 중) · 홈 「설정」 버튼 없앰 · 레일 시각 규칙(디자인 시스템 10절 · 토큰 3개 `RailColors`)
- 에이전트: builder(app-builder-v04) 이어서 × 1
- 센서: gradle lint · 단위 테스트 142 · assembleRelease 녹색 · 매니페스트 · 비밀 · 위생 · 프런트매터 위반 0
- 리뷰 지적: 해당 없음
- 막힘: 없음
- 사람 결정: 없음
- 다음: 리뷰 2관점
- 활동: 생략

## 4단계 수정(레일 4칸) 끝 — 2026-11-09 · 노희주
- 산출물: ff387ae fix(app) 레일 투명 색 한 표기 · 에뮬레이터 실측 V31 · V32 · V62(프로세스 재생성 뒤 복원) 통과 · 레일 전환 최장 32ms · 대비 17.7:1 · 7.9:1
- 에이전트: pleos-reviewer × 1 · structure-reviewer × 1 · builder 이어서 × 1(사용량 한도로 중단 → 사람이 다른 도구로 이어서 마침)
- 센서: 전부 녹색
- 리뷰 지적: 반려 0 · 구조 위반 0 · 확인 6(주행 중 레일 눌림 3회째 → 센서 후보 · 주행 팝업 · 버튼 4개 · RailColors 와 Theme 색 체계 두 갈래 → 색 토큰 이슈에서 흡수 · 투명 색 두 표기 → 고침)
- 막힘: 사용량 한도(메인 · builder) — 사람이 이어서 진행
- 사람 결정: 없음
- 다음: impl PR

## 6단계 PR · 7단계 develop PR(레일 4칸) — 2026-11-09 · 노희주
- 산출물: PR #32(app-shell-v5/impl → app-shell-v5/main · MERGED 71f1538) · PR #30(app-shell-v5/main → develop · MERGED bc54e19 · Closes #21)
- 센서: 머지 뒤 develop CI app 잡 실패 1회(Robolectric 안드로이드 파일 내려받기 `MavenArtifactFetcher` — 코드 무관) → 실패 잡 다시 실행 녹색
- 막힘: D28(navigation/main 먼저) 순서는 지키지 못함 — navigation/main 이 develop 에 들어올 때 `PinRepository` 등 `RecordingStatus` → `TripStatus` 고칠 일이 성호 쪽에 생김
- 다음: 원격 `app-shell-v5/main` · `app-shell/main` 사람 삭제 · 디자인 시스템 전체 적용은 별도 이슈
- 활동: 기간: 2026-11-07T15:16:42+09:00 → 2026-11-09T10:55:47+09:00 · 세션 1 · 도구 호출 424 · 실패 6 · guard 차단 0
