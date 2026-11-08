- 이슈: #25 — Gleo Action과 기존 여행 CRUD 연결 기반 — 장소 기록
- 기획: `docs/data/handoff.md` · `docs/data/gleo-handoff.md` · `docs/design/design-system.md` · `데이터표준정의서_v0.4.md` · `docs/data/data-decision-log.md`

# Gleo로 남기는 여행 흔적 — CRUD 연결 기반 스펙

검토용 초안 · 2026-11-08 · 사용자 승인 전 · 기준 `gleo-ai/main`의 `6281cdb4693224a9338743231070b1b9f03706da` · 산출물 예정 `docs/spec/gleo-ai/artifacts/gleo-crud-foundation.md`

## 1. 요약

- 대상·상황: 정차 중 위치 동의와 여행 시작을 마친 뒤, 이동 중 지금 지나가는 곳을 남기려는 Travel Crumbs 사용자.
- 핵심 경험: Gleo에 「이 장소 기록해 줘」 한마디 → 기존 장소 기록과 동일한 임시 저장 → 저장 확인 후 음성 결과.
- 이번 범위: 기존 앱 열기 유지 + 장소 기록 1개 연결 + 이후 CRUD 연결에 공통으로 사용할 수신·검증·결과 계약의 정의. 구현·PR 없이 MD 검토까지.

### 1.1 기능 흐름

(수정: 준비 확인과 기존 업무의 실행 순서 명료화)

```mermaid
flowchart LR
    A[사용자 음성 명령] --> B[Gleo 의도 해석]
    B --> C[등록된 Action 수신·입력·준비 확인]
    C --> D[기존 RecordPlaceUseCase]
    D --> E[기존 내비 위치 조회]
    E --> F[기존 여행 API·DB 저장]
    F --> G[기존 장소 수 갱신]
    G --> H[실제 결과 확인]
    H --> I[Gleo 콜백·음성 응답]
```

새로운 장소 저장 경로를 만드는 설계가 아니라, 기존 동작의 진입점에 음성 명령을 추가하는 계약. 그림의 신규 부분은 Action 검증·업무 연결·실제 결과에 따른 콜백이며, 내비 조회·서버 저장·장소 수 갱신은 기존 CRUD의 책임.

### 1.2 실행 현실성 판정

| 항목 | 확인한 근거 | 판정 |
|---|---|---|
| Gleo로 앱 열기 | 기존 `OPEN_TRAVEL_CRUMBS`·Intent 수신·Broadcast 응답 | 유지 대상. 본 세션의 재실행 검증 없음 |
| 준비된 여행에서 장소 기록 | 기존 `TripRecordPlaceUseCase` → 내비 현재 위치 → `TripRepository.recordPlace` | 새 API·DB 없이 연결 가능한 경로 |
| 저장 성공 판단 | 서버 `record_place`의 commit 후 응답, 앱의 `ActionResult.DONE` | 응답 확인 후 성공 표현 가능 |
| 모든 실패의 원인 구분 | 기존 반환값이 `DONE`·`FAILED`·`NOT_READY`뿐 | 위치 오류와 서버 오류의 정확한 분리는 현재 계약으로 불가. U1 결정 필요 |
| 7초 안 콜백 | 공식 Gleo 제한 7초, 기존 SDK 대기 최대 10초·HTTP 연결/읽기 각 20초 | 기존 함수를 단순 연결하는 것만으로 보장 불가. U2 및 실기 시험 필요 |
| 앱을 새로 켠 즉시 기록 | `RouteRecorder.start()`의 여행 복원이 비동기이며 Gleo 수신은 즉시 진행 | 준비 경합 존재. 초기 null을 ‘여행 없음’으로 확정 불가. U3 필요 |
| 동의 확인 | 기존 동의 API 존재, 장소 기록 UseCase 자체에는 동의 검사 없음 | Gleo 진입의 준비 조건 명시 필요. U3 필요 |
| 완전한 중복 실행 방지 | 공개 Action 계약의 요청 식별자 및 현 저장 API의 멱등 키 미확인 | 보장 불가. 진행 중 중복 접수 제한까지만 목표 |

**판정: 기존 CRUD를 재사용하는 구현 후보로 타당하며, 곧바로 실행 가능한 완성 기능으로는 미판정.** U1~U3의 계약 결정과 실행 시험이 구현 전 조건. `doctor.py`가 이 세션에서 FAIL 9·WARN 3인 상태로, 빌드·실기·서버 왕복 통과 주장 없음.

### 1.3 기존 CRUD 보호 범위

| 기존 기능·규칙 | 이번 계약 |
|---|---|
| 위치 동의 → 여행 시작 | 정차 중 기존 화면 사용. Gleo가 동의나 여행 시작을 대신 처리하지 않음 |
| `RecordPlaceUseCase` 및 실제 바인딩 `TripRecordPlaceUseCase` | 장소 저장의 유일한 기존 업무 경로로 재사용 |
| `TripRepository`·`server/app/trips/`·기존 DTO | 새 저장 경로·서버 스키마·재시도 규칙 추가 없음 |
| D18 임시 장소 | 위치·시각만 기록하고 `draft_yn = Y` 유지. 이름·후기 자동 생성 없음 |
| D19 공식 장소 | 임시 기록만으로 `place_mst` 생성 또는 공식 장소 연결 없음 |
| D27 일시정지 | `PAUSED`에서도 장소 기록 가능. 경로점 자동 저장 재개와 별개 |
| 날짜 기본 여행명·제목 수정 | 현재 동작 유지. `TripNameSuggester` 교체 없음 |
| 주행 화면의 「음성 입력」 | `StartVoiceInputUseCase`의 준비 중 상태 유지. 이 버튼이 Gleo 청취를 시작한다고 설명하지 않음 |

여행 시작·종료·일시정지·재개·제목 변경은 기존 저장 기능이 있으나 이번 음성 공개 범위 밖. 과거 여행 의미 검색·선택 문맥·내비 목적지 설정은 추가 기능이 필요한 후속 범위. 마이크 SDK·자체 STT/TTS·키워드 분류기·웨이크워드·일반 LLM 호출 제외.

## 2. 화면

### 2.1 기존 여행 기록 홈(A-03·A-05)

- 기존 여행 상태·장소 수·경로 그림·버튼 구성 재사용. 별도 Gleo 대화 화면 추가 없음.
- Gleo Action의 처리 결과는 짧은 상태 표시 한 곳으로 노출하는 설계안. 실제 상태 제공 위치는 D1의 최소 변경 범위 확인 대상.
- 장소 수는 기존 `activeTrip.placeCount`를 사용하며 Gleo가 화면 숫자를 임의로 증가시키지 않는 원칙.
- 서버 저장이 확인되어도 경로 그림·임시 기록 목록의 재조회가 늦거나 실패할 가능성의 구분. 그림이 바로 바뀌지 않았다는 이유로 재저장 금지.

| 상태 | 정차(PARKED) | 주행(DRIVING) | 모름(UNKNOWN) |
|---|---|---|---|
| 정상 표시 | 기존 홈과 짧은 결과 문구 | 기존 주행 전용 화면의 결과만 | 주행과 동일 |
| 준비 중 | 「기록할 준비를 확인하고 있어요」 | 같은 한 줄, 입력 창 없음 | 주행과 동일 |
| 처리 중 | 「이 장소 기록 중」 | 같은 한 줄 | 주행과 동일 |
| 저장 확인 | 「이 장소를 기록했어요」, 기존 장소 수 갱신 | 같은 한 줄, 상세 화면 전환 없음 | 주행과 동일 |
| 사전 조건 미충족 | 「정차 후 여행 기록을 시작해 주세요」 또는 「위치 동의를 확인해 주세요」 | 같은 짧은 안내만, 동의·설정 창 자동 노출 없음 | 주행과 동일 |
| 결과 미확인 | 「기록 여부를 확인하지 못했어요. 정차 후 확인해 주세요」 | 같은 한 줄, 즉시 재시도 유도 없음 | 주행과 동일 |

사전 조건 문구는 해당 조건을 실제 확인한 경우에만 사용. 조회 실패를 ‘동의 안 함’·‘여행 없음’으로 바꾸지 않는 원칙.

### 2.2 기존 주행 전용 화면

- 기록 상태·분 단위 경과·장소 기록·음성 입력·일시정지의 기존 구성 유지.
- Gleo 음성 진입에 별도 터치나 확인 버튼을 요구하지 않는 목표. 짧은 결과 안내 외 상세 입력 없음.
- 정차로 바뀌면 기존 앱의 홈 표시 규칙 적용. 주행·UNKNOWN으로 바뀌면 정차 전용 입력 화면 숨김.
- 「듣는 중」·음성 인식 원문·Gleo 내부 실패 횟수는 앱이 수신한다고 확인되지 않은 상태이므로 신규 표시 없음. 디자인 시스템 19장의 전체 음성 상태 구현과 이번 Action 연결 범위의 차이는 D1 기록.
- 음성 결과는 Gleo 콜백으로 요청하며 앱 자체 TTS 중복 재생 없음. Gleo의 실제 한국어 응답 문장은 플랫폼 생성 결과이므로 앱의 고정 멘트와 일치 보장 없음.

## 3. 상태

### 3.1 개념 상태 계약

아래 칸은 새 DB 모델이 아닌 앱 내 명령 처리와 화면 표시의 개념 계약. 기존 홈·주행 `UiState`를 대체하지 않고, 불변 화면 상태로 표현하는 원칙.

| 칸 | 값 | 의미 |
|---|---|---|
| `functionName` | OPEN / RECORD / 없음 | 수신한 등록 Function |
| `phase` | IDLE·PREPARING·PROCESSING·SAVED·FAILED·UNCONFIRMED | 앱이 실제 관측한 처리 단계 |
| `readiness` | UNKNOWN·READY·UNAVAILABLE | 여행·동의·내비가 확인된 준비 상태. 초기 null과 확인된 부재의 구분 |
| `resultReason` | 입력 오류·준비 미확인·확인된 사전 조건·일반 실패·시간 초과·없음 | 실제로 구분 가능한 이유만 사용 |
| `callbackSent` | 참/거짓 | 해당 처리 흐름에서 응답을 이미 보냈는지의 내부 상태. 서버 저장 여부와 별개 |
| `message` | 짧은 표시 문구 / 없음 | 화면이 표시하고 확인 후 지우는 상태 |

`tripUuid`·장소 수·동의 원본은 기존 저장소에서 조회하는 값으로, Gleo 전용의 별도 여행 원본 저장 없음. UI에서 자료원 직접 호출·별도 이벤트 Flow·Context 보유 없음. 근거: 저장소 Android 구조 규칙 및 [Google 아키텍처 권장안](https://developer.android.com/topic/architecture/recommendations).

### 3.2 정상 전이

1. IDLE → 등록 Action 검증. 미등록 Action은 업무·콜백 실행 없이 기존 무시 원칙 적용.
2. OPEN → 기존 앱 열기 결과 응답. 장소 저장 실행 없음.
3. RECORD → PREPARING. 기존 세션에서 위치 동의·현재 여행·내비 준비를 확인한 경우만 PROCESSING.
4. 준비 조건이 확인되지 않았으면 FAILED 또는 UNCONFIRMED. 이때 아직 장소 저장을 호출하지 않았다는 사실과, 이미 전송된 요청의 불명확한 결과를 구분.
5. PROCESSING → 기존 `RecordPlaceUseCase` 정확히 1회 호출 → `DONE`이면 SAVED, 실패면 확인 가능한 수준의 FAILED/UNCONFIRMED.
6. 마지막 결과의 콜백 1회 전송 시도. 화면 표시 해제는 업무 재실행의 계기가 아님.

‘콜백 1회’는 앱 내부 한 처리 흐름의 목표. 프로세스 종료·플랫폼 재전달까지 포함한 전역 exactly-once 보장과 구별.

### 3.3 시간·취소·늦은 결과

(수정: 콜백 기한과 실제 저장 작업 종료의 분리)

- 공식 한도: Gleo가 7초 뒤 Action을 취소하는 규칙. 근거: [Callback Schema 가이드](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback).
- 현재 대기: `SDK_CALL_TIMEOUT_MS = 10,000ms`, OkHttp 연결·읽기 각각 20초. 기존 수치를 전역으로 줄여 다른 CRUD에 영향을 주는 변경 금지.
- 설계안: Action 수신부터 콜백 전송 시도까지 앱 내부 6초 이내 목표로 준비 확인과 업무 실행을 함께 관리. 6초는 공식 수치가 아닌 U2 승인·기기 측정 대상이며, Gleo→앱 전달 지연까지 포함한 전체 7초 보장과 구별.
- 기한 전에 저장 응답을 받으면 success. 저장 요청 전송 후 결과가 없으면 ‘미저장’으로 단정하지 않고 결과 미확인 fail 응답.
- 코루틴/HTTP 취소는 서버 트랜잭션 롤백 증명이 아님. 기존 코드의 `runCatching`이 취소를 일반 실패로 바꾸는 가능성까지 시험 대상.
- 기한 내 fail 콜백을 보냈다는 이유만으로 기존 UseCase의 실행이 끝난 것으로 간주하지 않는 원칙. 실제 업무 실행이 진행 중이면 콜백 이후에도 추가 RECORD의 저장 실행을 허용하지 않는 목표.
- 시간 초과 뒤 자동 POST 재시도·자동 재기록 없음. 늦은 완료가 관측되어도 같은 Action에 두 번째 성공 콜백 전송 없음.
- 앱 안의 실제 업무 실행 종료가 확인되어도 서버 저장 결과 미확인이 남을 수 있는 상태. 다시 기록하도록 권유하지 않고 정차 후 기존 여행·장소 확인 안내 유지. 집계 수의 변화만으로 특정 음성 요청의 성공을 확정하지 않는 원칙.
- 프로세스 종료로 실행 문맥이 사라진 경우 자동 재실행 없음. 기록 존재 여부는 기존 목록을 통해 정차 후 확인.

### 3.4 중복·재진입

(수정: 시간 초과 응답 이후의 중복 실행 제한 유지)

- 동일 앱 프로세스의 RECORD 처리 중 추가 RECORD 접수로 두 번째 장소 저장을 시작하지 않는 목표. 새 요청 대기열이나 완료 후 자동 실행 없음.
- ‘처리 중’의 종료 기준은 콜백 전송이나 화면의 오류 표시가 아닌 기존 업무 실행의 실제 종료. 응답 기한이 지났어도 그 실행이 남아 있으면 추가 저장 실행 제한 유지.
- 저장 중 화면 재생성·Gleo 앱 재진입이 같은 업무 재실행을 일으키지 않는 목표. 기존 Activity 수명 및 SDK release 경합은 T20~T22 검증 대상.
- 공개 계약에서 요청 식별자를 확인하지 못한 상태로, 재전달과 사용자가 새로 한 동일 명령을 정확히 구분하지 못하는 한계. 진행 중 거절 문구·콜백 상관관계는 D2의 실제 Gleo 확인 전 확정 불가.
- 실제 업무 실행이 끝난 후 사용자가 별도로 새 명령을 하면 새 장소 기록 가능. 다만 이전 저장 결과가 미확인이면 정차 후 기존 기록 확인 안내 유지하며, 앱의 재시도 유도 없음. 텍스트·위치 유사성만으로 정상 요청을 임의 삭제하지 않는 원칙.
- 기존 화면 버튼과 음성의 동시 조작까지 한 번으로 보장하는 범위는 미확정. 공유 진입점의 최소 조정이 필요하면 CRUD 담당 검토 후 범위 결정.

## 4. 데이터

### 4.1 기존 데이터 재사용

| 흐름 | 기존 원본·규칙 | 이번 변경 |
|---|---|---|
| 현재 여행 | `TripRepository.activeTrip` / `currentTrip()` | 별도 여행 캐시·원본 생성 없음 |
| 위치 동의 | `ConsentRepository.isAgreed(LOCATION_COLLECT)` / `user_consent_hist` | 동의 조회 재사용, 음성으로 동의 생성·변경 없음 |
| 현재 위치 | `NaviRepository.currentLocation()` | 기존 UseCase 내부의 조회 사용. Gleo 입력으로 좌표 받기 없음 |
| 기록 시각 | 기존 `TimeSource.nowMillis()` | 음성 시작 시각으로 새 의미 부여 없음. 실제 위치 수신 뒤의 기존 기록 시각 유지 |
| 임시 장소 | `trip_place_dtl`의 `lat`·`lng`·`recorded_at`·`draft_yn` | 기존 POST와 저장 규칙 그대로 |
| 장소 수 | 서버 `trip_mst.place_cnt` → 기존 `activeTrip` 갱신 | Gleo가 독립 카운터 유지 없음 |
| 공식 장소·경험 | 기존 정차 후 A-06 처리 | 이름·주소·후기·태그·공식 POI 자동 저장 없음 |

신규 테이블·컬럼·마이그레이션·공통 코드 없음. 현재 저장 규칙을 변경하는 안이 필요해지면 이번 구현에 섞지 않고 인계서 7장에 따른 데이터 담당 검토 필요.

### 4.2 최소 수집·권한

(수정: 동의 검사에 따른 수집 제한을 이번 Gleo 명령의 추가 동작으로 한정)

- Gleo 수신: Function 이름과 무파라미터 요청. 사용자 ID·여행 UUID·좌표·주소를 음성 파라미터로 받지 않음.
- 동의가 확인되지 않으면 이번 Gleo 명령에 따른 추가 위치 조회·장소 POST 없음. 기존 앱의 모든 위치 수집을 이 명령 검사로 차단한다는 보장과 구별. 명령 검사 전에 시작되는 기존 `RouteRecorder`의 동의 재확인은 D5의 CRUD 의존 조건이며 이번 수정 범위 밖.
- Gleo 응답: Function 이름·상태·고정 영어 결과 문장만 사용. 개인 데이터가 없는 결과라 이번에는 선택 필드 `data` 생략, `mode`는 기존 기본 음성 응답 사용 제안.
- 디버그 로그: Action 종류·비식별 상태·처리 시간 수준. 음성 원문·좌표·주소·사용자 이름·여행 UUID·콜백 원문 전체를 추가 기록하지 않는 원칙.
- 기존 `GleoCallbackDataSource`가 payload 전체를 로그에 출력하는 상태이므로, 향후 개인 데이터 포함 콜백으로 확장할 때 그대로 사용 금지. 이번 고정 문장만의 응답에서도 원문 로깅 최소화 검토 대상.
- 기존 `CAR_DRIVE`·`NAVI_ROUTE`·`INTERNET` 재사용. 자체 마이크 수집이 없어 `RECORD_AUDIO`·`STT_SERVICE`·`TTS_SERVICE` 추가 없음. 목적지 설정 권한 추가 없음.
- 신규 RECORD Action filter는 기능 진입점 선언이며 새 차량 데이터 권한과 별개. exported Activity의 입력이 Gleo에서만 왔다고 보안상 신뢰하지 않는 원칙.

기준 하네스에는 ‘여행 원본은 기기’·‘서버 좌표 금지’가 남아 있지만, 현재 CRUD와 데이터 표준은 서버 저장 구조. 이 초안은 사용자 지시와 기존 CRUD를 존중한 상태이며, 충돌하는 규칙을 삭제하거나 우회하여 구현 승인으로 간주하지 않음. D3에서 팀의 하네스 정합성 확인 필요.

## 5. API 계약 변경

### 5.1 서버 API

**신규·변경 REST API 없음.** 아래는 기준 코드(`AppApi.kt`, `TripDtos.kt`, 서버 `router.py`·`schemas.py`·`service.py`)를 대조한 기존 계약 요약. 기준 스냅샷에 `docs/api/openapi.json`과 OpenAPI export 도구가 없어, 생성된 OpenAPI 검증 완료로 취급하지 않음(D4). 해당 파일을 손으로 새로 만들거나 수정하지 않는 원칙.

| 메서드·경로 | 입력 | 응답·실패 | 사용 |
|---|---|---|---|
| GET `/consents/latest` | `consent_type_cd=LOCATION_COLLECT` | 최신 `consent_yn` 또는 미이력 null | 준비 조건 확인에 기존 조회 재사용 |
| GET `/trips/current` | 사용자 식별은 기존 서버 경로 | `trip` 객체 또는 null | 기존 현재 여행 복원. 조회 전 null과 응답 null의 구분 필요 |
| POST `/trips/{trip_uuid}/places` | `lat`·`lng`·시간대 포함 `recorded_at` | 201: `trip_place_no`·좌표·시각·`draft_yn`·`place_cnt` | 기존 `RecordPlaceUseCase`를 통한 호출만 |
| 같은 POST의 실패 | 기존 서버 검사 | 404 `TRIP_NOT_FOUND`, 409 `TRIP_NOT_RECORDING`, 입력 422·전송 실패 등 | 서버/앱 경계에서 존재하나 현재 UseCase가 상세 이유를 반환하지 않는 한계 |

문서용 OpenAPI 형태 요약. 완전한 계약 파일이나 생성 결과가 아닌 기존 요청·응답의 일부 표현.

```yaml
paths:
  /trips/{trip_uuid}/places:
    post:
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [lat, lng, recorded_at]
              properties:
                lat: {type: number, minimum: -90, maximum: 90}
                lng: {type: number, minimum: -180, maximum: 180}
                recorded_at: {type: string, format: date-time}
      responses:
        '201':
          description: Existing draft place saved; response includes place_cnt
```

입력값의 실제 생성·검증·오류 계약은 기존 서버 스키마가 원본. userId·멱등 키·Gleo 요청 ID를 새 필드로 끼워 넣지 않는 원칙.

### 5.2 Gleo 공개 Function

| Function | 입력 | 동작 | 완료 기준 |
|---|---|---|---|
| `OPEN_TRAVEL_CRUMBS` | 기존 무파라미터 | 기존 앱 열기 | 기존 처리와 회귀 시험 유지 |
| `RECORD_CURRENT_PLACE` | 무파라미터 | 준비된 현재 여행에 현재 위치 임시 저장 | 기존 UseCase의 저장 확인 |

총 2개로 제안, 나머지 1개 슬롯 미사용. 공식 [App Schema 가이드](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability)의 최대 3개 한도 안에서 범용 명령의 임의 묶음 없음. 기존 `component=activity` 방식 유지안이며 `service` 문자열을 Android Service로 오해하지 않는 원칙.

신규 RECORD는 파라미터 없음/빈 값/`{}`를 무파라미터 입력으로 처리하는 안. JSON 배열·원시값·깨진 JSON 또는 예상 밖 업무 파라미터는 저장 전에 거절. 기존 OPEN의 파라미터 처리와 무관한 호환 동작은 유지. 공식 예시의 `ACTION_PARAM` JSON과 본문의 개별 extras 설명 차이는 실제 수신 형식 시험(T4·T5) 대상.

### 5.3 콜백 계약

기존 Broadcast `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE`의 `packageName`·`response` 사용. `functionName`은 실제 호출 Function과 일치. `status`는 저장 확인 때 `success`, 조건 미충족·처리 실패·결과 미확인 때 `fail`. 추가 입력 없는 한 동작이라 이번 `asking` 단계 없음.

| 결과 | 상태 | 영어 message 제안 | 사용자에게 전달할 의미 |
|---|---|---|---|
| 저장 확인 | success | `The current place has been recorded in the active trip.` | 기록 완료 |
| 입력 오류, 저장 호출 전 | fail | `The recording request could not be read.` | 요청 오류 |
| 준비 상태 미확인, 저장 호출 전 | fail | `Travel Crumbs is not ready to record a place yet.` | 준비 확인 불가 |
| 실제 확인된 진행 여행 없음 | fail | `An active trip is required to record a place.` | 정차 후 여행 시작 필요 |
| 실제 확인된 위치 동의 없음 | fail | `Location consent is required before recording a place.` | 정차 후 기존 동의 화면 필요 |
| UseCase의 일반 실패 | fail | `The app could not confirm that the place was recorded.` | 원인을 추측하지 않는 결과 미확인 |
| 기한 내 결과 없음 | fail | `The recording result could not be confirmed in time. Check the trip before trying again.` | 정차 후 기록 확인 필요 |

기존 `FAILED`만 보고 「GPS 오류」·「서버 오류」를 골라 말하지 않는 원칙. Gleo의 문장 생성은 앱 콜백 사실에 근거하도록 고정 결과 사용. 콜백 전송 성공은 사용자가 음성을 들었다는 확인과 별개.

등록에는 기존 `assets/schema.json`의 Function과 Manifest Action filter의 이름 일치 필요. 공식 [등록 가이드](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema)의 스키마 검토·활성화 및 실제 설치 환경에서 신규 Action 발견 여부는 구현 후 확인 조건.

## 6. 시험 케이스 표

(수정: T18·T19의 진행 중 업무 제한 및 T9의 Gleo 명령별 수집 범위 명확화)

모든 행은 앞으로 수행할 시험 계획이며 본 세션 실행 완료 표시 없음. 단위 시험은 순수 입력·상태·fake 기존 UseCase, 통합/기기 시험은 실제 CRUD·Gleo 왕복으로 구분.

| 번호 | 조건 | 행동 | 결과 | 비고 |
|---|---|---|---|---|
| T1 | 기존 앱 열기 | OPEN 호출 | 기존 화면 표시와 기존 콜백 유지, 장소 POST 없음 | 회귀 |
| T2 | 일반 런처 진입 | 앱 실행 | Gleo 업무·응답 없음 | 회귀 |
| T3 | 미등록 Action | Intent 전달 | 저장·임의 콜백 없음 | 입력 |
| T4 | 신규 RECORD의 없음/빈 값/`{}` | 각각 전달 | 준비 조건 충족 시 동일 무파라미터 동작 | 단위+기기 |
| T5 | 배열·숫자·깨진 JSON·예상 밖 업무 파라미터 | RECORD 전달 | 저장 호출 전 거절 | 실제 extras 형식 확인 포함 |
| T6 | 동의 확인·내비 준비·RECORDING 여행 | RECORD 호출 | 기존 UseCase 1회, 임시 장소 1행, 기존 장소 수 갱신, success | P0 왕복 |
| T7 | 같은 조건의 PAUSED 여행 | RECORD 호출 | 장소 기록 가능, 경로 자동 기록은 정지 유지 | D27 |
| T8 | 현재 여행 조회 완료·결과 없음 | RECORD 호출 | 장소 저장 없음, 확인된 사전 조건 fail | 초기 null과 구분 |
| T9 | 동의 최신값 N 또는 미이력 | RECORD 호출 | 이번 Gleo 명령에 따른 추가 위치 조회·장소 POST 없음, 정차 후 동의 안내 | 기존 RouteRecorder 전체 수집 차단 보장 아님·D5 |
| T10 | 동의 조회 실패 | RECORD 호출 | ‘미동의’로 단정 없음, 저장 시작 없음 | U3 |
| T11 | 초기화 전 또는 여행 복원 진행 중 | 앱을 Gleo로 새로 실행 | 여행 없음 단정·무조건 성공 없음, 준비 정책의 제한 응답 | U3 |
| T12 | 내비 초기화 미완료 | RECORD 호출 | 과거 좌표·0,0 폴백 없이 제한 응답 | U3 |
| T13 | 현재 위치 0,0·범위 밖·조회 실패 | 기존 기록 동작 호출 | 장소 POST 없음, 원인 미확정 상태에서 일반 실패 | 기존 검증 보존 |
| T14 | 서버가 404·409·422 반환 | RECORD 호출 | success 없음, 현재 포트가 반환하는 수준의 오류 안내 | U1 |
| T15 | 저장 완료 전에 응답 지연 | 기한 직전까지 관측 | 저장 확인 전 success 없음 | 시간 계약 |
| T16 | 위치 또는 저장이 앱 응답 기한 초과 | RECORD 호출 | 기한 내 결과 미확인 fail 목표, 자동 재시도 없음 | U2·시간 측정 |
| T17 | 서버 commit 후 응답 유실 | RECORD 호출 | 미저장 단정·자동 POST 없음, 기존 기록 확인 안내 | 중복 위험 |
| T18 | 시간 초과 fail 뒤 기존 업무 실행 진행 중 | 추가 RECORD와 늦은 결과 관측 | 실제 실행 종료 전 추가 저장 없음, 두 번째 success 콜백·자동 재저장 없음. 종료 후에도 결과 미확인이면 정차 후 확인 안내 | 서버 취소·미저장 보장 없음 |
| T19 | RECORD 처리 중이며 기한 전 또는 fail 콜백 후 | 중복 전달 | 콜백·화면 상태와 무관하게 실제 실행 종료 전 두 번째 저장 시작 없음, 콜백 상관관계의 실제 결과 확인 | D2·동일 프로세스 범위 |
| T20 | 기록 처리 중 구성 변경 | 화면 재생성 | 저장 반복 실행 없음 목표, 상태·SDK 수명 검증 | 기존 Activity 경합 포함 |
| T21 | 앱이 이미 열린 상태 | 신규 Intent 수신 | 최초 실행·재진입 모두 동일 계약, Task/Activity 중복 검증 | launchMode 가정 없음 |
| T22 | 저장 중 프로세스 종료 | 앱 재실행 | 과거 음성 명령 자동 재실행 없음, 저장 여부는 기존 조회 | exactly-once 보장 없음 |
| T23 | 음성과 기존 장소 버튼 동시 사용 | 동시 접수 | 실제 저장 수와 응답의 일치 검증, 범위 판단 기록 | D2 |
| T24 | PARKED→DRIVING→UNKNOWN | 상태 전환 | 주행·UNKNOWN에서 상세 입력 없음, 짧은 상태만 | UI |
| T25 | 화면 표시 해제·복귀 | 결과 상태 소비 | 저장 재실행·음성 중복 출력 없음 | UDF |
| T26 | 장소 저장 후 그림 재조회 실패 | 홈 복귀 | 저장 결과와 그림 갱신 실패 구분, 재저장 없음 | 기존 원본 유지 |
| T27 | 로그·콜백 검사 | 정상/실패 실행 | 좌표·음성 원문·개인정보·UUID 원문 신규 유출 없음 | 개인정보 |
| T28 | 스키마·Manifest·상수 대조 | 설치·Gleo 명령 | Function 이름 일치, 총 2개, 신규 RECORD 발견·호출 확인 | 승인·설치 의존 |
| T29 | 순차 정상 명령 2회 | 첫 완료 후 다시 호출 | 각 별도 의도에 1회씩 저장 가능 | 과잉 중복 제거 금지 |
| T30 | 기존 CRUD 회귀 | 시작·수동 장소 기록·PAUSED·A-06·종료·제목 수정 | 기존 요청·저장 규칙·데이터 유지 | 범위 밖 동작 보호 |

## 7. 미확정 표

(수정: U3의 동의 제한을 Gleo 추가 동작으로 한정하고 기존 RouteRecorder는 D5에 분리)

### 7.1 사용자에게 확인할 핵심 3가지

| 번호 | 질문 | 선택지 | 추천 |
|---|---|---|---|
| U1 | 위치/서버 실패를 현재 UseCase 변경 없이 어떻게 안내할지 | A. 이번에는 확인된 사전 조건+일반 실패 / B. CRUD 담당 검토 후 호환 가능한 결과 확장 | A로 이슈의 오류 구분 수준 조정 승인. 세분화가 필수면 B 승인 전 해당 구현 보류. UseCase 우회·중복 내비 조회·새 POST로 원인 추정 금지 |
| U2 | 앱의 응답 기한을 어떻게 잡을지 | A. 내부 6초 목표를 실기로 검증 / B. 다른 측정 기반 목표 | A를 설계안으로 검토. 6초는 공식 수치가 아니며 전체 7초 보장과 구별. 늦은 결과는 미확인 안내·자동 재시도 없음 |
| U3 | 콜드스타트까지 성공 범위에 포함할지 | A. 동의·여행·내비 준비 확인된 세션을 P0 / B. 앱을 새로 켠 직후의 준비 완료까지 포함 | A 추천. 미준비는 제한 응답. 초기 null로 여행 없음 판정 금지. 동의 미확인 시 이번 Gleo 명령에 따른 추가 위치 조회·장소 POST 없음 |

추천은 승인된 결정과 구별. 답 없는 항목을 완료 조건 충족으로 계산하지 않는 원칙.

### 7.2 팀·실행 환경의 의존 조건

| 번호 | 확인할 조건 | 처리 기준 |
|---|---|---|
| D1 | 기존 화면에 명령 처리 상태를 붙이는 최소 변경 위치 | 홈·주행의 짧은 상태 표시만 목표. 전체 음성 UI·마이크·듣기 시작 기능으로 확대 없음. 기존 VM 변경 필요분은 CRUD 담당 검토 |
| D2 | 식별자 없는 중복 수신·수동 버튼 동시 조작·콜백 상관관계 | 같은 프로세스의 진행 중 음성 재실행 제한까지만 목표. 실제 기기 결과 기록. API 멱등 계약 확장은 이번 범위 밖 |
| D3 | 구형 하네스의 서버 좌표 금지와 현재 CRUD의 충돌 | develop의 하네스 반영 및 `0015-server-postgres-as-source-of-truth.md` 확인 후 팀 동기화/담당 검토 필요. ADR 문서 자체의 승인 상태와 반영 사실 구별. 기존 CRUD 되돌림·이번 브랜치의 하네스 수정·센서 예외 추가 없음 |
| D4 | 생성 OpenAPI와 export 도구의 기준 브랜치 부재 | 팀의 계약 생성 기반 반영 필요. 이번 스펙은 router/schema/Retrofit 대조 요약까지만. 수동 `openapi.json` 작성 없음 |
| D5 | 실행 환경·스키마 활성화·실제 성능·기존 기록기의 동의 검사 | 현재 정적 검토만. 구현 전 doctor와 기존 CRUD 기준 시험, 신규 Action 등록·호출·시간 측정 필요. `MainActivity`가 Gleo 검사 전에 시작하는 기존 `RouteRecorder`는 여행 복원 후 별도 동의 재확인 없이 위치 조회 가능. 해당 동의 준수는 기존 CRUD 담당 검토 조건이며 이번 기능에서 수정 범위 확대 없음 |

## 8. 완료 조건

### 8.1 이번 산출물 — 스펙 검토 단계

- [ ] 이슈 #25 범위와 기준 SHA에 맞춘 사용자 스펙 검토 완료.
- [ ] U1~U3에 대한 사용자 결정 기록 또는 보류 명시.
- [ ] 기존 CRUD 경로·임시 장소·PAUSED 규칙·서버/데이터 변경 없음 확인.
- [ ] 공식으로 확인한 Gleo 제약, 코드로 확인한 상태, 구현 제안, 미검증 항목의 구분 확인.
- [ ] 사용자 재확인 전 PR·구현 없음.

### 8.2 향후 app 구현의 완료 조건

- [ ] 허용된 OPEN·RECORD만 처리하며 실제 저장 확인 후 성공 응답.
- [ ] 준비 상태와 실패 안내가 U1~U3의 승인 결정과 일치.
- [ ] 실제 Gleo 왕복·시간 초과·중복·수명·주행 UI·개인정보·기존 CRUD 회귀 시험 결과 기록.
- [ ] `doctor.py` 환경 점검과 기존 `testDebugUnitTest` 기준 상태 확인.
- [ ] 하네스의 `lint testDebugUnitTest assembleRelease` 및 매니페스트·비밀·위생·계약 등 적용 센서 녹색 확인. 기존 실패를 새 기능 통과로 숨기거나 baseline/허용 옵션 추가 없음.
- [ ] 센서나 하네스 충돌 미해소 시 구현 완료·PR 준비 완료 표시 보류.

### 8.3 server 영역

- [ ] 새 서버 코드·테이블·마이그레이션·REST API 변경 없음 확인.
- [ ] 기존 장소 POST의 commit·임시 기록·장소 수 반영을 실제 DB와 기존 조회로 확인.
- [ ] 계약 생성 기반 부재·기존 서버 센서 실패는 담당 팀의 해결 상태 별도 기록. 이번 기능이 서버 전체 품질을 검증했다고 표시하지 않음.

현재 달성 범위는 문서·코드의 정적 대조와 검토용 스펙 작성까지이며, 상기 구현·시험 체크의 완료 없음. `doctor` FAIL 항목은 이 실행 세션에서 확인할 수 없는 JDK·SDK·Pleos 이미지·AVD·Docker·gh 인증 및 Git 설정/ZIP 사본 항목으로, 사용자 PC 전체의 미설치 확정과 구별.

읽은 문서: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability · https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback · https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples · https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema · https://developer.android.com/topic/architecture/recommendations · https://developer.android.com/topic/libraries/architecture/saving-states
