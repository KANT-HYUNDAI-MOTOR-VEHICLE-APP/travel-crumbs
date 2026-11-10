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

## 2 실제 음성 시험 · Gleo 보고서 — 2026-11-10 · errorn
- 산출물: `docs/gleo/gleo-voice-integration.md`(실측 31 발화 · 지표 · 실패/예외 E1~E9 · 말하기 가이드 · 남은 시험 T-A~T-I · 결정 Q1~Q3) · 수정 `c37daaa`(빈 region · 추천 설명 · invokeWord)
- 지표: 성공 7건 말 끝 → Gleo 수락 중앙값 1.97초(Gleo 판단 1.42 · 앱 0.045 · Gleo 수락 0.50). 콜백 거절 0.
- 막힘: 일반 여행 추천 질문은 Gleo 가 웹 검색으로 가져감(앱에서 강제 불가) · 콜드 부팅 직후 위치 조회 10초로 앱 7초 기한 초과 — 사람 결정 Q1 · Q2

## 2 반복 · 콜드 · 앱 열기 시험과 PDF 보고서 — 2026-11-10 · errorn
- 반복 시험 36 발화: 기록 · 오늘 기록 · 경로 추천 · 지역 경로 추천(경주 · 속초) · 멈춤 · 재개 · 종료 모두 성공 · 콜드 기록 3회 약 3.0초 · 「트래블 크럼스 앱 열어줘」로 OPEN 없이 앱 실행(U4 해결)
- 최종 지표: 발화 67 · 우리 함수 성공 30 · 켜짐 중앙값 2.04초(p95 2.34) · 콜드 3.05초 · 콜백 거절 0
- 발견: 첫 음절 잘림(「경주」 → 「주」)으로 지역 잃음 · 영어 앱 이름 받아쓰기 오류 — 발음 가이드 · 유도 아이디어 I1~I8 로 정리
- 산출물: `docs/gleo/gleo-voice-report-20261110.pdf`(7쪽) · `docs/gleo/gleo-voice-integration.md` 갱신
