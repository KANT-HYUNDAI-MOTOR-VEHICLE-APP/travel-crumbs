# #4 [기능] 장소를 차량 내비 목적지로 설정(NaviHelper 길안내 연동)
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/4
- 종류: 기능
- 담당: 최성호
- 브랜치: navigation/main · spec · impl

## 1 스펙 끝 — 2026-11-04 13:35 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` · spec PR(→ navigation/main)
- 에이전트: spec-writer × 2(첫 작성 · 미확정 답 반영)
- 센서: 실행 안 함(스펙 단계) · 참고로 `check_manifest.py` 는 develop 기준 이미 빨강 1건(`NAVI_ROUTE` 허용 밖)
- 리뷰 지적: 없음(스펙 단계)
- 막힘: 없음. 노션 회의록은 spec-writer 가 읽지 못해 이슈 본문만 근거
- 사람 결정: 서버 없음 · 시연용 경량 앱 / 주행 중에도 한 번 터치 / 진입점은 홈 임시 목록 / 미확정 U2 ~ U12 추천안대로 확정 · U1(임시 장소 구체 값)만 남김
- 다음: 사람이 spec PR 확인 · 머지 → 2단계 구현. 매니페스트 센서 녹색은 하네스 이슈 #2(ALLOWED · DO) 머지가 전제
- 활동: 도구 호출 95 · 실패 0 · guard 차단 0
