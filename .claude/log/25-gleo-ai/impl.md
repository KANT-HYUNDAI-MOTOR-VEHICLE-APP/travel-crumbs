# #25 Gleo 음성 명령과 기존 장소 기록 — 선행 구현 기록

- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/25
- 담당: errorn (GitHub Riddlerio)
- 작업 브랜치: `gleo-ai/impl` → `gleo-ai/main`
- 스펙: PR #26 · `gleo-ai/spec`의 `docs/spec/gleo-ai/artifacts/gleo-crud-foundation.md`
- 기록 위치: 아직 머지되지 않은 spec PR의 `log.md`와 add/add 충돌을 피하기 위해 이번 구현만 같은 이슈 폴더의 `impl.md` 사용. 하네스 설정·guard 변경 없음.

## 2 구현 착수 — 2026-11-08 · errorn

- 사람 지시: spec PR 미머지 상태에서 main 기반 impl 브랜치를 만들어 스펙 기반 구현 진행. 기존 spec-only 보류 해제. 사람 대신 머지 금지 유지.
- 순서 차이: app-orchestrator의 spec 머지 선행 조건은 위 명시 지시에 따라 이번 작업에서 순서 변경. spec 문서·로그를 impl에 복사하거나 spec 브랜치를 삭제하지 않음.
- 기준: `6281cdb4693224a9338743231070b1b9f03706da`. 원본 사용자 체크아웃 변경 없이 별도 작업 사본 사용.
- 범위: 기존 OPEN 유지·무파라미터 RECORD→기존 RecordPlaceUseCase. 기존 CRUD·서버·DB·DTO·REST·공통 SDK/HTTP timeout·차량 권한 변경 없음.
- 구현 가정: U1A 원인 추측 없는 일반/미확인 결과, U2 내부 6초 응답 시도 목표, U3 준비된 세션 우선. 사람의 각 설계 확정이나 공식 7초 성능 보장으로 기록하지 않음.
- 에이전트: 저장소 builder 정의의 app 역할을 Codex 서브에이전트에 적용. Claude 전용 모델 실행으로 표시하지 않음. 메인은 제품 코드 수정하지 않음.
- 환경: 실제 호스트 doctor FAIL 5/WARN 3(JDK·SDK·Pleos 이미지·AVD·Docker 미확인). Git·gh 인증 확인. Windows CP949 문제는 `python -X utf8` 실행으로 해결, 하네스 수정 없음.
- 센서: manifest·secrets·hygiene·frontmatter·하네스 guard 11개/trace 5개/manifest guard 7개 통과. 시작 검사이며 최종 구현 후 결과와 구분. 로컬 Gradle은 Java 부재로 시작 불가.
- 충돌: PR #23와 현재 Gleo 기준 사이 구현 전부터 14개 충돌 확인. 다른 PR 병합 없이 신규 Gleo 파일 중심으로 분리. ShellUiState/VM을 함께 고치는 1차 설계는 새 충돌을 늘리는 실측 결과에 따라 별도 Gleo 화면 상태로 분리 예정.
- 미해소: 구형 하네스와 실제 CRUD의 서버 저장 규칙 차이·생성 OpenAPI/export 도구 부재·기존 자동 기록기의 동의 검사·실제 Gleo 등록 및 실기 시험.
- 다음: 코드·시험 작성, 두 관점 리뷰와 수정, 정적 센서 및 가능한 원격 CI 검증. 실기·운영 배포 완료와 구분.
- 활동: Claude Code 자동 trace 없음. 호출 수·실패 수·guard 차단 수 수동 생성 없음.

## 2 코드·시험 1차 작성 끝 — 2026-11-08 · errorn

- 산출물: `1629dc8fc371afce0c3dc6f05ac48caccfe854d9` · app 23개 파일 · 새 시험 27개. 실행 검증 완료와 구분.
- 에이전트: app builder 1개 지속형. 메인의 충돌 실측 전달 후 기존 ShellUiState/VM 수정을 제거하고 별도 Gleo ViewModel/UiState로 분리.
- 정정·확정: AppShellUiState·AppShellViewModel·기존 AppShellViewModelTest·홈/주행 ViewModel·CRUD·서버·DB·DTO 변경 없음. AppShellScreen 화면 수준에서 Gleo 상태를 구독하고 순수 한 줄 컴포넌트에 전달.
- 충돌 재검사: `git merge-tree --write-tree HEAD origin/app-shell-v5/impl`의 충돌 파일 집합은 구현 전 14개와 동일. 실제 병합·타 브랜치 수정 없음.
- 새 시험: Processor 14·Intent Reader 5·Readiness 6·Gleo ViewModel 2. 시간/중복/실패/표시 해제의 fake·가상 시간 시험. 실제 Activity/Hilt·SDK 수명·음성·서버 DB 시험을 대체하지 않음.
- 센서: builder의 manifest·secrets·hygiene·diff-check 통과. 로컬 Gradle은 Java 없음. 생성 OpenAPI 입력 부재를 실제 검사 오류로 확인.
- 스펙 밖 최소 보완: 단일 root Activity 재진입(singleTask)·복원 Intent 재실행 방지·구성 변경 동안 SDK 유지·RECORD 입력 크기 제한. 실기 수명 검증은 미완료.
- 다음: 소스 freeze 후 Pleos·구조 읽기 전용 리뷰. 로컬 빌드 도구 부재로 원격 CI 실행을 위한 검토용 Draft PR 준비. Ready/머지/운영 배포 아님.

## 4 두 관점 리뷰 응답 — 2026-11-08 · errorn

- 에이전트: 읽기 전용 Pleos reviewer 1회·Android structure reviewer 1회. 변경 app 23개 freeze 해시 일치. 정적 구조 위반 0/확인 0, Pleos 반려 0/확인 1.
- R1: exported RECORD의 쓰기 권한 보호 계약 미확인. 입력 검증이 발신 인증이라는 주장 없음. 공식 Gleo 일반 startActivity 계약과 API34 환경에서 신뢰할 발신 인증 방법을 확인하지 못했으므로 임의 package/referrer/extra 인증 코드 추가 없음.
- builder 응답: 공식 보호 계약 확보 전 보류. 개발 가이드·PR에 공식 계약 및 별도 UID 무권한 앱 차단 시험을 출시 전 조건으로 추가. 자동저장 공개 범위 결정 또는 활성화 보류 필요. 제품 소스 변경 없음.
- 검증 경계: 실제 Activity/Hilt 재생성·SDK 수명 시험은 미작성·미실행. source guard가 실제 기기에서 동작한다는 보장과 구분.

## 6 구현 검토용 Draft PR — 2026-11-08 · errorn

- 산출물: PR #27 https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/pull/27 · `gleo-ai/impl` → `gleo-ai/main` · Draft. 기준 브랜치 변경·머지·배포 없음.
- 커밋: 제품 코드·시험 `1629dc8` 및 개발 가이드·공유 기록. spec PR #26 및 그 브랜치 유지.
- 센서: 원격 CI의 changes/common 성공, server 변경 없음으로 생략. 첫 app 실행은 Kotlin/Hilt·신규 시험 포함 125개 컴파일·시험 실행 후 기존 TripPauseSyncTest 1개 실패. 결과 로그는 CI run `42475180073` 및 PR 체크에서 확인 가능.
- 실패 첫 줄: `TripPauseSyncTest > 앱을 다시 켰을 때 서버가 일시정지면 앱도 일시정지로 맞춤 FAILED`, `AssertionError at TripPauseSyncTest.kt:38`.
- 원인 대조: 해당 시험·TripPauseSync·FakeTripRepository·RecordingRepository의 기준 대비 변경 없음. 신규 Gleo가 이 시험에서 생성/호출되지 않음. 양방향 초기 복원 collector 경합 가능성은 있지만 현재 로그만으로 어느 assertion인지 확정 불가. 기대값 완화·삭제·skip·기존 CRUD 패치 없음.
- 다음: 실패 잡 재실행 및 문서 변경 이후 최신 CI 결과를 PR 본문·이슈 #25에 기록. CI 성공 전 Ready 또는 빌드 완료로 표시하지 않음. 제품 소스는 리뷰한 freeze 그대로 유지.
- 남은 것: R1, U1~U3 사람 리뷰, D3/D4/D5 및 실기 검증. 이 PR에서 다른 작업과 기존 품질 문제를 해결했다고 간주하지 않음.
- 활동: 자동 trace 없음. `trace_report.py` 입력 없음은 활동 기록 한계로 공개하며 임의 trace 또는 집계 생성 없음.
