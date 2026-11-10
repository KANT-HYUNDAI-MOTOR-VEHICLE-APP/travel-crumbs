# #51 추천 별점 진입점 후속 수정

- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/51
- 브랜치: codex/51-rating-button → v6/main

## 구현 · 리뷰 — 2026-11-10

- 사람 결정: 모든 추천 상세에 「별점 주기」 표시, 안내 전 클릭 시 「안내 시작 후 별점을 남길 수 있어요」 안내
- 변경: 공용 추천 상세 버튼, ViewModel 상태 안내, 공용 문구 연결 및 회귀 시험
- 저장 자격: 안내 성공 기록 필수 유지 · 서버 전송 추가 없음
- 에이전트: app builder 1 · Pleos 심사 1 · Android 구조 리뷰 1
- 리뷰: 두 관점 발견 0건
- 공통 센서: 비밀 · 저장소 위생 위반 0
- 매니페스트: 기존 내비 권한 2개 · 주행 메타데이터 1개 위반 유지(변경 밖)
- 활동: 이 세션의 자동 trace 없음 · 추정 집계 생성 없음
- 앱 센서: lint · testDebugUnitTest · assembleRelease · assembleDebug 전체 성공
- 에뮬레이터: install -r Success · 앱 실행 완료 · 데이터 보존
- 산출물: 앱 7파일 커밋 f8a20fd
- 다음: 성호님 리뷰 요청 · 머지는 사람
