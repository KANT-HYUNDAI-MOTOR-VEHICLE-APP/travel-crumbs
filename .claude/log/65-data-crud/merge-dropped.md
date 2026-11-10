# PR #65 `data-crud/main` → develop 병합 — develop 우선으로 접은 내용

- PR: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/pull/65 (이슈 번호 없음)
- 병합 브랜치: `data-crud/sync` (`data-crud/main` 에서 따서 `origin/develop` 머지 · team-workflow 5절)
- 되살릴 원본: `data-crud/main` = `7a96254` · 공통 조상 `0cc9873`
- 날짜: 2026-11-10 · 결정: 사람(giboJK)

## 왜 이렇게 했나

develop 이 공통 조상 뒤로 100커밋 앞서며 앱을 v6 로 다시 지었고(기기 안 Room 저장소 · 추천 · 별점 · 레일 4칸 · 디자인 시스템 #34),
`data-crud/main` 의 앱 13커밋은 그 이전 판 위에 서버 연동을 붙인 것이라 두 판이 같은 화면을 서로 다르게 갖고 있었음.
**사람 결정: 충돌 나는 모든 파일에서 develop 에 무조건 우선권.** 방향이 옳다고 보지는 않으나 병합을 먼저 끝내기로 함 —
그래서 접힌 내용을 여기 남기고 추후 할 일로 가져감.

## 들어간 것(온전히 살아남음)

- `server/` 전부 — FastAPI 서버 63파일 · 테이블 14개 · 시나리오 1 API(로그인 → 여행 종료) · Alembic 리비전 · 시드. develop 에는 `server/` 가 아예 없었으므로 충돌 없음.
- `docs/data/` — 데이터표준정의서 v0.4 · 결정 로그 · 인계서 · `docs/api/openapi.json`.

## 접은 것 — 추후 할 일

### 1. 앱 ↔ 서버 연동이 통째로 빠짐 (가장 큼)

`data/remote/AppApi.kt` 를 develop 판으로 받아, 서버 엔드포인트 13개를 부르던 Retrofit 선언이 사라짐.
지금 앱의 `AppApi` 에는 `navigation-starts` 둘만 있음.

접힌 엔드포인트: `GET me` · `GET consents/latest` · `POST consents` · `GET codes/{cd_grp_cd}` ·
`GET trips/current` · `POST trips` · `POST trips/{uuid}/route-points` · `POST trips/{uuid}/places` ·
`GET trips/{uuid}/places` · `PUT trips/{uuid}/places/{trip_place_no}` · `GET tags` · `GET place-types` · 여행 상세 · 목록 · 일시정지.

함께 접힌 것: 서버판 `TripRepository`(`DefaultTripRepository` — `currentTrip` · `startTrip` · `addRoutePoints` ·
`recordPlace` · `endTrip` · `detail` · `myTrips` · `pause` · `resume` · `rename` · `route`) ·
`di/TripModule` · `UserModule` · `NetworkModule`(Retrofit · OkHttp · Json 설정) 의 서버 쪽 바인딩.

**할 일**: develop 의 `TripRepository` 는 지금 기기 안 Room 구현이고, 그 KDoc 이 스스로
「서버 API 가 생기면 이 인터페이스의 구현만 바꿈(di/TripModule)」이라고 적어 둠.
`data-crud/main` 의 `AppApi` · `DefaultTripRepository` 를 되살려 develop 의 `TripRepository` 인터페이스에 맞춰
서버 구현을 하나 더 만들고 `TripModule` 에서 갈아 끼우는 것이 다음 작업.

### 2. 서버 연동용 데이터 · 도메인 조각이 고아로 남거나 지워짐

`data/remote/ConsentDtos.kt` · `MeResponse.kt` · `TripDtos.kt` · `data/code/CodeRepository.kt` ·
`data/consent/ConsentRepository.kt` · `data/place/PlaceRepository.kt` · `data/user/UserRepository.kt` ·
`domain/model/` 의 `TripDetail` · `TripRoute` · `NaviPlace` · `NaviLocation` 등.
develop 의 `domain/model/Trip.kt` 가 같은 이름(`UserProfile` · `ConsentType` · `TripPlace`)을 이미 갖고 있어 겹침.

**할 일**: 1번을 되살릴 때 이름 겹침을 먼저 정리 — 서버 DTO 는 `data/remote` 에 두고 도메인 모델은 develop 의 `Trip.kt` 한 곳으로 모으기.

### 3. 화면 · 부품 두 벌 중 data-crud 판을 버림

| 버린 것(data-crud) | 남은 것(develop) |
|---|---|
| `ui/components/TcButton.kt` | `AppButtons.kt` |
| `ui/components/TcCard.kt` | `AppCard.kt` |
| `ui/components/TcChip.kt` | `AppCard.kt` · `PlaceTagSheet.kt` |
| `ui/components/CrumbRoute.kt` | `RoadmapParts.kt` |
| `ui/placeinput/` 3파일 | develop 의 장소 기록 흐름(#51 U4) |
| `ui/tripsetup/` 3파일 | develop 의 여행 시작(#51 U3) |
| `ui/places/PlacesScreen.kt` · `ui/home/components/LocationConsentDialog.kt` | develop 의 홈 · 설정 화면(#51 U2) |
| `ui/home/components/HomeFeatureSections.kt` | develop 이 지운 파일(#63 홈 정리) |

색 토큰 이름도 `TcColors` → `TravelCrumbsColors` 로 통일함(열린 PR #43 이 `app/src/debug/` 에 `TcColors` 를 따로 갖고 있어,
main 소스셋에 같은 이름을 두면 debug 변종에서 재선언으로 빌드가 깨지기 때문).
`TcColors` 에만 있던 `BrandLavender` · `BrandBlueGray` · `BorderFocus` · `RouteOatBeige` · `Success` · `Warning` · `Info` 는 버림.

### 4. 함께 버려진 시험

`TripPauseSyncTest` · `TripRecordPlaceUseCaseTest` · `PlaceCandidatesTest` · `RouteRulesTest` · `TripNameTest` ·
`PlaceInputViewModelTest` · `TripSetupViewModelTest` · `TripDetailViewModelTest`.
1번을 되살릴 때 같이 되살릴 것.

## 되살리는 법

```
git show data-crud/main:<파일경로>            # 파일 하나 꺼내 보기
git diff 0cc9873 data-crud/main -- app/      # 접힌 앱 변경 전체
```

## 실제로 한 일 (2026-11-10)

사람 결정 「무조건 develop 우선」을 그대로 적용한 결과, **`app/` 전체를 `origin/develop` 과 바이트 단위로 같게 맞춤**.
충돌 35개를 하나씩 develop 판으로 고른 뒤에도 data-crud 에만 있던 앱 파일 54개가 고아로 남아
`compileDebugKotlin` 에서 오류 318개(지워진 `TcColors` 참조 · `domain/model` 이름 재선언 등)가 났기 때문.

- 충돌 35개 → develop 판 34개 · develop 이 지운 파일 1개(`ui/home/components/HomeFeatureSections.kt`) 삭제
- data-crud 에만 있던 앱 파일 54개 삭제
- `MainActivity.kt` 의 data-crud 추가분 7줄(RouteRecorder 주입) 되돌림
- 결과: `./gradlew compileDebugKotlin compileDebugUnitTestKotlin` 녹색

이 PR 이 develop 에 실제로 보태는 것은 **`server/` 68파일과 `docs/data/` 문서뿐**이고, 앱은 develop 그대로임.

### data-crud 에만 있고 develop 에는 없던 앱 파일(= 이번에 남지 않은 것) 65개

- `M:data/code/CodeRepository.kt`
- `M:data/consent/ConsentRepository.kt`
- `M:data/draft/DraftLocalDataSource.kt`
- `M:data/draft/DraftRepository.kt`
- `M:data/place/PlaceRepository.kt`
- `M:data/remote/ConsentDtos.kt`
- `M:data/remote/MeResponse.kt`
- `M:data/remote/TripDtos.kt`
- `M:data/trip/PassThroughTripNameSuggester.kt`
- `M:data/trip/RouteFlushingEndTripUseCase.kt`
- `M:data/trip/RouteRecorder.kt`
- `M:data/trip/TripPauseSync.kt`
- `M:data/trip/TripRecordPlaceUseCase.kt`
- `M:data/user/UserRepository.kt`
- `M:di/ConsentModule.kt`
- `M:domain/EndTripUseCase.kt`
- `M:domain/PlaceCandidates.kt`
- `M:domain/RouteRules.kt`
- `M:domain/TripName.kt`
- `M:domain/TripNameSuggester.kt`
- `M:domain/model/ConsentType.kt`
- `M:domain/model/Draft.kt`
- `M:domain/model/NaviLocation.kt`
- `M:domain/model/NaviPlace.kt`
- `M:domain/model/RecordingSession.kt`
- `M:domain/model/RecordingStatus.kt`
- `M:domain/model/TripDetail.kt`
- `M:domain/model/TripPlace.kt`
- `M:domain/model/TripRoute.kt`
- `M:domain/model/UserProfile.kt`
- `M:ui/components/CrumbRoute.kt`
- `M:ui/components/RecordingTexts.kt`
- `M:ui/components/TcButton.kt`
- `M:ui/components/TcCard.kt`
- `M:ui/components/TcChip.kt`
- `M:ui/home/components/HomeFeatureSections.kt`
- `M:ui/home/components/LocationConsentDialog.kt`
- `M:ui/placeinput/PlaceInputScreen.kt`
- `M:ui/placeinput/PlaceInputUiState.kt`
- `M:ui/placeinput/PlaceInputViewModel.kt`
- `M:ui/places/PlacesScreen.kt`
- `M:ui/shell/components/DraftBanner.kt`
- `M:ui/tripsetup/TripSetupScreen.kt`
- `M:ui/tripsetup/TripSetupUiState.kt`
- `M:ui/tripsetup/TripSetupViewModel.kt`
- `R:drawable/ic_add.xml`
- `R:drawable/ic_check.xml`
- `R:drawable/ic_chevron_right.xml`
- `R:drawable/ic_close.xml`
- `R:drawable/ic_edit.xml`
- `R:drawable/ic_nav_home.xml`
- `R:drawable/ic_people.xml`
- `R:drawable/ic_record.xml`
- `R:drawable/ic_stop.xml`
- `T:data/draft/DraftRepositoryTest.kt`
- `T:data/trip/TripPauseSyncTest.kt`
- `T:data/trip/TripRecordPlaceUseCaseTest.kt`
- `T:domain/PlaceCandidatesTest.kt`
- `T:domain/RecordingSessionTest.kt`
- `T:domain/RouteRulesTest.kt`
- `T:domain/TripNameTest.kt`
- `T:testing/FakeTripRepositories.kt`
- `T:ui/placeinput/PlaceInputViewModelTest.kt`
- `T:ui/tripdetail/TripDetailViewModelTest.kt`
- `T:ui/tripsetup/TripSetupViewModelTest.kt`
