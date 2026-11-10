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
