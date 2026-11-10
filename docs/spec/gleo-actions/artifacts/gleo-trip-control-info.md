# Gleo 음성으로 여행 제어 · 오늘 기록 · 근처 추천 — 함수 3개 재구성 스펙

- 이슈: #69 · 기능 브랜치 `gleo-actions/main`(develop `09f0032`에서)
- 앞선 작업: #25 스펙(#26) · 구현(#27) · develop 병합(#68) — `docs/spec/gleo-ai/artifacts/gleo-crud-foundation.md`
- 공식 계약: [App Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability)(함수 최대 3개 · 파라미터 `string` + `enum`) · [Callback](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback) · [예제](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples)

## 1. 요약

Gleo로 부를 수 있는 함수를 3개로 다시 짠다. 음성 인식·의도 매칭·말하기는 Gleo가 하고, 앱은 Action을 받아 **화면 버튼과 같은 저장소·규칙**으로 처리한 뒤 7초 안에 콜백한다.

| 함수 | 파라미터 | 하는 일 | 화면 버튼 대응 |
|---|---|---|---|
| `RECORD_CURRENT_PLACE` | 없음 | 지금 여행에 현재 위치를 임시 장소로 기록(#25 그대로) | 「이 장소 기록」 |
| `CONTROL_CURRENT_TRIP` | `operation`: `PAUSE` · `RESUME` · `END` (필수) | 일시정지 · 재개는 바로, 종료는 **확인 팝업만 띄움** | 「기록 일시정지」 · 「기록 재개」 · 「여행 종료」 |
| `SHOW_TRAVEL_INFO` | `infoType`: `TODAY_RECORDS` · `RECOMMENDED_ROADMAPS` (필수) · `region`: 지역 이름(선택) | 오늘 기록 요약 · 추천 로드맵 상위 3개를 말로 알려 줌. 말한 지역이 있으면 그 지역(「경주 근처」 → 경주), 없으면 현재 위치에서 가장 가까운 지역. 정차면 해당 화면도 엶 | 「내 여행」 · 「추천」 |

`OPEN_TRAVEL_CRUMBS`는 뺀다. Gleo는 설치 앱 목록으로 앱을 직접 실행한다(Gleo 로그 `[AppLibrary] buildLaunchApps(Travel Crumbs)`). 「장소 기록한 거 보여줘」처럼 화면을 보자는 말은 `SHOW_TRAVEL_INFO(TODAY_RECORDS)`가 받는다.

### 1.1 흐름

```
발화 → Gleo 인식·함수 선택 → Intent action "com.example.travelcrumbs.<함수>" + extra ACTION_PARAM(JSON 문자열)
     → MainActivity → GleoIntentReader(접두어 제거 · 함수명 확인) → GleoActionParser(파라미터 검증)
     → GleoCommandProcessor(한 번에 한 명령 · 7초 기한)
        ├ RECORD   → GleoRecordingReadiness → RecordPlaceUseCase(TripPlaceRecorder)
        ├ CONTROL  → RecordingRepository.togglePause() / 종료 확인 요청
        └ INFO     → TripRepository.trips · RecommendationRepository + NaviRepository.currentLocation
     → GleoCallback(functionName · status · message[영어] · data[JSON 배열 문자열]) → Gleo가 말함
     → 화면 한 줄 안내(GleoNoticeLine) · 정차면 화면 이동
```

### 1.2 실행 현실성(2026-11-10 에뮬레이터 실측)

- 실제 Gleo가 우리 schema.json을 부팅 때 읽고 함수 매칭까지 함. 승인 절차가 로컬 호출을 막지 않음.
- Intent action은 「패키지명.함수명」. 파라미터 없는 함수에도 `ACTION_PARAM` extra 하나가 옴.
- 「이 장소 기록해줘.」 인식 완료 → 콜백 수락까지 약 2초.
- 「여행가자고」는 Gleo 내비 도메인, 잡담은 chitchat으로 감 — 우리 함수 설명은 다른 앱 기능과 겹치지 않게 「Travel Crumbs trip record」를 분명히 씀(4.1).

## 2. 화면

새 화면은 없다. 기존 화면과 `GleoNoticeLine`(하단 한 줄 · #25 U3)만 쓴다.

| 상황 | 정차(PARKED) | 주행 · UNKNOWN |
|---|---|---|
| PAUSE · RESUME | 기록 칸 상태가 바뀌고 한 줄 「기록을 잠시 멈췄어요」 · 「기록을 다시 시작했어요」 | 주행 전용 화면 버튼 상태가 바뀌고 같은 한 줄 |
| END | 기록 칸의 기존 「현재 여행 기록을 종료할까요?」 팝업(A-04)을 띄움 | 주행 전용 화면의 기존 종료 확인을 띄움(이미 주행 화면에 있는 동작과 같음) |
| TODAY_RECORDS | 「내 여행」 칸으로 이동 + 한 줄 「오늘 n곳 기록했어요」 | 화면 이동 없음 · 한 줄만 |
| RECOMMENDED_ROADMAPS | 「추천」 칸을 그 지역으로 열기 + 한 줄 「○○ 추천 코스를 열었어요」 | 화면 이동 없음 · 한 줄 「추천 코스는 정차 후 볼 수 있어요」 |

주행 중 화면 전환을 하지 않는 이유: 주행 중 허용 칸은 「기록」뿐(app-shell-v5 2-8 · DO).

## 3. 상태 · 규칙

### 3.1 공통

- 한 번에 한 Gleo 명령(#25 `recording` 잠금을 모든 함수로 넓힘). 처리 중 다른 명령이 오면 즉시 `fail` + 「Another request is still being processed.」
- 7초 기한·늦은 결과 규칙은 #25 그대로(U2). 읽기 전용(INFO)은 기한 넘으면 미확인 없이 `fail`.
- 파라미터가 없거나 enum 밖이면 `fail` + 입력 실패 문장, 아무것도 바꾸지 않음.

### 3.2 CONTROL_CURRENT_TRIP

| operation | 전제 | 동작 | status · message |
|---|---|---|---|
| PAUSE | 지금 여행 RECORDING | `togglePause()` 한 번 | success · 「The trip recording is paused.」 |
| PAUSE | 이미 PAUSED | 바꾸지 않음 | success · 「The trip recording is already paused.」 |
| RESUME | 지금 여행 PAUSED | `togglePause()` 한 번 | success · 「The trip recording has resumed.」 |
| RESUME | 이미 RECORDING | 바꾸지 않음 | success · 「The trip recording is already running.」 |
| END | 진행 중 여행 있음 | 종료 확인 팝업 요청만(저장소 변경 없음) | success · 「Please confirm ending the trip on the screen.」 |
| 모두 | 진행 중 여행 없음 | 없음 | fail · 「There is no trip in progress.」 |

`togglePause()`는 토글이라 **현재 상태를 읽고 맞을 때만** 부른다(같은 말을 두 번 해도 반대로 뒤집히지 않음).

### 3.3 SHOW_TRAVEL_INFO

- **TODAY_RECORDS:** `TripRepository.trips`에서 오늘(기기 시간대 자정 이후) 기록한 장소 수 합계 + 지금 여행 상태·장소 수. data = `[{"todayPlaceCount":n,"currentTripStatus":"RECORDING|PAUSED|NONE","currentTripPlaceCount":m}]`. message 예: 「You recorded 3 places today. The current trip is recording with 2 places.」
- **RECOMMENDED_ROADMAPS:** 지역을 고르는 순서(U3 결정).
  1. **말한 지역(`region`)이 있으면 그 지역.** 예: 「경주 근처 추천 코스」 → `region="경주"` → 경북 경주시 로드맵.
     - 맞추는 법: 공백과 「특별시 · 광역시 · 특별자치도 · 도 · 시 · 군 · 구」 꼬리를 떼고 시드의 시군구 이름과 먼저, 없으면 시도 이름(「서울」 · 「제주」)과 비교. 시도로 맞으면 그 시도 전체.
     - 여러 시군구가 맞으면(예: 「중구」) 함께 말한 시도가 있으면 그쪽, 없으면 맞은 곳을 모두 합쳐 순위.
     - 맞는 지역이 시드에 없으면 `fail` · 「There are no recommended roadmaps for <region> yet.」(지어내거나 다른 지역으로 바꾸지 않음).
  2. **말한 지역이 없으면 현재 위치에서 가장 가까운 시군구.** `NaviRepository.currentLocation()`(앱 예산 안 3초) → 시드 로드맵 첫 장소와의 거리로 가장 가까운 시군구. 위치를 못 읽으면 전체에서.
  3. 그 지역 로드맵을 기존 `RecommendationRules.rank`(별점순)로 상위 3개.
  4. data = `[{"tripNm":"…","region":"경북 경주시","ratingAvg":4.6,"placeCount":4}, …]`, message 예: 「Top roadmaps in Gyeongju-si: A, B and C.」
  5. 좌표는 콜백·로그에 넣지 않음(지역 이름만).
- 시드는 「시연용 데이터」 — 콜백 message에는 넣지 않고, 화면은 기존 「시연용 데이터」 표시를 그대로 씀.

### 3.4 release 빌드

| 함수 | release | 이유 |
|---|---|---|
| RECORD_CURRENT_PLACE | 차단(현행 R1 유지) | 다른 UID 앱 호출 차단 미확인 · 기록을 바꿈 |
| CONTROL_CURRENT_TRIP | 차단 | 같은 이유 · 여행 상태를 바꿈 |
| SHOW_TRAVEL_INFO | **허용(U1)** | 읽기 전용 · 바뀌는 데이터 없음 |

## 4. 데이터 · 계약

### 4.1 schema.json

debug(`app/src/debug/assets/schema.json`) — 3개. release(`app/src/main/assets/schema.json`) — U1 결정대로 `SHOW_TRAVEL_INFO` 1개. 설명은 영어 한두 문장·예시 없음(공식 가이드).

```json
{ "name": "RECORD_CURRENT_PLACE",
  "description": "Records the vehicle's current location as a draft place in the active Travel Crumbs trip.",
  "parameters": { "type": "object", "properties": {}, "required": [] }, "component": "activity" }
{ "name": "CONTROL_CURRENT_TRIP",
  "description": "Pauses, resumes, or asks to end the trip that Travel Crumbs is currently recording.",
  "parameters": { "type": "object",
    "properties": { "operation": { "type": "string", "enum": ["PAUSE","RESUME","END"],
      "description": "What to do with the current Travel Crumbs trip recording." } },
    "required": ["operation"] }, "component": "activity" }
{ "name": "SHOW_TRAVEL_INFO",
  "description": "Tells the user today's Travel Crumbs trip records or recommended travel roadmaps near the vehicle.",
  "parameters": { "type": "object",
    "properties": {
      "infoType": { "type": "string", "enum": ["TODAY_RECORDS","RECOMMENDED_ROADMAPS"],
        "description": "Which Travel Crumbs information to tell the user." },
      "region": { "type": "string",
        "description": "Korean city, county, district or province name the user asked about for recommended roadmaps; omit when the user did not name a place." } },
    "required": ["infoType"] }, "component": "activity" }
```

### 4.2 매니페스트 · 상수

- intent-filter: 함수마다 접두어 있는 꼴(`com.example.travelcrumbs.<함수>`)과 없는 꼴. RECORD · CONTROL은 debug 매니페스트에만, INFO는 main.
- `AppConfig`: `GLEO_FUNCTION_*` 3개 · 메시지 상수(영어 문장) · OPEN 상수 삭제.
- `GleoActionParser`: `ACTION_PARAM` JSON 객체에서 함수별로 정한 키만 받음(`operation` · `infoType` enum, `region` 문자열 50자 이내). 알 수 없는 키는 무시하지 않고 거절(#25 T5와 같은 엄격함).

### 4.3 저장 · 수집

새로 저장하는 값 없음. 읽기: Room 여행·장소, 추천 시드, 내비 현재 위치(메모리에서 거리 계산 뒤 버림). 서버 전송 없음.

## 5. 시험 케이스

| # | 장면 | 입력 | 기대 |
|---|---|---|---|
| T1 | 접두어 action | `com.example.travelcrumbs.CONTROL_CURRENT_TRIP` + `{"operation":"PAUSE"}` | 함수 인식 |
| T2 | 파라미터 누락·enum 밖 | `{}` · `{"operation":"STOP"}` | fail · 상태 변화 없음 |
| T3 | PAUSE(RECORDING) | — | PAUSED · success |
| T4 | PAUSE 두 번 | — | 두 번째 「already paused」 · 여전히 PAUSED |
| T5 | RESUME(PAUSED) | — | RECORDING · success |
| T6 | END | — | 종료 확인 팝업 표시 · 여행 상태 그대로 |
| T7 | 진행 중 여행 없음 | PAUSE · RESUME · END | fail 「no trip in progress」 |
| T8 | TODAY_RECORDS | 오늘 2여행에 장소 1·2 | todayPlaceCount 3 |
| T9 | TODAY_RECORDS 자정 경계 | 어제 23:59 장소 | 오늘 수에 안 셈 |
| T10 | RECOMMENDED 지역 말함 | `region="경주"` · `"경주시"` · `"경북 경주"` | 경북 경주시 로드맵 상위 3개(별점순) · 현재 위치 안 씀 |
| T10a | RECOMMENDED 시도만 | `region="제주"` | 제주 전체(제주시 · 서귀포시) 상위 3개 |
| T10b | RECOMMENDED 시드에 없는 지역 | `region="대구"` | fail 「no recommended roadmaps for 대구」 · 화면 이동 없음 |
| T10c | RECOMMENDED 지역 안 말함 · 위치 있음 | 서울 강남 근처 좌표 | 「서울 강남구」 상위 3개 |
| T11 | RECOMMENDED 위치 실패 | 내비 null | 전체 상위 3개 · success |
| T12 | 주행 중 INFO | DRIVING | 화면 이동 없음 · 한 줄 |
| T13 | 정차 중 INFO | PARKED | 내 여행 · 추천 칸 이동 |
| T14 | 동시 명령 | RECORD 처리 중 CONTROL | 두 번째 즉시 fail |
| T15 | release | 빌드 산출물 | 스키마·매니페스트에 U1 결정 함수만 |
| T16 | 실제 음성 | 「기록 잠깐 멈춰 줘」 · 「다시 기록해 줘」 · 「여행 끝낼게」 · 「오늘 몇 군데 기록했어?」 · 「근처 추천 코스 알려 줘」 · 「경주 근처 추천 코스 알려 줘」 | 각각 해당 함수(마지막은 `region` 경주) · 7초 안 수락 |
| T17 | OPEN 제거 | 「여행 부스러기 열어 줘」 | Gleo 기본 앱 실행으로 열림(우리 함수 호출 없음) |

## 6. 사람 결정(2026-11-10 · 스펙 PR #70)

| # | 질문 | 결정 |
|---|---|---|
| U1 | release 에서 `SHOW_TRAVEL_INFO`만 허용할까 | 허용. RECORD · CONTROL 은 R1 확인 전까지 debug 만 |
| U2 | 음성 END 를 확인 팝업 없이 바로 끝낼까 | 팝업만 |
| U3 | 「근처」 기준 | 말한 지역이 있으면 그 지역(「경주 근처」 → 경주시), 없으면 현재 위치에서 가장 가까운 시군구 1곳 |
| U4 | OPEN 없이 「여행 부스러기 열어 줘」가 열리는지 | OPEN 을 빼고 진행 · T17 실기 확인. 안 열리면 그때 다시 결정 |

## 7. 완료 조건

- [ ] schema · 매니페스트 · 상수 3함수로 정리, OPEN 제거
- [ ] 파서 · 프로세서 · 함수별 처리기와 단위 시험 T1~T15
- [ ] 정차·주행 화면 동작(2절)
- [ ] 실제 Gleo 음성 T16 · T17 에뮬레이터 확인, 콜백 7초 안 수락
- [ ] 센서 전부 녹색 · release 산출물 확인
