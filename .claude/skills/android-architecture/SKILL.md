---
name: android-architecture
description: travel-crumbs Android 앱(app/)의 구조 규칙 원본 — Google 권장 MVVM(ui → domain → data) · 패키지 배치 · 이름 규칙 · 화면 1개 = 파일 1개 · ViewModel 순수성 · 자료원은 저장소 거쳐 · UDF · 바뀔 값 한 곳 · Vehicle SDK 경계 · Hilt 모듈 경계 · 네트워크(Retrofit) · 토큰 저장 · 오프라인 큐, 그리고 구조 리뷰 점검표와 Konsist 로 재는 법. app/ 코드를 쓰거나 고치기 전(builder) · 스펙의 화면 · 상태 절을 쓰기 전(spec-writer) · 앱 구조 리뷰(structure-reviewer) 때 반드시 읽는다. 서버(server/)에는 쓰지 않는다 — server-architecture.
---

# Android 앱 구조 — Google 권장 MVVM

근거: Google 「Guide to app architecture」 · 「Recommendations」(https://developer.android.com/topic/architecture/recommendations). 규칙마다 재는 법을 적음 — 센서가 재는 규칙은 리뷰어가 다시 세지 않음.

## 패키지 배치(`com.example.<패키지>`)

```
<앱>Application.kt         @HiltAndroidApp
MainActivity.kt             @AndroidEntryPoint · setContent 만 · 수명 이벤트를 VehicleConnection 에 전달
di/                         Hilt 모듈만 · 도메인별 파일(VehicleModule · NetworkModule · AuthModule · <도메인>Module)
ui/<화면>/<화면>Screen.kt · <화면>ViewModel.kt · <화면>UiState.kt     화면 1개 = 폴더 1개
ui/navigation/              목적지 · 주행 중 허용 화면 판정
ui/theme/
domain/model/               DrivingState(PARKED · DRIVING · UNKNOWN) · Session · <도메인 모델>
domain/<동사>UseCase.kt      ObserveDrivingState · <동사><대상>
domain/AppConfig.kt         바뀔 수치 한 곳(간격 · 한도 · 개수)
data/vehicle/               VehicleGearDataSource(SDK 감싸는 유일한 파일) · UxRestrictionsDataSource · DrivingSignalRepository
data/location/              LocationDataSource · LocationRepository(위치를 쓸 때만)
data/<도메인>/               <자료원>DataSource · <도메인>Repository
data/remote/                AppApi(Retrofit 인터페이스 · 계약과 1:1) · DTO · AuthInterceptor · TokenAuthenticator
data/local/                 Room(오프라인 큐) · DataStore(SessionStore)
data/auth/                  AuthRepository
```

## 규칙 8

| 번호 | 규칙 | 근거 | 재는 법 |
|---|---|---|---|
| 1 | import 방향 `ui → domain → data` 한쪽. `domain` 은 `ui` · `data` · `android.*` · `androidx.*` 를 import 하지 않음. `data` 는 `ui` 를 모름 | 계층 분리 | Konsist: 패키지별 import |
| 2 | 화면 1개 = 파일 1개 — `*Screen.kt` 에 최상위 `@Composable` 화면 함수 하나. 부품은 `components/` 로 | 수정 범위 격리 · 두 사람 충돌 감소 | Konsist: 파일당 화면 함수 수 |
| 3 | ViewModel 은 `Context` · `Activity` · `Resources` · `android.view` · `androidx.compose` 를 들지 않음 · `AndroidViewModel` 안 씀 · 화면 단위에만 | 수명 주기 독립 | Konsist: 생성자 파라미터 · import |
| 4 | 자료원(GPS · SDK · TTS · assets · Retrofit · Room · DataStore)은 저장소(Repository)를 거쳐서만. Composable · ViewModel 이 `*DataSource` · `AppApi` · Dao 를 직접 부르지 않음 | 저장소로 노출 | Konsist: `ui` 에서 금지 타입 참조 |
| 5 | UDF — ViewModel 은 `uiState: StateFlow<…UiState>` 하나 · `stateIn(viewModelScope, WhileSubscribed(5000), 초기값)` · UI 로 이벤트를 쏘지 않음(결과를 상태로) · 사용자 행동은 메서드 호출 · 화면은 `collectAsStateWithLifecycle` | UDF | 구조 리뷰 |
| 6 | 바뀔 값 한 곳 — 콘텍츠 `assets/*.json` · 수치 `AppConfig` · 문구 `strings.xml` · 서버 주소 `BuildConfig`(빌드 타입별) · 같은 숫자 · 문자열 두 곳 금지 | 바뀌기 쉬운 구조 | 구조 리뷰 + Lint(HardcodedText) |
| 7 | SDK 타입(`ai.pleos.*`)은 `data/vehicle/` 밖 금지 · `initialize()` 한 번 · `release()` 한 번 | SDK 가이드 · 샘플 앱 | Konsist: import 위치 |
| 8 | `@Module` 은 `di/` 에만 · Retrofit 인터페이스는 `data/remote/` 에만 · DTO 에 좌표(lat · lng · latitude · longitude) 칸 없음 · 앱 코드에 `clientSecret` · `api.pleos.ai/v1/auth/token` 문자열 없음 | 바꾸는 곳 한 곳 · 최소 수집 · 비밀은 서버만 | Konsist + `check_api_contract.py` + `check_secrets.py` |

## 상태 · 수명 규칙(규칙 5 의 세부)

- UiState 는 불변 `data class` · 화면이 그릴 것만(주행 중 · 재생 중 · 버튼 표시 여부 · 선택 결과 · 오류 코드).
- 일회성 결과(저장됨 · 실패)도 상태 칸으로 — 화면이 보고 지움(`onMessageShown()`).
- 수명 작업은 Compose 효과(`LifecycleStartEffect` · `repeatOnLifecycle`)로 · Activity 콜백 재정의 금지(SDK `release()` 경로 하나만 예외).
- 프로세스 재생성: `SavedStateHandle` 로 화면 상태 복원.

## 네트워크 · 세션 · 오프라인

- Retrofit + OkHttp + kotlinx.serialization 컨버터. 인터페이스 `AppApi` 는 `docs/api/openapi.json` 과 경로 · 메서드 1:1(센서 `check_api_contract.py`).
- 토큰: `AuthInterceptor`(헤더) · `TokenAuthenticator`(401 → refresh → 재시도 1회). 저장은 DataStore + Keystore 로 감싼 키 암호화. 비밀번호 · Pleos 토큰은 앱에 없음.
- 데이터 원본은 기기 안: 여행 기록 · 공유는 Room · DataStore 가 원본이고 서버로 보내지 않음. 서버로 가는 것은 여러 사용자 데이터가 필요한 것(추천 근거 집계 · 로그인)만 · 스펙의 「최소 수집」 절에 적힌 칸만(ADR 0014).
- 오프라인: 서버로 보낼 기록(예: 안내 시작 기록)은 Room 전송 대기 큐에 먼저 → 네트워크 되면 전송 · 실패는 재시도(지수 백오프) · 보내면 큐에서 지움. 전송 상태를 화면에 보일지는 스펙이 정함.
- 타임아웃 · 재시도 수치는 `AppConfig` 또는 `NetworkConfig` 한 곳.
- debug 빌드만 `10.0.2.2:4000` cleartext(`network_security_config`) · release 는 HTTPS 만.

## Hilt

- `@HiltAndroidApp` · `@AndroidEntryPoint` · `@HiltViewModel` + `@Inject constructor`.
- 모듈은 `di/` 도메인별 파일 · 인터페이스 ↔ 구현은 `@Binds` · 외부 객체(Retrofit · Room · SDK)는 `@Provides` · `@Singleton` 은 공유 상태 · 비싼 객체만.
- KSP 판 = 내장 Kotlin 판(`pleos-vehicle-sdk` 걸림 ②).

## 테스트

- 단위: 도메인(순수 계산 · 기어 → DrivingState) · ViewModel(fake 저장소 · `uiState.value` 단언 · `kotlinx-coroutines-test`).
- Konsist 테스트 하나(`app/src/test/.../ArchitectureTest.kt`)에 규칙 1 · 2 · 3 · 4 · 7 · 8.
- Compose UI 테스트: 주행 ↔ 정차 전환 시 정차 전용 버튼 표시 · 숨김.
- fake 우선 · mock 라이브러리 안 씀. 테스트 이름에 시험 케이스 번호.

## 구조 리뷰 점검표(structure-reviewer)

1. 센서 결과(`04_sensors.md`) 먼저 — Konsist · Lint 가 빨간 규칙은 「센서 실패 상태로 넘어옴」 한 줄.
2. 바뀐 파일마다: 어느 패키지인가 → 그 패키지가 import 하면 안 되는 것(규칙 1 · 4 · 7 · 8)을 grep.
3. 새 `*Screen.kt`: 최상위 `@Composable` 하나인가 · ViewModel 을 화면 단위에서만 받나.
4. 새 ViewModel: `uiState` 하나인가 · 이벤트 Flow 가 있나(위반) · 생성자에 Context 류가 있나.
5. 숫자 · 문자열 리터럴: 같은 값이 다른 파일에도 있나(규칙 6) · `strings.xml` 밖의 한글 UI 문구.
6. DTO: 좌표 칸 · userId 칸(서버가 토큰에서 얻음 — 요청에 넣지 않음).
7. 표로: 파일:줄 · 규칙 번호 · 고칠 방향 · 린트로 만들 수 있나 · 반복 여부.

## Konsist 규칙 초안(builder 가 테스트로 옮김)

```kotlin
// 1 계층: domain 은 ui · data · android 를 모름
Konsist.scopeFromProject().files.withPackage("..domain..")
  .assertFalse { it.hasImport { i -> i.name.startsWith("com.example.<패키지>.ui") || i.name.startsWith("com.example.<패키지>.data") || i.name.startsWith("android") || i.name.startsWith("androidx") } }
// 2 화면 1파일: *Screen.kt 의 최상위 @Composable 은 하나
// 3 ViewModel: 생성자에 Context · Activity 없음 · androidx.compose import 없음
// 4 ui 는 *DataSource · AppApi · *Dao 를 참조하지 않음
// 7 ai.pleos import 는 ..data.vehicle.. 에만
// 8 @Module 은 ..di.. 에만 · retrofit2.http import 는 ..data.remote.. 에만
```

## 참고
Google 아키텍처 안내 https://developer.android.com/topic/architecture · UI 층 https://developer.android.com/topic/architecture/ui-layer · 데이터 층 https://developer.android.com/topic/architecture/data-layer · Hilt https://developer.android.com/training/dependency-injection/hilt-android · Konsist https://docs.konsist.lemonappdev.com/ · callbackFlow https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html
