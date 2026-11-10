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
