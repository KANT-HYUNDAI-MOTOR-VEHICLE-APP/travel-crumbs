# #59 추천 지도·공개 장소 핀

- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/59
- 담당: hiju200208-png
- 브랜치: recommendation-map/spec → recommendation-map/main

## 스펙 초안
- 사람 결정: 현재 시드·기기 공개 자료로 지도·핀·장소 정보·안내 연결
- 산출물: docs/spec/recommendation-map/artifacts/place-map.md
- 에이전트: spec-writer 1회
- 제약: 지도 자산은 강원 5구역 · 전국 지도 제공 방식 미정
- 미확정: 지도 범위·반경·동의·랭킹 이동 관계·중첩·확대이동
- 다음: 성호님 검토 및 사람 결정 뒤 스펙 확정·구현
- 활동: 자동 trace 없음 · 추정 집계 생성 없음

## 지도 범위 결정 반영
- 사람 결정: 전국 지도까지 연결(추가 지도 서비스·이용 조건 검토)
- 기존 강원 5구역 한정안 제외 · 지도 공급자·키·요금·Pleos 호환성과 데이터 처리 조건은 별도 검토
- 스펙 N1 및 온라인 지도 요청 조건 보완 · 코드 변경 없음

## 구현 끝 (2026-11-10)
- 사람 결정: N1 ~ N7(스펙 PR #60) · N8(z12 · ABI x86_64+arm64 · 남한 경계 · APK 자산) · 지도 파일은 GitHub 릴리스 `map-kr-z12-20261109` 에서 빌드 때 받기(SHA-256 검증)
- M00: Protomaps 20261109 · z10 20MB ~ z14 362MB 측정 · Pleos 에뮬레이터 MapLibre 13.7.1 동작 · 망 요청 0
- 에이전트: app-builder(Claude · M00 · 본 구현 · 리뷰 1회차) · Codex(본 구현 이어받기 c58abe6 ~ cb4a39d) · pleos-reviewer · structure-reviewer
- 스펙 밖: #55 home-nearby/main 통합(3d19364 · 위치 동의 의존) · #58 별점 표시 보존
- 리뷰: 심사 반려 0 · 확인 3 / 구조 위반 0 · 확인 5 → 구조 1 ~ 4 고침(00a9e52) · 구조 5 · M30 · M33 남김
- 센서: lint · 단위 497 · assembleRelease 녹색 · secrets · hygiene 0 · data_standard 위반 0 · **check_manifest 위반 3(tools:node="remove" 오탐 · 하네스 피드백 필요)**
- 막힘: 에뮬레이터 화면 캡처 검정(이어받기 중 콜드 부트로 해소) · 사용량 한도로 builder 1회 중단
- 다음: impl PR(→ recommendation-map/main) · 성호님 리뷰 · check_manifest 하네스 이슈
