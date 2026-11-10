- 이슈: #66
- 기획: 없음(별도 기획 폴더 없음 · 이슈 본문이 범위). 연관으로 읽음 — `docs/design/wireframes-v6.md`(A-01 · A-02 · A-03 · I-05) · `docs/spec/navigation/artifacts/set-destination.md`(3.2 · 4.4 · 결정 기록 U4 · U21 · U42) · `origin/home-nearby/main` 의 `docs/spec/home-nearby/artifacts/nearby-roadmaps.md`(동의 · 위치 규칙)
- 데이터 표준: v0.35

# 내비 목적지 자동 장소 기록(navi-autorecord)

산출물 경로: `docs/spec/navi-autorecord/artifacts/auto-place-record.md`(새 파일)

「(수정: 도착 뒤 재출발)」 = 2판 고침. 이유: PR #67 리뷰(희주 님 — 안내 시작 때 기록하면 순서 · 취소 · 수동 중복 · 기록 시각이 어긋남) + 사람 결정(기록 시점 = 목적지 도착 뒤 차를 다시 출발시킬 때).

## 1. 요약 (수정: 도착 뒤 재출발)

- 기록 중(RECORDING)인 여행에서 운전자가 **차량 내비 앱에서 직접** 시작한 안내가 **목적지에 도착**하면 그 도착(목적지 이름 · 주소 · 좌표 · 도착 시각)을 기기 안 「기록 대기」로 기억 → 그 뒤 **차를 다시 출발시킬 때**(도착 뒤 정차(P)를 거쳐 처음 주행으로 바뀜 · 3.4) 현재 여행의 장소 1곳(`trip_place_dtl` · `draft_yn = Y` · `recorded_at` = 도착 시각)으로 자동 기록. 안내 시작 때는 기록하지 않음 · 취소 · 도착 못 한 목적지 · 놓친 도착은 기록 안 함.
- 위치 수집 동의(`LOCATION_COLLECT`)가 있을 때만 · 현재 여행 장소(수동 「이 장소 기록」 포함)와 100 m 안이면 한 번만 · 이 앱이 요청한 안내(추천 「안내 시작」 · 길안내 입구)는 기존대로 핀만 · 중간에 들른 곳은 지금처럼 수동 「이 장소 기록」(장소 순서 = 기록 순).
- 확정은 주행으로 바뀌는 순간이라 화면 알림 · 입력 없음(장소 수만 늘어남) · 정차 뒤 「이어서 쓰기」 → A-03 으로 이름 · 태그 · 메모. 서버 · API · DB 표 · 표준 단어 · 권한 변경 없음. 「재출발」 신호 = 이미 읽는 기어 신호(시동 신호는 SDK 공개 API 에 없고 AAOS 경로는 새 권한 · N1).

### 1.1 코드 · SDK 로 확인한 사실 (수정: 도착 뒤 재출발 — F17 ~ F25 추가)

| 번호 | 사실 | 근거 |
|---|---|---|
| F1 | NaviHelper 3.0.5 리스너의 알림 메서드: `onRouteStarted(RouteStartInfo)` · `onRouteEnded()` · `onRoutingCompleted()` · `onReRouteEnded()` · `onRouteOptionChanged(RouteOption)` · `onRouteCancelled()` · `onWaypointChanged(…)` · `onDestinationArrived(DestinationArrivedInfo)` · `onWayPointArrived(WaypointArrivedInfo)` · `onBookMarkChanged()` · `onDestinationChanged()`(인자 없음) · `onError(NaviErrorCode)` · 조회 응답 `onDestinationInfo` · `onRouteStateInfo` · `onCurrentLocationInfo` 등. 전부 기본 구현이 있는 메서드(필요한 것만 받음) | Gradle 캐시 `NaviHelper-3.0.5` 클래스(javap) |
| F2 | `addListener` · `removeListener` 에는 권한 표시 없음. `getDestinationInfo` · `getRouteStateInfo` · `getCurrentLocationInfo` = `@RequiresPermission(pleos.car.permission.NAVI_ROUTE)` · `requestRoute` = `NAVI_ROUTE_SEARCH`. 둘 다 매니페스트에 이미 있음 → **새 권한 없음** | javap `-v` · `AndroidManifest.xml` 12 · 14줄 |
| F3 | 내비 연결 구조: 앱 하나가 내비 서비스에 콜백 하나를 등록(`registerCallback(콜백, 패키지 이름)`)하고 모든 응답 · 알림을 한 통로로 받음 — 요청마다 따로 구독하는 구조가 아님. 지금 앱은 리스너를 **조회 · 길안내 요청 때만 잠깐 붙였다 뗌**(상시 리스너 없음). 내비 연결 수명 = 화면(Activity) 생성 ~ 파기(SDK 세션 시작 · 끝 한 곳) | `navi-aidl-3.0.1` `INaviService` · `NaviInfoDataSource.kt` · `PleosSdkSession.kt` |
| F4 | **실측(2026-11-06 · #4)**: 해운대 길안내 요청이 TIMEOUT 된 뒤 사용자가 내비에서 「경복궁」을 직접 검색 · 「안내 시작」 → 이 앱의 살아 있던 리스너가 그 경로의 `onRouteStarted` 를 받음. 앱이 얼려져 있던 동안 온 알림은 얼림이 풀릴 때 받음 | `_workspace/4/02_builder_app_report.md` 「해운대 TIMEOUT 뒤 내비에서 경복궁을 직접 시작」 · `GuideRequestTracker.kt` 163줄 주석 |
| F5 | `RouteStartInfo` 칸 = duration · distance · tollFare · name · remainDestinationChargingValue — **좌표 · 주소 · POI 없음** | javap |
| F6 | `DestinationInfo` 칸 = latitude · longitude · displayLatitude(null 가능) · displayLongitude(null 가능) · name · address · remainDistance · remainTime · tollFare · poiId · poiSubId · poiProvider | javap(`navi-data-3.0.4`) |
| F7 | 「안내 중」 판정 = 내비 모드 `ROUTE_GUIDING` · `SIMULATION_GUIDING`(`isRouting` 은 안내 중에도 false 였음 · 2026-11-06 실측). 안내 중이 아니면 목적지를 묻지 않는 규칙이 이미 있음 | `NaviInfoDataSource.activeRoute` |
| F8 | 실측 콜백 순서(앱 요청 때): 경로 탐색 → 내비 경로 선택 화면(자동 시작 카운트다운 약 6 ~ 8초) → `onRouteStarted` → **곧바로 `onRouteEnded`** · `onNavigationStatus(ROUTE_DRIVE)` · 이미 안내 중이면 먼저 `onRouteCancelled`. → `onRouteEnded` 는 「안내가 끝남(도착)」 신호로 쓸 수 없음. Pleos 문서는 `onRouteStarted` 를 「경로 탐색을 시작하면 호출」로 적음 — 실측과 어긋남 | set-destination 354줄 · NaviHelper destinations 문서 |
| F9 | 내비는 `onError` 를 모든 구독자에게 보냄 → 조회 중에 온 오류는 길안내 요청 결과로 쓰지 않는 규칙(조회 창)이 있음. 얼림이 풀린 직후 쌓인 콜백이 먼저 지나가도록 조회 전 1.5초 기다리는 규칙이 있음 | `NaviQueryWindow.kt` · `AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS` |
| F10 | 「이 장소 기록」: 진행 중(RECORDING · PAUSED)이면 장소 1행(`draft_yn = Y` · 이름 = 주소에서 만든 처음 값 · 순서 = 다음 번호) · 동의 없으면 **위치 없이** 장소만 · 주행 · 모름이면 「이어서 쓰기」 칸에 그 장소를 남김(칸은 하나 · 새로 쓰면 덮어씀) · 정차면 화면이 바로 A-03 | `TripPlaceRecorder.kt` · `UnfinishedInputRepository.kt` |
| F11 | 이 앱이 요청한 안내가 시작되면 **핀만** 남고(기록 중일 때만) `trip_place_dtl` 행은 생기지 않음. 요청 상태 = 대기 · 요청 중(X) · 시작됨(X) · 실패(X) + TIMEOUT 뒤 5분 기억. 「내비가 X 로 안내 중」 판정 = 안내 중 + 목적지 이름 같음 + 좌표 100 m 안 | `GuideRequestTracker.kt` · `PinRepository.kt` · `RouteMatchRules.kt` |
| F12 | 기기 `trip_place_dtl` 칸 = trip_place_no · trip_no · place_ord · user_place_nm · memo · draft_yn · lat · lng · addr · recorded_at · 공통 칸 · deleted_yn. **POI · 출처 · 확정 여부 칸 없음**. 지금은 기기 안 Room 이 임시 원본(서버 API 전) · 서버로 보내지 않음 | `TravelTables.kt` · `TripRepository.kt` 33줄 |
| F13 | 위치 동의 철회(C-03): 내 장소의 좌표 · 주소와 「주소에서 만든 처음 이름」만 지움 · 사람이 고친 이름은 남김 | `TripRepository.clearLocations` |
| F14 | 주행 화면 「내비 안내」 카드는 화면이 앞에 있을 때 15초마다 내비 안내 상태를 다시 읽음(#51 U0) — 이번 기능은 이 주기를 쓰지 않음 | `AppConfig.NAVI_STATUS_REFRESH_MILLIS` |
| F15 | 앱의 `TripStatus` 에는 PAUSED 가 있으나 표준 v0.35 `TRIP_STATUS`(결정)에는 RECORDING · COMPLETED · INTERRUPTED · CANCELED 만 — 기존 어긋남 · 이번에 바꾸지 않음 | `TripStatus.kt` · `standard.md` 6절 |
| F16 | 프로토타입(#43 · `origin/share-prototype/impl`) 선례: 내비 알림을 상시 흐름(시작 · 도착 · 취소)으로 받아 「내비에서 시작」 · 「도착」을 처리 · 목적지 대조는 **이름만** 비교 | `DemoGuidanceViewModel.kt` · `NaviInfoDataSource.kt`(프로토타입) 읽기만 |
| F17 | `DestinationArrivedInfo` 칸 = duration · distance · name · address — **좌표 · POI · 도착 시각 없음**. `WaypointArrivedInfo` = duration · distance · index · name · address(경유지 도착은 따로 오는 알림) | javap(`navi-data-3.0.4`) |
| F18 | 조회로 「안내가 도착으로 끝났는지」를 알 길 없음 — `RouteStateInfo` 칸 = termsAgreement · naviMode(NOT_WORKING · SAFE_GUIDING · ROUTE_GUIDING · SIMULATION_GUIDING) · isRouting · routeOption · 충전 · 교통 · `NavigationStatus` = INACTIVE · INITIALIZING · FREE_DRIVE · ROUTE_DRIVE. 끝난 이유(도착 · 취소) 칸이 없음 · 최근 목적지 조회(`getRecentDestinationInfo`)도 도착 여부 칸 없음(이슈 「최근 목적지 가져오기 안 함」) | javap |
| F19 | 도착 알림에 좌표가 없으므로 좌표는 **안내 시작 때 목적지 조회(`getDestinationInfo` · F6)로 얻어 두고**, 도착 알림의 이름과 맞춰 씀(3.3) | F5 · F6 · F17 |
| F20 | Vehicle SDK 2.3.2 에 시동 상태 값 꼴(`IgnitionState` = UNDEFINED · LOCK · OFF · ACC · ON · START · `PleosPropertyIds.IGNITION_STATE`)은 있으나 **이를 읽는 공개 API(get · register)가 없음** — `Vehicle` 이 내주는 영역은 EvBattery · HVAC · Door · Seat · Window · Display · Odometer · Light · Safety · CarInfo · DrivingMode · Gear · SteeringWheel · Toll · TurnSignal · Wiper · Tire · SideMirror · Brake · Fuel 뿐 | Gradle 캐시 `Vehicle-2.3.2` · `vehicle-data-1.1.11`(javap · 참조 검색) |
| F21 | AAOS 표준 `VehiclePropertyIds.IGNITION_STATE` 는 읽기에 `Car.PERMISSION_POWERTRAIN`(normal) 필요 — 지금 매니페스트에 없음 · 이슈 「새 권한 없음」과 어긋남 | Android `VehiclePropertyIds` 문서 |
| F22 | 앱이 이미 읽는 「주행 판정」 = 기어(Vehicle SDK · `CAR_DRIVE`) P → PARKED · R/N/D → DRIVING + CarUxRestrictions · 하나라도 DRIVING → DRIVING · 둘 다 PARKED → PARKED · 모름 → UNKNOWN. 값은 앱 전역 저장소 하나에서 흐름으로 나옴 | `pleos-vehicle-sdk` 스킬 4절 · `DrivingSignalRepository` · `TripPlaceRecorder.kt` 38줄 |
| F23 | 차량 신호 · 내비 연결은 화면(Activity)이 살아 있는 동안만 — 시동이 꺼져 프로세스가 끝나면 다음에 사용자가 앱을 열 때까지 기어 변화를 받지 못함(앱이 스스로 다시 시작하지 않음 · 부팅 수신 없음) | `MainActivity.kt` · `PleosSdkSession.kt` |
| F24 | 「이어서 쓰기」 칸 · 핀은 기기 안 저장(DataStore · Room)이라 프로세스 종료 · 시동 꺼짐을 넘김. 같은 꼴의 기기 안 저장(설정 키)을 이 기능의 「기록 대기」에 쓸 수 있음 · DB 표가 아니므로 표준 테이블 이름 규칙 밖 | `UnfinishedInputLocalDataSource.kt` · `AppPreferencesKeys` |
| F25 | 「여행 종료」(A-04 「종료」)는 진행 중(RECORDING · PAUSED) 여행을 COMPLETED 로 바꾸는 한 곳 · 종료 시각을 돌려줌 | `RecordingRepository.endTrip` |

### 1.2 모르는 것(실측 필요 · builder 첫 단계) (수정: 도착 뒤 재출발 — M1 · M6 고침 · M7 ~ M11 추가)

| 번호 | 모르는 것 | 확인 방법 | 결과에 따라 |
|---|---|---|---|
| M1 | 상시 리스너가 사용자가 내비에서 직접 시작한 안내의 `onRouteStarted` **와 `onDestinationArrived`** 를 받나 — 앱이 앞 · 나눔 화면 옆 칸 · 뒤 · 얼림 각각에서 | 에뮬레이터: 여행 기록 중 → 내비에서 목적지 검색 · 「안내 시작」 → 모의 주행(SIMULATION_GUIDING) 또는 GPX 로 도착 → 앱 로그(콜백 이름만) | 도착 알림을 못 받으면 N12(희주 님 대안)를 사람에게 되물음 |
| M2 | 직접 시작 때 `onRouteStarted` 가 오는 때(경로 선택 화면 vs 「안내 시작」 뒤) · 그때 목적지 조회가 되나 | 같은 실측 | 안내 시작 전이면 그 순간 목적지 조회가 안 될 수 있음 → 3.3 「목적지 모름」 행 |
| M3 | 재탐색 · 경로 옵션 변경 · 경유지 추가 때 `onRouteStarted` 가 다시 오나 | 에뮬레이터 GPX 이탈 · 옵션 바꾸기 | 와도 같은 목적지면 「진행 중 안내」 기억만 새로 씀(3.3) |
| M4 | 얼림이 길 때 쌓인 알림(도착 · 기어)이 버려지나 · 늦게 오면 순서가 지켜지나 | 내비 앞 5분 이상 → 도착 · P · D → 앱 복귀 | 버려지면 그 도착은 기록 안 함(놓친 도착) · 늦게 오면 도착 시각 = 받은 때(N7) |
| M5 | `poiId` · `poiSubId` · `poiProvider` 에 실제 값이 오나 | 조회 결과 칸 비었나 여부만 로그(값 없음) | 이번엔 저장 · 판정에 쓰지 않음(N5) |
| M6 | 도착 알림의 이름(`DestinationArrivedInfo.name`)이 시작 때 목적지 조회의 이름(`DestinationInfo.name`)과 같은 글자인가 | 같은 실측(이름 같음 여부만 로그 · 값 없음) | 다르면 「진행 중 안내 목적지가 하나뿐이면 그것」으로 맞춤(N8) |
| M7 | 도착 판정 반경 · 도착 알림 뒤 내비 모드(안내 끝 · 자동 종료 여부) | 같은 실측 | 기록에는 영향 없음 · 보고서에만 |
| M8 | 경유지가 있는 안내에서 경유지 도착 때 `onDestinationArrived` 도 오나(`onWayPointArrived` 만 오나) | 내비에서 경유지 1개 추가 뒤 모의 주행 | 경유지 도착이 `onDestinationArrived` 로 오면 목적지 이름 대조(3.3)로 걸러짐 |
| M9 | 기어 P → D 를 Virtual Console 로 바꿀 때 앱이 PARKED → DRIVING 을 받는가(앱 앞 · 얼림) | Virtual Console Motion → Gear | 받지 못하면 「재출발」 판정 불가 → N1 다른 후보 |
| M10 | 에뮬레이터 · 실차에서 시동 끔 → 켬 때 앱 프로세스가 살아남는가(대기 전원 상태) · 끝나는가 | 에뮬레이터 콜드 부트 · 실차는 사람 확인 | 끝나면 다음 앱 열림 때 대기 처리(3.4 「앱 새로 시작」 행) |
| M11 | 도착 알림이 차가 움직이는 중(D)에 오는지 · 정차(P) 뒤에 오는지 | 실측 | P 뒤에 오면 「도착 뒤 PARKED 를 봄」 조건(3.4)은 다음 P 를 기다림 → 시험 T22 확인 |

## 2. 화면 (수정: 도착 뒤 재출발 — 확정이 주행 전환 순간이라 정차 한 줄 안내를 뺌)

새 화면 없음 · 새 문구 없음. 자동 기록은 화면과 무관하게 앱 안에서 일어나고, 결과는 기존 화면의 「장소 수」 · 「이어서 쓰기」 띠 · A-03 시트 · 장소 목록으로 보임. 「기록 대기」(도착 ~ 재출발 사이)는 화면에 보이지 않음(N10).

### 2.1 기록 칸(정차 홈 · 진행 중 여행) (수정: 도착 뒤 재출발)

| 요소 | 자동 기록 뒤 |
|---|---|
| 맨 위 영역 장소 수(「장소 %1$d곳」) | +1 — 「이 장소 기록」과 같음(대기 중엔 늘지 않음) |
| 한 줄 안내 | 없음 — 확정은 주행으로 바뀌는 순간 · 여행 종료 때(3.4)라 정차 기록 칸이 보이는 동안 일어나지 않음 |
| A-03 시트 | 자동으로 열지 않음 |
| 「이어서 쓰기」 띠 | 2.3 규칙대로 남은 경우 기존 띠 「작성 중이던 내용이 있어요」 · 「이어서 쓰기」 그대로 |

### 2.2 주행 화면(A-02) (수정: 도착 뒤 재출발)

| 요소 | 자동 기록 뒤 |
|---|---|
| 장소 수 | +1(화면이 앞이면 바로 · 아니면 다음에 보일 때) |
| 한 줄 안내 · 팝업 · 소리 · 화면 전환 | 없음(완료 조건 「주행 중 화면 알림 · 입력 없음」) |
| 「내비 안내」 카드 · 버튼 4개 | 그대로 |

### 2.3 「이어서 쓰기」 띠 → A-03 장소 기록 · 태그 시트 (수정: 도착 뒤 재출발 — 기록 시각 = 도착 시각)

- 자동 기록 1건마다 「이어서 쓰기」 칸에 그 장소를 남김 — **칸이 비어 있을 때만**(이미 작성 중 내용 · 다른 장소가 있으면 덮어쓰지 않음 · N6). 한 번에 여러 건이 확정되면 가장 이른 도착 1건만.
- 정차 뒤 띠의 「이어서 쓰기」 → 기존 A-03 시트가 그 장소로 열림:

| 요소 | 값 |
|---|---|
| 제목 | 「이 장소를 기록했어요」(기존) |
| 기록 시각 | 「기록 %1$02d:%2$02d」 = **도착 시각**(`recorded_at` · 4.1 · N7) |
| 장소 이름 칸 | 내비 목적지 이름(고칠 수 있음) · 비었으면 주소에서 만든 처음 값 · 둘 다 없으면 빈 칸 |
| 태그 · 메모 · 「건너뛰기 · 나중에」 · 「저장」 | 기존 그대로 |

### 2.4 장소 목록(A-05 로드맵 검토 · A-08 내 로드맵 상세)

- 자동 기록 장소는 「이 장소 기록」 장소와 **같은 모양**(출처 표시 없음) — 순서 = 기록 순(확정 순 · N9) · 이름 · 「미완성」 표시(`draft_yn = Y` 동안) · 「+ 태그 달기」 → A-03.

### 2.5 주행 · 정차 · 모름 (수정: 도착 뒤 재출발)

| 판정 | 도착 → 대기 | 대기 → 확정(재출발) | 화면 |
|---|---|---|---|
| 정차(PARKED) | 함(3.3 조건) | 하지 않음 — 「도착 뒤 PARKED 를 봄」만 표시 | 변화 없음 |
| 주행(DRIVING) | 함 | PARKED 를 본 대기가 있으면 확정 | 장소 수 +1 · 안내 없음 · 띠는 정차 뒤 |
| 모름(UNKNOWN) | 함 | **하지 않음**(재출발로 보지 않음 · N2) | 변화 없음 |

화면 표시는 주행처럼 다루는 「모름」을, 확정 판정에서는 「재출발 아님」으로 다룸 — 잘못 확정(가지 않은 곳)보다 늦은 확정(다음 PARKED → DRIVING · 여행 종료)이 안전.

## 3. 상태 (수정: 도착 뒤 재출발 — 전체 다시 씀)

### 3.1 UiState 칸

| 화면 | 칸 | 변경 |
|---|---|---|
| 기록 칸(`HomeUiState`) | `placeCount` · `message` | 그대로(값이 자동 기록으로도 늘어남 · 새 메시지 종류 없음) |
| 주행 화면(`DrivingUiState`) | `placeCount` | 그대로 · 새 칸 없음 |
| 앱 틀(`AppShellUiState`) | `unfinishedInputAvailable` | 그대로 · 새 칸 없음 |

화면 밖 상태(앱 수명 · 화면이 그려지지 않아도 이어짐):
- **진행 중 직접 안내(메모리)** — 시작 알림 때 목적지 조회로 얻은 목적지(이름 · 주소 · 좌표) 1건 · 「직접 시작」으로 분류된 것만(3.6). 새 시작 알림 · 취소 알림 때 바뀌거나 지워짐. 프로세스가 끝나면 사라짐(그 안내의 도착은 기록 안 함 · 3.5).
- **기록 대기(기기 안 저장)** — 4.2 꼴 · 시동 꺼짐 · 프로세스 종료 · 얼림을 넘김.
- 좌표 · 주소 · 이름은 화면 상태에 넣지 않음.

### 3.2 판정에 쓰는 상태

| 상태 | 값 | 출처 |
|---|---|---|
| 여행 기록 상태 | RECORDING · PAUSED · 진행 중 여행 없음 · COMPLETED · CANCELED · INTERRUPTED · 못 읽음(한도 2초) | 기존 기록 상태 단일 출처 |
| 위치 수집 동의 | 동의함 · 동의 안 함(이력 없음 · 거부 · 철회) · 못 읽음 | 기존 동의 이력 최신 행 |
| 내비 연결 | 연결됨 · 미연결 | 기존 |
| 주행 판정 | PARKED · DRIVING · UNKNOWN | 기존 합성 판정(F22) |
| 이 앱 요청 | 대기 · 요청 중(X) · 시작됨(X) · 실패(X) · TIMEOUT 뒤 기억 중(X) | 기존 길안내 요청 입구 |
| 진행 중 직접 안내 | 없음 · 목적지 D(이름 · 주소 · 좌표) | 3.1 |
| 기록 대기 | 0 ~ 5건(4.2) | 3.1 |
| 현재 여행의 장소 · 핀 | 지우지 않은 장소 좌표 · 핀 장소(앱 장소 목록 이름 · 좌표) | 기기 |

### 3.3 사건별 처리 — 시작 · 도착 · 취소

위에서부터 처음 맞는 행 하나만. 처리는 한 번에 하나씩 차례로.

| 사건 | 조건 | 결과 | 조회 |
|---|---|---|---|
| 시작 알림(`onRouteStarted`) | 여행 ≠ RECORDING · 동의 ≠ 동의함 · 미연결 | 진행 중 직접 안내 = 없음 | 없음 |
| 시작 알림 | 안내 중 아님 · 조회 실패 · 한도 지남 · 좌표 쓸 수 없음(범위 밖 · (0, 0)) | 진행 중 직접 안내 = 없음(목적지 모름 · 그 안내의 도착은 기록 안 함) | 안내 상태(+ 목적지) |
| 시작 알림 | 목적지가 이 앱 요청과 같음(3.6) | 진행 중 직접 안내 = 없음(핀은 기존 규칙) | 함 |
| 시작 알림 | 그 밖 | 진행 중 직접 안내 = 목적지 D(재탐색 · 같은 목적지 재시작이면 같은 D 로 덮어씀) | 함 |
| 취소 알림(`onRouteCancelled`) | — | 진행 중 직접 안내 = 없음(취소한 목적지는 기록 안 함) · 이미 있는 기록 대기 · 장소는 그대로 | 없음 |
| 도착 알림(`onDestinationArrived`) | 진행 중 직접 안내 = 없음 | 기록 안 함 | 없음 |
| 도착 알림 | 여행 ≠ RECORDING · 동의 ≠ 동의함 | 기록 안 함 · 진행 중 직접 안내 = 없음 | 없음 |
| 도착 알림 | 도착 이름 ≠ D 의 이름(앞뒤 공백 제거 비교 · M6 · N8) | 기록 안 함(경유지 · 다른 안내 도착일 수 있음) | 없음 |
| 도착 알림 | 그 밖 | **기록 대기 1건 추가**(D 의 이름 · 주소 · 좌표 · 여행 번호 · 도착 시각 = 알림 받은 때 · 「PARKED 봄」 = 아니오) · 진행 중 직접 안내 = 없음 | 없음 |
| 경유지 도착(`onWayPointArrived`) · `onRouteEnded` · `onDestinationChanged` | — | 아무것도 안 함(N11 · F8) | 없음 |

- 도착 알림 때는 조회하지 않음 — 도착 뒤엔 안내가 끝나 목적지 조회가 비거나 다를 수 있음 · 좌표는 시작 때 얻어 둔 값(F19).
- 이름은 `DestinationArrivedInfo.name` 이 아니라 시작 때 조회한 `DestinationInfo.name` 을 씀(완료 조건 「이름 · 주소 · 좌표는 내비의 목적지 정보에서」).

### 3.4 확정(재출발) · 폐기 (#66)

「재출발」 = **도착 뒤 주행 판정 PARKED 를 한 번 본 다음 처음 DRIVING 으로 바뀜**(기어 P → R/N/D 또는 주행 규제 · F22 · N1 · N2).

| 사건 | 조건 | 결과 |
|---|---|---|
| 주행 판정이 PARKED 가 됨(앱이 받은 때) | 기록 대기 있음 | 「PARKED 거치지 않은」 대기를 「PARKED 봄」으로 표시 |
| 주행 판정이 DRIVING 이 됨 | 「PARKED 봄」 대기 없음 | 그대로 |
| 주행 판정이 DRIVING 이 됨 | 「PARKED 봄」 대기 있음 · 그 대기의 여행이 지금 RECORDING · 동의함 | 도착 순으로 하나씩 **확정 판정**(아래 표) · 처리한 대기는 지움 |
| 주행 판정이 DRIVING 이 됨 | 「PARKED 봄」 대기 있음 · 여행이 PAUSED | 대기 유지(재개 뒤 다음 재출발 때 · N3) |
| 주행 판정이 UNKNOWN | — | 아무것도 안 함 |
| 앱이 새로 시작(프로세스 시작 · 시동 꺼졌다 켜짐 포함) | 기록 대기 있음 | 모든 대기를 「PARKED 봄」으로 표시(시동이 꺼졌다 켜졌으면 차가 섰던 것 · M10 · N2) → 첫 DRIVING 때 확정 |
| 새 시작 알림(다른 목적지 안내 시작) | 「PARKED 거치지 않은」 대기 있음 | 그 대기 폐기(도착 알림은 왔으나 서지 않고 지나침) |
| 도착 뒤 10분(받은 때 기준) 안에 PARKED 를 못 봄 | — | 그 대기 폐기(지나침 · N4) |
| 「여행 종료」(A-04 「종료」) | 그 여행의 대기 있음 · 동의함 | **종료 직전에 확정 판정**(PARKED 여부 무관 · 마지막 목적지 도착 뒤 바로 끝내는 경우 · N3) |
| 여행이 끝남 · 취소 · 지움 · 「내 기록 모두 지우기」 · 다른 여행 시작 | 그 여행의 대기 | 폐기 |
| 위치 동의 철회 | — | 모든 대기 즉시 삭제 |
| 대기가 5건을 넘음 | — | 가장 오래된 도착부터 버림 |

확정 판정(대기 1건):

| 순서 | 조건 | 결과 |
|---|---|---|
| 1 | 현재 여행 장소(수동 · 자동 · 지우지 않은 것 · 좌표 있는 것) 가운데 대기 좌표와 100 m 안이 있음 | 기록 안 함(도착 뒤 수동 「이 장소 기록」 했으면 여기서 걸림 · 3.5) |
| 2 | 앞서 확정한 대기와 100 m 안 | 기록 안 함 |
| 3 | 그 밖 | **장소 1행 기록**(4.1) → 「이어서 쓰기」 칸(2.3) |

### 3.5 놓친 도착 · 중복 (수정: 도착 뒤 재출발 — 옛 「놓친 시작 되찾기」를 바꿈)

- **놓친 도착은 기록 안 함** — 앱이 얼려진 동안 쌓인 도착 알림을 얼림이 풀릴 때 받으면 그때 처리(M4) · 프로세스가 끝나 잃었으면 되찾지 않음. 조회로 「도착으로 끝났는지」를 알 수 없음(F18) · 현재 위치로 짐작하지 않음. 화면 복귀 · 재연결 때 목적지 조회(옛 3.5)는 하지 않음 · 주기 조회 없음.
- **진행 중 직접 안내를 모르는 도착**(시작을 놓침 · 프로세스 다시 시작)은 기록 안 함 — 도착 알림에 좌표가 없어 중복 판단 · 기록을 할 수 없음(F17).
- **중복**: 확정 때 현재 여행의 모든 장소(수동 「이 장소 기록」 포함)와 좌표 100 m(기존 「목적지 같음」 반경 한 곳 재사용). 같은 목적지를 여러 번 도착해도 하나 · 도착 뒤 수동으로 이미 남긴 곳은 자동 기록 안 함. 수동 「이 장소 기록」은 이 판단의 대상이 아님(수동은 막지 않음).

### 3.6 이 앱 요청 안내와 구분

시작 알림 때 목적지가 아래 하나에 맞으면 「이 앱 요청」 → 진행 중 직접 안내로 기억하지 않음 → 그 도착은 기록 안 함(맞음 = 기존 판정 「이름 같음 + 좌표 100 m 안」).

| 대상 | 이유 |
|---|---|
| 요청 입구 상태가 요청 중(X) · 시작됨(X) · TIMEOUT 뒤 기억 중(X) 일 때의 X | 앱 요청의 알림도 같은 리스너로 옴(F3) — 요청 입구가 핀을 남김 |
| 현재 여행의 핀 장소(앱 장소 목록의 이름 · 좌표) | 프로세스가 끝나 요청 입구 기억이 사라진 뒤에도 이중 기록을 막음 |

- 요청 입구가 실패(X · TIMEOUT 아님)이거나 기억 기한이 지난 뒤 사용자가 내비에서 직접 X 를 시작하면: 핀 없음 → 직접 안내로 기억.

### 3.7 전이 표 (수정: 도착 뒤 재출발)

| 지금 | 사건 | 다음 | 비고 |
|---|---|---|---|
| RECORDING · 동의 · 연결됨 | 내비에서 직접 A 로 안내 시작 | 진행 중 직접 안내 = A · 장소 0 | 시작 때 기록 안 함 |
| 진행 중 A | 가는 길에 「이 장소 기록」(밥집 B) | 장소 B(순서 n) | 기존 수동 |
| 진행 중 A | A 도착 알림 | 기록 대기 A(도착 시각) · 장소 그대로 | 3.3 |
| 대기 A(PARKED 아직) | 주차(P) | 대기 A 「PARKED 봄」 | 3.4 |
| 대기 A(PARKED 봄) | 다시 출발(D) | 장소 A(순서 n+1 · 기록 시각 = 도착 시각) · 대기 비움 · 띠(비었으면) | 들른 곳 → 목적지 순서 |
| 대기 A(PARKED 봄) | 그 사이 A 에서 「이 장소 기록」(A 와 100 m 안) → D | 수동 A 1행만 · 자동 0 | 3.4 확정 판정 1 |
| 진행 중 A | 취소 → 다른 곳 C 로 직접 시작 → C 도착 → P → D | 장소 C 만 | A 는 기록 안 함 |
| 진행 중 A | 안내를 끝내지 못하고 시동 끔(도착 알림 없음) | 장소 0 | 도착 못 한 목적지 |
| 대기 A(PARKED 아직) | 서지 않고 지나쳐 다른 목적지 C 안내 시작 | 대기 A 폐기 | 3.4 |
| 대기 A(PARKED 아직) | 10분 동안 PARKED 없음 | 대기 A 폐기 | N4 |
| 대기 A | 시동 끔(프로세스 끝남) → 시동 켬 → 앱 열림 → D | 장소 A | 3.4 「앱 새로 시작」 · 앱을 열 때까지 확정 안 됨(F23) |
| 대기 A(PARKED 봄) | 「여행 종료」 | 장소 A 확정 → 여행 COMPLETED | N3 |
| 대기 A · PAUSED | D | 대기 유지 → 「기록 재개」 뒤 P → D 때 확정 | N3 |
| 대기 A | 위치 동의 철회 | 대기 삭제 · 장소 0 | N9 |
| PAUSED · 여행 없음 · 끝남 | 직접 안내 A 도착 | 기록 안 함 | 3.3 |
| 앱 요청 안내 X | X 도착 · P · D | 핀(기존) · 장소 0 | 3.6 |
| RECORDING · 앱 얼림 | A 도착(알림 쌓임) → P → D → 앱 복귀 | 쌓인 알림을 받은 순서대로 처리 → 장소 A(도착 시각 = 받은 때) | M4 · N7 |

## 4. 데이터 (수정: 도착 뒤 재출발 — 4.1 · 4.2(기록 대기) · 4.3 · 4.4 고침)

### 4.1 앱 모델 — 확정 1건이 채우는 `trip_place_dtl` 행

| 칸 | 값 |
|---|---|
| `trip_no` | 대기의 여행(확정 때 RECORDING · 여행 종료 직전) |
| `place_ord` | 그 여행의 다음 번호(기존 규칙 · 확정 순 · N9) |
| `user_place_nm` | 시작 때 목적지 조회 `name`(앞뒤 공백 제거 · 60자 한도) · 비었으면 주소에서 만든 처음 값 · 둘 다 없으면 null |
| `addr` | 시작 때 목적지 조회 `address` · 비었으면 null |
| `lat` · `lng` | 시작 때 목적지 조회 `latitude` · `longitude`(입구 좌표 `display*` 안 씀) |
| `draft_yn` | Y |
| `memo` | null |
| `recorded_at` | **도착 시각**(도착 알림을 받은 때 · 재출발 시각 아님 · N7) |
| 공통 칸 | 기존 규칙(생성 · 수정 시각 = 확정한 때) |
| POI 번호 · 출처 | 저장하지 않음(N5) |

- 「이어서 쓰기」 칸 · 핀: 기존 꼴 그대로 · 핀은 만들지 않음.

### 4.2 기록 대기(기기 안 저장 · 새 DB 표 없음) — 서버 테이블 변경 없음

| 항목 | 값 |
|---|---|
| 저장 자리 | 기기 안 설정 저장(「이어서 쓰기」와 같은 꼴 · DB 표 · Room 엔티티 아님 · F24 · N10) |
| 한 건 | 여행 번호 · 목적지 이름 · 주소 · 좌표 · 도착 시각 · 「PARKED 봄」 여부 |
| 한도 · 순서 | 최대 5건 · 도착 순 · 넘치면 가장 오래된 것 버림(수치는 바뀔 값 한 곳) |
| 유효 기간 | 확정 · 폐기될 때까지(3.4) — 여행이 끝나면(종료 때 확정 판정 뒤) 남지 않음 · PARKED 를 못 본 대기는 도착 뒤 10분 |
| 지움 | 위치 동의 철회 · 「내 기록 모두 지우기」 · 여행 삭제 · 앱 데이터 지우기 · 앱 삭제 |
| 백업 | 안 됨(`allowBackup="false"` 유지) |
| 서버 | 없음 |

비교한 안(N10): (가) `trip_place_dtl` 에 도착 때 바로 draft 행을 넣고 재출발 때 확정 — 「확정 전」을 가릴 칸이 없음(F12 · 표준 단어 · Room 이전 필요) · 장소 수가 도착 때 먼저 늘어 사람 결정(재출발 때 기록)과 어긋남 · 지나친 곳을 지우는 삭제가 생김 → 버림. (나) 별도 기기 안 대기 → 추천.

서버 · Alembic · 시드 변경 없음. 기기 `trip_place_dtl` 은 기존처럼 기기 안 임시 원본 — 이 기능이 서버 전송을 새로 만들지 않음.

### 4.3 최소 수집 · 개인정보 점검 (수정: 도착 뒤 재출발 — 기록 대기 · 진행 중 직접 안내 행)

| 항목 | 결과 |
|---|---|
| 무엇을 새로 읽나 | 내비 안내 상태 · 목적지(이름 · 주소 · 좌표 · POI 칸) — 기록 중 + 위치 수집 동의일 때 · 시작 알림 때만(주기 없음) · 도착 · 취소 알림(이름 · 주소) · 기존 주행 판정 |
| 메모리 | 진행 중 직접 안내 1건(이름 · 주소 · 좌표) — 취소 · 새 시작 · 도착 · 동의 철회 때 지움 |
| 기기 저장 | 기록 대기(4.2 · 최대 5건) · 확정된 `trip_place_dtl` 1행 — 동의 문구 「기록한 장소의 위치 좌표 · 주소 · 기기 안에만 저장」 범위 안 · POI 번호는 저장 안 함 |
| 서버 전송 | 없음 · 핀 전송에도 들어가지 않음 |
| 로그(앱) | 콜백 이름 · 처리 결과 종류(대기 추가 · 확정 · 폐기 · 건너뜀 이유 코드) · 대기 건수 · 오류 코드 이름 · 예외 종류만. **좌표 · 주소 · 장소 이름 · POI 번호 · 도착 시각 · trip_place_no 를 남기지 않음** |
| 화면 상태 | 좌표 · 주소 없음 · 대기는 화면에 없음 |
| 동의 전 · 철회 뒤 | 목적지 조회 0 · 대기 0 · 기록 0 · 철회 순간 대기 · 진행 중 직접 안내 삭제 · 이미 확정된 행의 좌표 · 주소는 기존 철회가 지움 · 이름은 N13 |
| 보관 · 삭제 | 확정 행 = 기존 「이 장소 기록」과 같음 · 대기 = 4.2 유효 기간 |
| 기어 · 속도 · VIN · Pleos 토큰 · 시동 | 새로 읽는 것 없음(기어는 기존 주행 판정을 보기만 · 시동 신호 안 읽음) · 저장 · 전송 없음 |
| LLM | 없음 |

### 4.4 쓴 표준 용어 · 사전에 없는 단어 (수정: 도착 뒤 재출발 — 대기 저장)

| 구분 | 이름 | 표준 v0.35 |
|---|---|---|
| 테이블 | `trip_place_dtl` | 사전에 있음 |
| 컬럼 | `trip_place_no` · `trip_no` · `place_ord` · `draft_yn` · `lat` · `lng` · `memo` · `recorded_at` · 공통 칸 | 사전에 있음 |
| 컬럼(기존 어긋남) | `user_place_nm` · `addr` | 사전에 없음 — #51 에서 이미 쓴 이름 · 이번에 새로 만들지 않음(N14) |
| 코드 | `TRIP_STATUS.RECORDING` · `CONSENT_TYPE.LOCATION_COLLECT` | 결정(PAUSED 는 표준에 없음 · F15) |
| 기록 대기 | 기기 설정 저장(표 · 칸 아님) — 칸 이름을 붙이면 사전 단어(`trip_no` · `lat` · `lng` · `addr` 꼴)를 따르되 「도착」 · 「대기」 단어는 사전에 없음 | 표 · 컬럼이 아니라 등록 불필요로 봄 · 사람 확인(N10) |
| 쓰지 않음 | POI 번호 · 출처 코드(`DATA_SRC` 가안) · `place_no` · 확정 여부 칸 | N5 · N10 |

### 4.5 권한

새 권한 없음 — 상시 리스너(권한 표시 없음) · `getRouteStateInfo` · `getDestinationInfo`(`NAVI_ROUTE` · 선언됨). `check_manifest.py` 허용 목록 변경 없음.

## 5. API 계약 변경

없음. `docs/api/openapi.json` 변경 없음 · 앱 Retrofit 인터페이스 변경 없음.

```yaml
# 변경 없음 — 이 기능은 서버 경로를 부르지 않음
paths: {}
```

## 6. 시험 케이스 (수정: 도착 뒤 재출발 — 전체 다시 씀)

단위 = fake 내비(알림 · 조회 결과) · fake 주행 판정 · fake 기록 상태 · fake 동의 · fake 시계 · 기기 저장 fake. E = 에뮬레이터(Pleos Connect · 기본 내비 앱 · Virtual Console 기어).

| 번호 | 조건 | 행동 | 결과 | 비고 |
|---|---|---|---|---|
| T1 | RECORDING · 동의 · 연결됨 · 장소 0 | 직접 A 시작 알림(목적지 조회 A) | 장소 0 · 대기 0 · 진행 중 직접 안내 A | 단위 · 시작 때 기록 안 함 |
| T2 | T1 | A 도착 알림 | 대기 1(A 이름 · 주소 · 좌표 · 도착 시각 = 지금 · PARKED 아님) · 장소 0 | 단위 |
| T3 | T2 | PARKED → DRIVING | 장소 1행(A · draft Y · 순서 1 · `recorded_at` = 도착 시각) · 대기 0 · 띠 칸 = 그 행 · 안내 없음 | 단위 · ViewModel(장소 수 +1) |
| T4 | T2 | DRIVING 만 계속(PARKED 없음) | 장소 0 · 대기 유지 | 단위 |
| T5 | T2 | 10분 동안 PARKED 없음 | 대기 폐기 · 장소 0 | 단위(가상 시간) · N4 |
| T6 | T2 | PARKED → UNKNOWN | 장소 0 · 대기 유지(PARKED 봄) | 단위 · N2 |
| T7 | T1 · 진행 중 A | 「이 장소 기록」(B · A 와 1 km) → A 도착 → P → D | 장소 B(1) · A(2) | 단위 · 리뷰 순서 문제 |
| T8 | T2 → P | A 에서 「이 장소 기록」(A 와 50 m) → D | 수동 1행만 · 자동 0 · 대기 0 | 단위 · 3.4 판정 1 |
| T9 | 진행 중 A | 취소 알림 → 직접 C 시작 → C 도착 → P → D | 장소 C 만 | 단위 |
| T10 | 진행 중 A | 도착 알림 없이 시동 끔(프로세스 끝) → 다시 열림 | 장소 0 · 대기 0 | 단위 · 도착 못 함 |
| T11 | 대기 A(PARKED 아직) | 직접 C 시작 알림 | 대기 A 폐기 | 단위 · 지나침 |
| T12 | 대기 A(PARKED 아직) · 저장됨 | 프로세스 새로 시작 → DRIVING | 장소 A | 단위 · 3.4 「앱 새로 시작」 |
| T13 | 대기 A(PARKED 봄) | 「여행 종료」 | 장소 A 확정 → COMPLETED · A-05 에 A | 단위 · N3 |
| T14 | 대기 A(PARKED 봄) · PAUSED | DRIVING | 대기 유지 · 장소 0 → 재개 · P → D → 장소 A | 단위 · N3 |
| T15 | 대기 A | 여행 취소 · 여행 삭제 · 「내 기록 모두 지우기」 | 대기 0 | 단위 |
| T16 | 대기 A | 위치 동의 철회 | 대기 0 · 진행 중 직접 안내 없음 · 이후 D 에도 장소 0 | 단위 |
| T17 | PAUSED · 여행 없음 · COMPLETED | 직접 안내 도착 | 대기 0 | 단위 |
| T18 | RECORDING · 동의 없음 | 직접 시작 · 도착 · P · D | 목적지 조회 0 · 대기 0 · 장소 0 | 단위 |
| T19 | 진행 중 직접 안내 없음(시작 놓침) | 도착 알림 | 대기 0 | 단위 · 3.5 |
| T20 | 진행 중 A | 도착 알림 이름이 A 와 다름(경유지 등) | 대기 0 | 단위 · M6 · M8 · N8 |
| T21 | 진행 중 A | 경유지 도착 · `onRouteEnded` · `onDestinationChanged` | 아무것도 안 함 | 단위 |
| T22 | 도착 알림이 PARKED 뒤에 옴 | 도착 → D | 장소 0 · 대기 유지(PARKED 아직) → 다음 P → D 때 장소 A | 단위 · M11 |
| T23 | 시작 때 목적지 조회 실패 · 안내 중 아님 · 좌표 (0, 0) | 도착 · P · D | 장소 0 | 단위 |
| T24 | A 장소 있음 · 같은 A 다시 직접 시작 · 도착 · P · D | — | 장소 그대로(1행) | 단위 · 중복 |
| T25 | 대기 A · A(80 m 떨어진 이름 다른 곳) 둘 다 PARKED 봄 | D | 장소 1행(먼저 도착한 것) | 단위 · 3.4 판정 2 |
| T26 | 대기 5건 | 6번째 도착 | 가장 오래된 대기 버림 · 5건 | 단위 |
| T27 | 앱 요청 요청 중(X) | 시작 알림 X → X 도착 → P → D | 핀 1건(기존) · 장소 0 | 단위 · 3.6 |
| T28 | 현재 여행 핀 X · 프로세스 새로 시작 | 시작 알림 X(재탐색) → 도착 · P · D | 장소 0 | 단위 |
| T29 | 앱 요청 시작됨(X) | 사용자가 직접 B 시작 → B 도착 → P → D | 장소 B | 단위 |
| T30 | 이름 · 주소 빈 문자열 | 확정 | 장소 1행 · 이름 · 주소 null · 좌표 있음 | 단위 |
| T31 | 「이어서 쓰기」 칸에 작성 중 내용 있음 | 확정 | 장소 1행 · 칸 그대로 | 단위 · N6 |
| T32 | 확정 A · 칸 비었음 → 정차 | 「이어서 쓰기」 | A-03 · 이름 = A · 기록 시각 = 도착 시각 → 「저장」 → draft N | ViewModel |
| T33 | 얼림 중 A 도착 · P · D 알림이 쌓였다 복귀 때 차례로 옴 | — | 장소 A 1행 · `recorded_at` = 도착 알림 받은 때 | 단위 · M4 · N7 |
| T34 | 전 과정 | 로그 수집 | 좌표 · 주소 · 이름 · POI · 도착 시각 · trip_place_no 없음 | 단위(로그 대역) 또는 리뷰 |
| T35 | 전 과정 | 화면 앞 유지 | 주기 조회 0 · 내비 조회는 시작 알림 때만 | 단위(조회 횟수) |
| T36 | E · 기록 중(동의) · 내비에서 직접 목적지 검색 · 「안내 시작」 | 모의 주행으로 도착 → Virtual Console P → D | 장소 수 +1 · 로그에 `onRouteStarted` · `onDestinationArrived` | E · M1 · M9 |
| T37 | E · T36 중 | 도착 전 경로 취소 | 장소 수 그대로 | E |
| T38 | E · 내비가 앞(앱 얼림) | 직접 시작 → 도착 → P → D → 앱으로 | 장소 1행 | E · M4 |
| T39 | E · 경유지 1개 추가한 직접 안내 | 경유지 도착 · 목적지 도착 → P → D | 목적지 1행만 | E · M8 |
| T40 | E · 대기 A(P) | 에뮬레이터 콜드 부트 → 앱 열기 → D | 장소 A | E · M10 |
| T41 | E · 추천 「안내 시작」(B-05)으로 X | 도착 → P → D | 핀 1건 · 장소 수 그대로 | E · 3.6 |

## 7. 미확정 (수정: 도착 뒤 재출발 — 다시 씀 · 옛 N1 · N2 · N7 · N8 · N15 · N16 은 사람 결정 · 새 흐름으로 없어짐)

| 번호 | 질문 | 선택지 | 추천 |
|---|---|---|---|
| N1 | 「재출발」 신호 | (a) 이미 읽는 주행 판정: 도착 뒤 PARKED 를 거쳐 첫 DRIVING (b) 시동 상태(IGNITION_STATE) — Vehicle SDK 공개 API 없음(F20) · AAOS 직접은 `CAR_POWERTRAIN` 새 권한(F21) (c) 앱 프로세스 재시작 · 화면 복귀 — 사용자가 앱을 안 열면 안 옴 · 시동과 무관 (d) 내비 재안내 시작 — 목적지를 안 정하고 출발하면 안 옴 | (a) — 새 권한 없음 · 이미 시험된 신호 · 시동을 끄지 않는 짧은 정차도 포함. (c) 는 (a) 의 보조로만(3.4 「앱 새로 시작」) · (b) 는 이슈 「새 권한 없음」과 어긋남 · 사람이 권한 추가를 허락하면 다시 봄 |
| N2 | 주행 판정 「모름」 · 앱 새로 시작의 다룸 | (a) 모름은 재출발 아님 · 앱 새로 시작은 「PARKED 봄」으로 봄 (b) 모름도 재출발 (c) 앱 새로 시작도 PARKED 를 다시 기다림 | (a) — 기어 신호가 없을 때 가지 않은 곳이 확정되는 것을 막음 · 시동이 꺼졌다 켜졌으면 차가 섰던 것. 대가: 주행 중 프로세스가 죽었다 살아나면 지나친 대기가 확정될 수 있음(10분 기한이 대부분 막음) |
| N3 | 확정 때 여행이 RECORDING 이 아니면 | (a) PAUSED = 유지(재개 뒤 재출발 때) · 여행 종료 = 종료 직전 확정 · 취소 · 삭제 · 여행 없음 = 폐기 (b) RECORDING 아니면 모두 폐기 (c) PAUSED 도 확정 | (a) — 마지막 목적지에 도착해 바로 「여행 종료」하는 흔한 경우(S-1 「시장 도착」)에 재출발이 없어 목적지가 빠짐 · 사람 결정 ②(재출발 때)의 예외라 확인 필요. 도착 순간은 RECORDING 이어야 함(3.3) |
| N4 | 「지나침」 처리 — 도착 알림 뒤 서지 않음 | (a) 도착 뒤 10분 안에 PARKED 없음 · 또는 새 안내 시작이면 폐기 (b) 기한 없음 (c) PARKED 때 현재 위치를 조회해 목적지 반경 안인지 봄 | (a) — 조회 · 위치 읽기 없이 됨 · (b) 는 한참 뒤 다른 곳에서 선 것을 목적지 방문으로 확정 · (c) 는 조회 추가. 10분은 바뀔 값 한 곳 · 사람 확인 |
| N5 | POI 번호 · 출처(내비) 저장 | (a) 저장 안 함 (b) 표준 등록 뒤 칸 추가 | (a) — 이슈 「표준 단어 변경 없음」 · 판정은 좌표로 충분 |
| N6 | 「이어서 쓰기」 칸이 차 있을 때 | (a) 덮어쓰지 않음 (b) 덮어씀 | (a) — 작성 중 글을 몰래 지우면 안 됨 · 자동 기록 장소는 A-05 에서 「미완성」으로 채움 |
| N7 | `recorded_at` | (a) 도착 시각 = 도착 알림을 받은 때 (b) 확정(재출발) 시각 | (a) — 사람 추천안 ⑤(방문 시각). 대가: 앱이 얼려져 알림이 늦게 오면 받은 때라 실제보다 늦음(알림에 시각 칸 없음 · F17) · A-03 「기록 hh:mm」 이 도착 시각으로 보임 |
| N8 | 도착 알림과 진행 중 직접 안내를 맞추는 법 | (a) 도착 이름 = 시작 때 목적지 이름(공백 제거) (b) 진행 중 직접 안내가 있으면 이름 무관 (c) 도착 때 현재 위치 조회로 좌표 대조 | (a) 로 시작 · M6 실측에서 이름이 다르게 오면 (b) 로 — (c) 는 조회 추가 |
| N9 | 장소 순서(`place_ord`) | (a) 확정 순 다음 번호(기존 규칙) (b) 도착 시각 순으로 끼워 넣음(뒤 행 번호 +1) | (a) — 사람 결정 ③(기록 순) · 단순. 대가: 도착 뒤 머무는 동안 수동 기록한 근처 다른 곳(100 m 밖)이 목적지보다 앞 순서가 됨 · 사람 확인 |
| N10 | 기록 대기 저장 꼴 | (가) `trip_place_dtl` draft 행 미리 넣고 확정 (나) 별도 기기 안 설정 저장(표 아님) (다) 새 Room 표(이름 표준 등록 필요) | (나) — 4.2 비교 · 표 · 칸 · 표준 단어 변경 없음 · 이슈 「서버 · DB · 표준 단어 변경 없음」. 표가 아니라도 칸 이름에 「도착」 · 「대기」 단어를 쓰는 것을 정의서 담당이 문제 삼는지 사람 확인 |
| N11 | 경유지 도착 | (a) 기록 안 함(목적지만 · 이슈 범위) (b) 경유지도 대기 | (a) — 이슈는 「목적지」 · 들른 곳은 수동 「이 장소 기록」(사람 결정 ③) |
| N12 | M1 실측에서 **도착 알림이 오지 않으면** | (a) 희주 님 대안 — 시작 때 기록 + 그 뒤 수동 기록이 생기면 자동 행을 맨 뒤 순서로 미룸 + 수동 기록도 같은 여행 100 m 안이면 새로 만들지 않음(또는 알림) + 취소 때 손대지 않은 자동 행(draft Y) 지움 (b) 이번 이슈 보류 | 실측 결과로 사람이 정함 — (a) 는 수동 기록 · 취소 규칙을 바꿔 1판보다 넓어짐 · 그 경우 스펙 3판 |
| N13 | 위치 동의 철회 때 자동 기록 장소의 **이름** | (a) 남김(기존 철회 = 좌표 · 주소 · 주소에서 만든 이름) (b) 지움(자동 행을 가릴 칸 필요) (c) 이름을 저장하지 않음 | (a) 추천 · 사람 확인 — 표준 · 칸 변경 없이 됨. 「경복궁」 같은 이름도 위치를 드러내 심사 「개인정보」에서 물을 수 있음 · (c) 는 완료 조건과 어긋남. 대기 · 진행 중 직접 안내는 철회 순간 전부 지움(결정 아님 · 이 스펙 규칙) |
| N14 | 표준 사전에 없는 `user_place_nm` · `addr`(기존 어긋남) | (a) 이번엔 그대로 (b) 등록 요청 | (a) + 정의서 담당에게 (b) 를 따로 알림 |
| N15 | 진행 중 직접 안내(시작 때 얻은 목적지)를 프로세스 종료를 넘겨 기억 | (a) 메모리만 — 끝나면 그 안내의 도착은 기록 안 함 (b) 기기 저장 | (a) — 「놓친 것은 기록 안 함」과 맞음 · 저장 좌표를 늘리지 않음. 대가: 주행 중 프로세스가 죽으면 그 목적지는 빠짐 · 수동 기록으로 보완 |

## 8. 완료 조건 (수정: 도착 뒤 재출발)

### app

- [ ] (첫 단계) M1 · M2 · M4 · M6 · M8 · M9 · M10 · M11 에뮬레이터 실측 결과를 보고서에 적음 — 도착 알림이 안 오면(M1) 구현 전에 메인에 알리고 N12 를 사람에게 물음
- [ ] 내비 연결이 살아 있는 동안 시작 · 도착 · 취소 알림을 받는 상시 리스너 1개 · 연결이 끝나면 함께 뗌(SDK 타입은 `data/navi` 밖으로 안 나감) — T1 · T2 · T9
- [ ] 안내 시작 때 기록하지 않음 · 시작 때 목적지 조회로 진행 중 직접 안내만 기억(RECORDING · 동의 · 연결됨 · 안내 중일 때만 · 동의 없으면 조회 0) — T1 · T18 · T23
- [ ] 직접 안내의 도착 → 기기 안 기록 대기(4.2) → 도착 뒤 PARKED 를 거친 첫 DRIVING 때 확정 · 모름은 확정 안 함 · 앱 새로 시작은 PARKED 봄 — T2 ~ T6 · T12 · T22
- [ ] 취소 · 도착 못 함 · 놓친 도착 · 시작을 모르는 도착 · 지나침(10분 · 새 안내)은 기록 안 함 — T5 · T9 ~ T11 · T19 · T20
- [ ] 확정 행 = 4.1(시작 때 목적지 이름 · 주소 · 좌표 · draft Y · `recorded_at` = 도착 시각 · 순서 = 다음 번호) — T3 · T7 · T30
- [ ] 중복: 현재 여행 장소(수동 포함) · 앞선 확정과 100 m 안이면 기록 안 함 · 수동 기록은 막지 않음 — T8 · T24 · T25
- [ ] RECORDING 아님: 도착 때 대기 0 · 확정 때 PAUSED 유지 · 여행 종료 직전 확정 · 취소 · 삭제 폐기(N3 결정대로) — T13 ~ T15 · T17
- [ ] 위치 동의 철회 · 「내 기록 모두 지우기」 때 대기 · 진행 중 직접 안내 즉시 삭제 · 대기 최대 5건 — T15 · T16 · T26
- [ ] 이 앱 요청 안내와 이중 기록 없음 · 핀 규칙 그대로 — T27 ~ T29 · T41
- [ ] 화면 알림 · 입력 없음 · 「이어서 쓰기」 칸은 비었을 때만 · 새 문구 없음 — T3 · T31 · T32
- [ ] 로그에 좌표 · 주소 · 장소 이름 · POI · 도착 시각 · trip_place_no 없음 · 주기 조회 없음 — T34 · T35
- [ ] 새 권한 없음(`check_manifest.py` 녹색 · 시동 신호 안 읽음) · 서버 · `openapi.json` 변경 없음(`check_api_contract.py` 녹색)
- [ ] 센서 녹색: `./gradlew lint testDebugUnitTest assembleRelease` · Konsist · 매니페스트 검사 · 비밀 리터럴 · 저장소 위생 · 데이터 표준 대조(새 표 · 칸 없음)
- [ ] 에뮬레이터에서 내비 앱으로 목적지를 직접 입력 · 도착 · 기어 P → D 로 확인 — T36 ~ T41(스크린샷 · 콜백 이름만 담은 로그)
- [ ] 미확정 N1 ~ N4 · N7 · N9 · N10 · N13 은 사람 답 또는 추천 기준으로 구현했음을 보고서에 표시

### server

- [ ] 변경 없음(서버 센서 영향 없음)

읽은 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations(열림 — `onRouteStarted` 「경로 탐색을 시작하면」 · `onDestinationChanged` 「목적지가 설정되거나 제거되면」 · 직접 시작한 경로의 알림 여부 · 도착 알림 · `DestinationInfo` 칸은 적혀 있지 않음) · https://developer.android.com/reference/android/car/VehiclePropertyIds(열림 — `IGNITION_STATE` 읽기 = normal 권한 `Car.PERMISSION_POWERTRAIN`) · Gradle 캐시 `ai.pleos.playground:NaviHelper:3.0.5`(메서드 · `@RequiresPermission` · 리스너 메서드) · `navi-data:3.0.4`(`DestinationInfo` · `RouteStartInfo` · `DestinationArrivedInfo` · `WaypointArrivedInfo` · `RouteStateInfo` · `NaviMode` · `NavigationStatus` · `RecentDestinationInfo` 칸) · `navi-aidl:3.0.1`(`INaviService.registerCallback`) · `Vehicle:2.3.2` · `vehicle-data:1.1.11`(`Vehicle` 영역 목록 · `IgnitionState` · `VehicleIgnitionStateValue` · `PleosPropertyIds.IGNITION_STATE` · `PleosPermission`) — javap 로 읽음 · `pleos-vehicle-sdk` 스킬(주행 판정 · 권한 · Virtual Console) · https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline(스킬 요약으로 · 직접 열지 않음) · https://developer.android.com/topic/architecture/recommendations(스킬 요약으로 · 직접 열지 않음) · 저장소: `app/.../data/navi/*` · `domain/GuideRequestTracker.kt` · `domain/GuidePorts.kt` · `domain/RouteMatchRules.kt` · `domain/AppConfig.kt` · `data/pin/PinRepository.kt` · `data/trip/TripPlaceRecorder.kt` · `data/trip/TripRepository.kt` · `data/local/TravelTables.kt` · `data/unfinishedinput/*` · `data/recording/RecordingRepository.kt` · `data/PleosSdkSession.kt` · `MainActivity.kt` · `domain/model/TripStatus.kt` · `ui/home/HomeUiState.kt` · `ui/driving/DrivingUiState.kt` · `ui/components/PlaceTagSheet.kt` · `res/values/strings.xml` · `AndroidManifest.xml` · `_workspace/4/02_builder_app_report.md`(2026-11-06 실측) · `_workspace/66/pr67_reviews.md`(희주 님 리뷰) · `origin/share-prototype/impl`(읽기만) · `docs/data/standard.md` v0.35 · `docs/data/README.md` · `docs/spec/README.md`
