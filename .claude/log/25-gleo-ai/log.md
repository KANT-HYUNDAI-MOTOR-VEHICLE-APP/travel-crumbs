# #25 Gleo Action과 기존 여행 CRUD 연결 기반 — 장소 기록

- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/25
- 종류: 기능
- 담당: errorn (git user.name) · GitHub Riddlerio
- 브랜치: gleo-ai/main · gleo-ai/spec · gleo-ai/impl(미생성)

## 1 스펙 검토안 작성 끝 — 2026-11-08 14:22 KST · Riddlerio

- 산출물: `docs/spec/gleo-ai/artifacts/gleo-crud-foundation.md` · `32de301ceb4c9f2d5fe27cff528d8f7b11c9db46` · PR 없음.
- 기준: `gleo-ai/main`의 `6281cdb4693224a9338743231070b1b9f03706da`와 같은 CRUD 사본. GitHub 이슈 #25 본인 담당 확인 후 그 기준에서 `gleo-ai/spec` 생성.
- 에이전트: spec-writer 역할 1회 + 부분 수정 2회, CRUD 실행 현실성 검토 1회 + 작성 문서 재검토 1회. Claude 전용 정의를 Codex 서브에이전트 역할로 적용했으며 Claude 모델 실행으로 기록하지 않음.
- 센서: `doctor.py` FAIL 9 · WARN 3. 이 실행 세션에서 JDK·SDK·Pleos 이미지·AVD·Docker·gh 인증·Git 환경을 확인하지 못한 결과이며 사용자 PC 전체의 미설치 확정 아님. ZIP 사본은 Git 저장소 아님. 빌드·서버·Gleo 실기 시험 미실행.
- 정적 확인: 기존 app/server/하네스 관련 269개 파일 해시 변경 0. 스펙 8절·첫 두 줄·참고 문서·코드 펜스·시험 계획 30행 확인. 실제 시험 통과와 구별.
- 리뷰 지적: 위치→서버 저장 순서 명료화, 콜백 종료와 실제 업무 종료의 분리, Gleo 추가 동작과 기존 자동 경로 수집의 동의 보장 범위 구분. 스펙 수정 반영.
- 실행 판단: 기존 장소 저장 재사용 가능. 현재 FAILED는 원인 손실, SDK 10초/HTTP 각 20초는 Gleo 7초 보장 불가, 콜드스타트 복원·동의·내비 준비 경합 존재. 완전한 멱등 실행·서버 취소 보장 없음.
- 막힘: 환경 FAIL, 구형 기준 하네스와 실제 CRUD 데이터 규칙 충돌, 생성 OpenAPI/내보내기 도구 부재. 구현·센서 녹색·PR 준비 완료로 표시하지 않음.
- 사람 결정: 이슈 초안·기준 브랜치·산출물 경로 승인. 이번 지시는 MD 스펙만 작성하고 재확인. PR·구현 보류.
- 단계 적용: 환경 FAIL 이후 실행 단계는 진행하지 않음. 사용자가 요청한 문서 범위에서 정적 검토용 스펙만 작성. 하네스 설정·센서 예외·기존 CRUD·DB·서버·OpenAPI 수정 없음.
- 다음: 사용자에게 스펙 제출. U1 실패 안내 수준·U2 내부 응답 기한·U3 준비된 세션 범위의 추천안 검토 후 확인. 환경·팀 의존 사항을 확인하고 별도 지시 시 스펙 PR 단계 검토.
- 활동: Claude Code 자동 trace 없음. 도구 호출 수·실패 수·guard 차단 수를 추정하여 만들지 않음. PR 전 `trace.md` 생성 단계 미진행.
