# Gleo AI 참고 사항

작성: 2026-11-08

데이터 CRUD 작업 중 검토한 Gleo AI 관련 내용(참고용).

## 1. 참고할 수 있는 코드 위치

| 번호 | 화면 | 인터페이스 | 현재 구현 | 바인딩 위치 |
|---|---|---|---|---|
| G1 | A-04 여행 시작 설정 「수정」 | `domain/TripNameSuggester` | `data/trip/PassThroughTripNameSuggester`(입력값 그대로 반환) | `di/TripModule.kt` `bindTripNameSuggester` |
| G2 | A-08 여행 상세 「제목 수정」 | `domain/TripNameSuggester`(G1 과 동일) | 동일 | 동일 |
| G3 | 주행 화면 「음성 입력」 | `domain/StartVoiceInputUseCase` | `NotReadyStartVoiceInputUseCase`(「준비 중」) | `di/ShellModule.kt` `bindStartVoiceInputUseCase` |
| — | Gleo 로 앱 열기 | `assets/schema.json`(`OPEN_TRAVEL_CRUMBS`) · `data/gleo/` | 동작함 | `di/GleoModule.kt` |

파일 경로는 `app/src/main/java/com/example/travelcrumbs/` 기준이다.

### 1-1. G1 여행명 — A-04 「수정」

```kotlin
fun interface TripNameSuggester {
    suspend fun suggest(current: String): String
}
```

- 호출: `ui/tripsetup/TripSetupViewModel.onEditName()` — 「수정」을 누르면 반환값을 입력칸에 채운 뒤 입력칸을 연다. 사용자는 이후 직접 수정할 수 있다.
- 입력: 현재 화면의 여행명(기본값은 날짜만 · 예 「2026.11.08 여행」)
- 출력: 100자 이하(`trip_mst.trip_nm` VARCHAR(100) · 앱이 100자로 자름). 예외가 발생하면 앱은 입력값을 그대로 사용한다.
- 저장: 「여행 시작」 시 `POST /trips` → `trip_mst.trip_nm`
- 여행 시작 시점에는 여행지 정보가 없다(위치는 보통 출발지).

### 1-2. G2 여행명 — A-08 「제목 수정」

- 호출: `ui/tripdetail/TripDetailViewModel.onEditTitle()` — G1 과 같은 `suggest(current)` 를 호출한다(입력 = 현재 여행명).
- 저장: `PUT /trips/{trip_uuid}` → `trip_mst.trip_nm` 수정, 공통 변경 이력 `data_change_hist` 1행(서버 처리)
- 현재 인터페이스는 입력이 여행명 하나이므로, 여행 정보를 활용하려면 인터페이스 확장이 필요하다.

### 1-3. G3 음성 입력 — 주행 화면

- 호출: `ui/driving/DrivingViewModel.onVoiceInput()`
- 반환: `ActionResult`(`NOT_READY` 이면 화면에 「준비 중」 안내)

## 2. 활용 아이디어


- **여행명 기본값:** 여행명 기본값은 날짜만 사용하며 지역명 자동 생성은 하지 않기로 결정하였다(결정 로그 D11). 여행 시작 시점에는 출발지 정보만 있어 지역명이 의미 없고, 사용자가 Gleo AI 와 대화하며 이름을 정하는 방식이 더 적합하다고 보았다.
- **여행명 추천 시점:** 여행 종료 후(G2)에는 활용 가능한 정보가 많다.
  - 방문 장소: `trip_place_dtl.user_place_nm` · `place_mst.place_nm`(방문 순서 = `recorded_at`)
  - 일시: `trip_mst.start_at` · `end_at`
  - 동행자: `trip_companion_dtl.companion_type_cd`(SOLO · SPOUSE · CHILD · PARENT · FRIEND · PET)
- **주소 정보:** 내비 현재 위치(`NaviHelper.getCurrentLocationInfo` · 권한 NAVI_ROUTE)에 `address` 가 포함되어 있다(`domain/model/NaviLocation.address`). 이름이 없는 장소도 지역을 알 수 있으나, 현재 DB 에는 저장하지 않는다. 저장이 필요하면 인계서(`docs/data/handoff.md`) 7장 절차를 따른다.
- **변경 이력:** 여행명 수정은 공통 변경 이력(`data_change_hist`)에 기록된다. 추천 이름과 최종 이름을 별도로 기록할지(추천 품질 확인용)는 검토 대상이며, DB 를 바꾸면 인계서 7장 절차를 따른다.
- **디자인 시스템 기준(지시서 19장):** Gleo AI 상태는 대기 · 듣는 중 · 처리 중 · 입력 확인 · 완료 · 실패로 구분한다. 원형 마이크 버튼, 짧은 상태 문구, 인식 결과 텍스트 표시와 확인 · 수정, 시작 · 중단 수동 버튼, 실패 시 화면 입력 전환, 긴 대화보다 단일 명령 중심. 주행 중에는 음성 입력이 주 입력이다(9장).
- **차량 신호 연계 아이디어(미확정):**
  - A-05 기록 중: 기어 P(정차) 감지 시 「방금 장소 기록할까요?」 질문
  - A-06 장소 경험: 와이퍼 작동 시 「비 오는 날 방문」 태그 제안
  - A-08 여행 종료: 시동 꺼짐 · 문 열림 시점에 여행 종료 확인
  - 동행자: 좌석 점유 정보로 동행 인원 기본값 제안(차종 지원 여부 확인 필요)

## 3. 기존 Gleo 관련 코드

- `app/src/main/assets/schema.json` — Gleo Interface function 정의(`OPEN_TRAVEL_CRUMBS`)
- `data/gleo/` — Gleo Action 파싱 · 콜백
- `domain/StartVoiceInputUseCase.kt` — 음성 입력 연결 지점(G3)
