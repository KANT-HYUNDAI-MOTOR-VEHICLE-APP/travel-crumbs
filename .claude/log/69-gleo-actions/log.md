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

## 2 Gleo 브리핑 간결판 — 2026-11-11 · errorn
- `docs/gleo/gleo-voice-integration.md` · `gleo-voice-report-20261110.pdf`(2쪽)를 사람 요청대로 핵심만: 기능별 되는 말 · 안 된 말 · 이유, 「제주도 → 서울」 원인(첫 음절 잘림 + 빈 region + 에뮬레이터 고정 위치), 속도, 실험과 이유, DB 구축 참고(Gleo 가 읽고 쓰는 테이블 · 칸)
- 확인: region=제주도 → 제주 추천 정상 · Room 테이블 · 칸 이름(trip_mst · trip_place_dtl) 대조

## 2 DB 참고를 표준정의서 v0.4 기준으로 — 2026-11-11 · errorn
- 사람 질문(표준을 따랐나)으로 대조: 이전 DB 절은 develop Room 구현을 옮긴 것이라 표준과 5곳 다름 — S1 trip_place_dtl.place_ord(D09 위반) · S2 addr(표준 밖) · S3 trip_status_hist 미기록(R12 · D27) · S4 추천 시드 모델 ≠ route_reco_dtl · S5 req_uuid · req_hash 정의서 미반영
- 문서 · PDF 5절을 표준 기준으로 다시 쓰고 차이를 5.1 에 분리. Gleo 는 저장소 인터페이스만 불러 S1 ~ S3 · S5 는 저장소만 고치면 됨 · S4 만 지역 맞추기 입력 결정 필요

## 2 다른 브랜치 대조 · DB 참고 보완 — 2026-11-11 · errorn
- 원격 46 브랜치 · 이력 조사: route_reco_dtl · route_reco_place_dtl 구현 없음 · trip_status_hist 는 서버에만 · 지역 계산은 develop RoadmapRules.regionOfTrip 재사용 가능(S4 권장안)
- 표준 문서 두 벌 발견: docs/data/standard.md v0.35(앱 · 센서) ↔ 루트 데이터표준정의서_v0.4.md(서버) — place_ord 차이(S1). 데이터 담당 정리 요청 초안 `_workspace/gleo-actions/msg-data-owner.md`
- navi-autorecord/main(#66)과 4파일 충돌(TripPlaceRecorder 등) — 머지 순서 조율 필요

## 2 DB 참고 단순화 — 2026-11-11 · errorn
- 사람 요청: 최종 DB 선택은 데이터 담당이 나중에 함 → 차이 분석(S1~S5) · 표준 두 벌 · 브랜치 겹침은 문서에서 빼고, 데이터 표준정의서 v0.4 기준으로 Gleo 기능이 쓰는 테이블 · 칸만 간단히

## 4 두 관점 리뷰 반영 — 2026-11-12 · errorn
- 에이전트: structure-reviewer 1 · pleos-reviewer 1(병렬 · 읽기 전용)
- 리뷰 지적: 구조 위반 5 · 확인 3 / Pleos 반려 1(중) · 확인 1(중) · 하 3 → 고침 9 · 안 고침 3(이유: domain 이 data 를 못 부르는 현 규칙 · 시도 별칭 위치 · 시험 위치)
  - 콜백 받는 앱을 Gleo(ai.umos.ambientai)로 고정 · 늦게 만들어진 화면이 지난 명령(종료 팝업 · 화면 이동)을 따르지 않게 결과 시각 + 10초 유효 · 앱 틀 ViewModel 이 저장소에서 직접 받음 · enum · 문장 AppConfig 한 곳 · 처리기 기본값 제거 · %1$s 노출 수정 · 지역 원문 정리
- 센서: 단위 455(새 3) · lint 오류 0 · debug/release 빌드 · manifest · secrets · hygiene · guard 녹색 · 에뮬레이터: 콜백 Gleo 수신 확인 · 「대구!!@@」 → 「대구」
