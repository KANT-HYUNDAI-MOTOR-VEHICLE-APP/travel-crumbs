# #61 홈에서 「가 볼 장소」 · 「장소 고르기」 구역 제거

- 기능명 home-cleanup · 영역 app · 담당 최성호 · 스펙 생략(이슈 본문이 스펙)

## 2 구현 끝 — 최성호 · 2026-11-10

- 산출물: `4b122c2` 홈 구역 제거(HomeFeatureSections · PlaceSections 삭제 · `RouteFailText.kt` 로 실패 문구 함수만 이동 · 구역 전용 문자열 9개 삭제 · 시험 재작성)
- 센서(develop `d402c73` 기준): lint · 단위 시험 393 실패 0 · release 빌드 · 위생 · 비밀값 통과 · 에뮬레이터 홈 캡처(여행 없음 · 여행 중)
- 리뷰 에이전트(심사관 · 구조)는 돌리지 않음 — 사람 요청으로 PR 먼저
- 남은 것(후속): HomeViewModel 안내 · 핀 로직 · A-14 연결 화면 · 고정 쓰기 경로 · `HomeGuideScreenTest`
- 막힘: 없음 · 다음: 사람이 구현 PR 머지
