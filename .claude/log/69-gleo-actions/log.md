# #69 Gleo 음성으로 여행 제어 · 오늘 기록 · 근처 추천 — 함수 3개 재구성
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/69
- 종류: 기능
- 담당: errorn
- 브랜치: gleo-actions/main · spec · impl

## 1 스펙 작성 끝 — 2026-11-10 · errorn
- 산출물: `docs/spec/gleo-actions/artifacts/gleo-trip-control-info.md` · spec PR
- 에이전트: Claude Code 메인 1(스펙 직접 작성 · 사용자가 함수 구성 「기록 · 여행제어 · 정보」를 미리 결정)
- 근거: Pleos App Schema(함수 최대 3개 · enum) 재확인, develop 의 RecordingRepository · TripRepository · RecommendationRepository · NaviRepository 대조, #68 실제 음성 로그(접두어 action · ACTION_PARAM)
- 센서: 문서만 변경
- 리뷰 지적: 없음(스펙 단계)
- 막힘: 없음 · 미확정 U1~U4 는 PR 「남은 결정」

## 1 스펙 PR #70 사람 결정 반영 — 2026-11-10 · errorn
- U1 release 는 SHOW_TRAVEL_INFO 만 · U2 종료는 팝업만 · U3 말한 지역 우선(「경주 근처」 → 경주시), 없으면 현재 위치 기준 가장 가까운 시군구 → `SHOW_TRAVEL_INFO` 에 선택 파라미터 `region` 추가 · U4 OPEN 빼고 진행, 실기 T17 확인
- 시험 케이스 T10 · T10a · T10b · T10c 로 나눔, T16 에 「경주 근처 추천 코스 알려 줘」 추가

## 2 구현 · 센서 — 2026-11-10 · errorn
- 산출물: `gleo-actions/impl` — 함수 3개(RECORD 유지 · CONTROL_CURRENT_TRIP · SHOW_TRAVEL_INFO) · OPEN 제거
  - data/gleo: `GleoTripControl`(현재 상태 확인 후 togglePause · END 는 팝업 요청만) · `GleoTravelInfo`(오늘 기록 · 추천 — 말한 지역 우선, 없으면 현재 위치 기준) · `GleoOutcome` · 파서 함수별 키 · enum 검증 · 프로세서 BUSY · 7초 기한 · writeEnabled(release 는 INFO 만)
  - domain: `GleoTravelRules`(지역 맞추기 · 가장 가까운 지역 · 오늘 장소 수) · `GleoScreenRequest` · Callback `data`
  - ui: 앱 틀이 정차 중에만 화면 요청을 따름(내 여행 · 지역 추천 · 종료는 기록 칸) · 추천 칸이 Gleo 지역 순위(B-02)부터 · 기록 칸 · 주행 화면이 종료 확인 팝업을 엶 · 안내 한 줄 문구 12개
  - schema: release = SHOW_TRAVEL_INFO · debug = 3개(`_workspace/gleo-actions/gen_schema.py` 로 생성)
- 에이전트: Claude Code 메인 1(직접 구현). 두 관점 리뷰는 PR 전에.
- 센서: 단위 452(새 23) 통과 · lint 오류 0 · debug/release 빌드 · release 스키마 · 매니페스트 SHOW_TRAVEL_INFO 만 · manifest · secrets · hygiene · frontmatter · data standard · guard 녹색
- 에뮬레이터(ADB · Gleo 형식 Intent): PAUSE → 일시정지 · PAUSE 다시 → already paused · RESUME · TODAY 1곳 · 「경주」 → 신라 유적 산책 + 추천 칸 경북 경주시 순위 · 「대구」 → 실패 · 지역 없음 → 서울 강남구 · END → 기록 칸 + 종료 확인 팝업 · STOP → 입력 실패 · OPEN 은 앱이 받지 않음
- 막힘: 실제 음성 T16 · T17 은 사람 발화 대기(두 번 기록 시도 동안 발화 없음)
