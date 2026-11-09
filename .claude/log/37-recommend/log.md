# #37 [기능] 추천 흐름 — 지역 고르기 · 로드맵 랭킹 · 태그별 보기 · 상세 · 안내
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/37
- 종류: 기능(스펙 생략)
- 담당: 노희주
- 브랜치: recommend/main · impl

## 기획
- 기준: 와이어프레임 v6 B-01 ~ B-05 · 시나리오 S-2 · 디자인 시스템(#34) · #16(성호님 기획) 중 둘러보기 · 랭킹 화면 부분
- 데이터: 서버 테이블 없음(지역 · 별점 표준 미등록) → 앱 안 시연용 시드

## 2단계 구현 끝 — 2026-11-09 · 노희주
- 산출물: 57a60b9 feat(app) — 「추천」 칸 B-01 지역 고르기 → B-02 랭킹(별점순 · 최신순 · 동행자 필터) → B-03 태그별 보기 → B-04 상세 → B-05 안내 확인 · 시연용 시드(`data/recommendation/RecommendationSeed.kt` 한 곳 · `RecommendationRepository`) · 길안내 포트 `RouteGuidanceRequester`(debug 더미 · release 준비 중)
- 에이전트: builder(app-builder-37) × 1
- 센서: gradle lint · 단위 테스트 164 · assembleRelease 녹색 · 매니페스트 · 비밀 · 위생 · 데이터 표준 위반 0
- 다음: 리뷰 2관점

## 4단계 수정 끝 — 2026-11-09 · 노희주
- 산출물: 39219c2 fix(app) — 「시연용 데이터」 모든 빌드 표시 · release 안내 「시연용 · 실제 안내는 준비 중」 · 부품은 상태 + 람다만 · 기호 문자열 리소스로 · 레일 「추천」 · 「추천 보러 가기」는 늘 B-01 · 시드 좌표 벌림
- 에이전트: pleos-reviewer × 2(재리뷰 포함) · structure-reviewer × 1 · builder 이어서 × 1
- 센서: 단위 테스트 166 · 나머지 같음 · 녹색
- 리뷰 지적: 심사 반려 1(시연 시드가 release 에서 실제처럼 보임 · 해소) · 확인 5(debug 문구 위치 · 부품의 ViewModel · 시드 바인딩 · 기호 문자열 · 빌드별 싱글턴) · 구조 위반 0 · 재리뷰 반려 0
- 사람 결정: 시연 데이터 release 유지 + 모든 빌드 표시(제출 APK 가 release) · 추천 진입은 늘 B-01 · B-04 핀 겹침은 시드 좌표 벌림
- 다음: impl PR

## 6단계 PR — 2026-11-09 · 노희주
- 산출물: impl PR(recommend/impl → recommend/main)
- 다음: 사람이 확인 · 머지 → develop PR(#36 테마 먼저 develop 에)
- 활동: 기간: 2026-11-09T17:34:13+09:00 → 2026-11-09T19:21:03+09:00 · 세션 1 · 도구 호출 120 · 실패 0 · guard 차단 0
