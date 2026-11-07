# Gleo AI 연결 지점 인계 (데이터 CRUD 작업 → Gleo AI 입출력 담당)

시나리오 1(데이터 CRUD) 작업 중 Gleo AI 가 들어갈 자리를 모아 둔 문서. Gleo 담당 팀원과 그 AI 가 읽고 이어서 구현하도록 작성함.
묶음이 진행될 때마다 이 문서에 추가함.

## 원칙

- 앱은 Gleo 가 들어갈 자리를 **인터페이스(domain)** 로만 둠. 지금 구현은 임시(입력을 그대로 돌려줌).
- Gleo 담당은 **구현 클래스를 새로 만들고 Hilt 바인딩 한 줄만 교체**하면 됨. 화면 · ViewModel · DB 는 건드리지 않아도 됨.
- 추천이 실패하거나 늦으면 앱은 지금 값을 그대로 씀(화면이 멈추지 않음).
- 저장 기준은 ERD v0.3 + `docs/data/data-decision-log.md`(D 번호).

## 연결 지점

| 번호 | 화면 | 상태 | 하는 일 |
|---|---|---|---|
| G1 | A-04 여행 시작 설정 | 자리 있음(묶음 3) | 여행명 「수정」 시 이름 추천 |
| G2 | A-08 여행 종료 확인 | 예정(묶음 6) | 들른 장소 이름으로 여행명 추천 |

### G1 여행명 추천 — A-04 「수정」

- 인터페이스: `app/src/main/java/com/example/travelcrumbs/domain/TripNameSuggester.kt`
  ```kotlin
  fun interface TripNameSuggester {
      suspend fun suggest(current: String): String
  }
  ```
- 지금 구현: `data/trip/PassThroughTripNameSuggester.kt` — `current` 를 그대로 돌려줌
- 교체 위치: `di/TripModule.kt` 의 `bindTripNameSuggester`
- 호출: `ui/tripsetup/TripSetupViewModel.onEditName()` — 「수정」을 누르면 추천을 받아 입력칸에 넣고 입력칸을 엶. 사용자가 그 뒤 고칠 수 있음
- 입력: 지금 화면의 여행명(기본값 「2026.11.07 여행」 — 날짜만 · 결정 로그 D11)
- 출력 제약: 100자 이하(`trip_mst.trip_nm varchar(100)` · 앱이 100자로 자름). 예외가 나면 앱이 `current` 를 씀
- 저장: 사용자가 「여행 시작」을 누르면 최종 이름이 `POST /trips` → `trip_mst.trip_nm`
- 참고: 시작 시점에는 여행지 정보가 없음(시작 위치는 보통 출발지). 의미 있는 추천은 G2 에서 가능

### G2 여행명 추천 — A-08 여행 종료 (예정)

- 생각: 여행을 마칠 때 들른 장소 이름을 Gleo 에 넘겨 여행명 추천(예: 「강릉 중앙시장 외 2곳」 수준보다 자연스러운 이름)
- 입력 후보: `trip_place_dtl`(기록 순서 = `recorded_at`) → `place_mst.place_nm`(장소 연결은 A-06 · 묶음 5), `trip_mst.start_at · end_at`, `trip_companion_dtl.companion_type_cd`(SOLO · SPOUSE · CHILD · PARENT · PET)
- G1 인터페이스를 넓혀 쓸지(입력에 장소 목록 추가) 따로 둘지는 묶음 6 에서 정함
- 주소 재료: 내비 현재 위치(NaviHelper `getCurrentLocationInfo` · 권한 NAVI_ROUTE)에 `address` 가 있음 → 앱 모델 `domain/model/NaviLocation.address`. 장소 이름이 없는 기록(장소 연결 전)도 주소로 지역을 알 수 있음. 지금은 DB 에 저장하지 않음(trip_place_dtl 에 주소 컬럼 없음) — 쓰려면 저장 여부를 지수와 정함
- 저장: 여행명 변경 = 기존 값 수정 → 공통 변경 이력(결정 로그 D17) 대상

## Gleo 담당이 정할 것

- 추천 품질 확인용 기록: 추천 이름과 사용자가 최종으로 정한 이름을 남길지(남긴다면 어떤 테이블에 · 데이터 표준은 지수 확인)

## 이미 있는 Gleo 관련 코드 (데이터 작업 전부터 있던 것)

- `app/src/main/assets/schema.json` — Gleo Interface function 정의(`OPEN_TRAVEL_CRUMBS`)
- `app/src/main/java/com/example/travelcrumbs/data/gleo/` — Gleo Action 파싱 · 콜백
- `domain/StartVoiceInputUseCase.kt` — 음성 입력 자리(지금은 「준비 중」). A-05 음성 장소 기록과 연결될 수 있음 → 묶음 4 에서 확인 후 이 문서에 추가
