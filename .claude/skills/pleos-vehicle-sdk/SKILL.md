---
name: pleos-vehicle-sdk
description: Pleos Connect(AAOS) 앱에서 Pleos Vehicle SDK 로 차량 신호(기어)를 읽고, 주행 규제(CarUxRestrictions) · 오디오 포커스 · 위치 · TTS 를 다루는 방법과 제약. 배포소 · 의존성 · 권한은 쓰는 도메인만 · capability → 읽기 → Callback → callbackFlow · initialize 한 번 · release · Virtual Console 시험 · 알려진 걸림 · 공식 문서 주소. travel-crumbs 앱 코드(app/)를 쓰거나 스펙을 쓰기 전에 builder(app) · spec-writer 가 반드시 읽는다. 서버 코드 · Pleos 계정 로그인 API(서버 쪽)는 server-architecture 가 맡는다.
---

# Pleos Vehicle SDK — travel-crumbs 앱이 지키는 것

설명은 명사형. 「확인」 표시는 builder 가 에뮬레이터 · 문서로 확인해 보고서에 적는 항목.

## 1. 가져오기

| 항목 | 값 |
|---|---|
| 배포소 | `maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }` — `settings.gradle.kts` 의 `dependencyResolutionManagement.repositories` 에 추가(공개 · 자격증명 없음) |
| 의존성 | `ai.pleos.playground:Vehicle:2.3.2`(버전 카탈로그 `vehicleSdk`) · 끌어오는 `kotlin-reflect` 판을 내장 Kotlin 판과 맞춰 명시 |
| 문서 | SDK 가져오기 https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup · 샘플 앱 https://github.com/Pleos-Playground/pleos-sample-vehicle-app (매니페스트 · 빌드 설정의 기준) |

## 2. 권한 — 쓰는 도메인만

- 기어(DrivingMode) 하나 → `pleos.car.permission.CAR_DRIVE` 하나. protectionLevel normal(런타임 대화상자 없음).
- 다른 `CAR_*` · `CONTROL_CAR_*` · `PRIVATE_*` 선언 금지 — 심사 「최소 권한」. 센서 `check_manifest.py` 가 잼.
- 차량 권한 밖(위치 · 네트워크 · 포그라운드 서비스 등)을 포함한 **앱 전체 허용 목록의 원본은 `check_manifest.py` 의 `ALLOWED`** · 늘리는 기준은 `sensors` 「권한 허용 목록을 늘리는 길」. 여기에 목록을 베껴 두지 않음(한 곳만 고쳐져 어긋나지 않게).
- 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission

## 3. 호출 순서(한 파일 — `data/vehicle/VehicleGearDataSource.kt`)

1. `Vehicle(context.applicationContext)` 하나 · `initialize()` 한 번 — 완료 전 속성 호출 금지.
2. `checkXxxCapabilities()` → `CapabilityConfig.accessType` 으로 읽기 가능 확인. 불가 → `DrivingState.UNKNOWN`.
3. `getCurrentXxx(onComplete, onFailed)` 로 첫 값.
4. 변화는 Callback API 로 구독 → `callbackFlow { register; awaitClose { unregister } }`.
5. `release()` 는 Activity `onDestroy` 경로로 한 번.
6. SDK 타입(`ai.pleos.*`)은 이 패키지 밖으로 안 나감 — 도메인 enum(`Gear` · `DrivingState`)으로 바꿔 내보냄.
- 문서: capability https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability · 읽기 https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property · 콜백 https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/callback

## 4. 주행 판정 — 둘 다 봄

| 신호 | 출처 | 규칙 |
|---|---|---|
| 기어 | Vehicle SDK DrivingMode | P → PARKED · R/N/D → DRIVING |
| 주행 규제 | AAOS `CarUxRestrictionsManager`(설계 가이드 「주행 안전」: 앱은 CarUxRestrictions 로 제한 상태 확인) | 제한 있음 → DRIVING |
| 합성 | `ObserveDrivingStateUseCase` | 하나라도 DRIVING → DRIVING · 둘 다 PARKED → PARKED · 모름 → UNKNOWN(화면은 DRIVING 처럼) |

확인: 에뮬레이터에서 UxRestrictions 값이 오는지. 안 오면 기어만으로 판정하고 보고서에 적음.

## 5. 주행 중 화면 규칙(설계 가이드 · 심사 「운전자 주의 분산」)

- 주행 중 사용 선언(DO): 주행 중에 쓰는 화면이 있는 액티비티는 `<meta-data android:name="distractionOptimized" android:value="true"/>` 를 선언함 — Pleos 「지역별 주행 규제 정책」은 DO 를 선언하지 않은 앱을 주행 중 사용 불가로 봄. 선언은 「주행 중에도 열림」일 뿐이고, 무엇을 숨길지는 코드가 4절 판정 · `CarUxRestrictions` 의 `ActiveRestrictions` 를 보고 정함(ADR 0013).
- 주행 중: 영상 · 애니메이션 · 자동 화면 전환 없음 · 긴 글 없음 · 정차 전용 화면 · 로그인 · 입력 화면 숨김 · 오디오 재생은 앱 카테고리가 허용할 때만 계속(카테고리 문서 참고).
- 정차: 버튼은 크고 고대비 · 한 번 터치로 끝 · 키보드 입력 최소(로그인은 WebView 한 번).
- 창: `screenOrientation` 미지정(portrait · 창 1개) · 창 크기 변경에도 핵심 기능 유지.
- 백그라운드: 화면 갱신 · 위치 수집 정지 · 재생 중인 오디오는 끝까지(필요할 때) · 프로세스 재생성 뒤 상태 복원.
- 성능: 주행 중 CPU 5% 미만 · RAM 400 MB 이하.
- 문서: 주행 안전 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline · 앱 구조 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model · 성능 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt · 카테고리 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category

## 6. 오디오 — 포커스 대응 필수

- `AudioFocusRequest` 로 획득 → 재생 · 상실(내비 안내 등) → 일시 정지 · 다시 획득 → 이어서. 대응 로직 없는 재생은 심사 반려 사유.
- TTS 는 기기 엔진(`TextToSpeech`) · 네트워크 TTS 안 씀. 확인: 에뮬레이터에 엔진 · 한국어 음성(`TextToSpeech.engines` · `isLanguageAvailable`).
- 문서: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development

## 7. 위치

- Google Fused(Play 서비스 `FusedLocationProviderClient`) · Geofencing 클라이언트는 안 씀 — Play 서비스 없음 전제. 확인: `adb shell pm list packages com.google.android.gms`.
- Pleos FusedLocation SDK(권한 `pleos.car.permission.FUSED_LOCATION`)는 Google Fused 와 별개 — `isSupported()` 가 참이면 쓰고, 아니면 프레임워크 `LocationManager` → `callbackFlow`. 어느 쪽을 쓸지는 스펙이 정함. 2026-11 기준 Pleos 콘솔 My Project 에서 FusedLocation 은 「준비 중」 · 에뮬레이터에는 `ai.pleos.playground.service.location.fused` 탑재. 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro
- 위치 판정(예: 지역 진입 = 하버사인 ≤ 반경)은 도메인 계산. 좌표는 기기 밖으로 안 나감.
- 시험: 에뮬레이터 확장 컨트롤 → Location → GPX 재생(`docs/test-routes/*.gpx` · 앱이 쓰는 좌표 자료와 같은 좌표).

## 8. Virtual Console(기어 바꾸기)

```
adb forward tcp:8088 tcp:8088   →  http://127.0.0.1:8088/  →  Motion → Gear(P · R · N · D)
```
속도 · GPS 없음. 문서: https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide

## 9. 알려진 걸림(선택지 중 사람 손이 안 드는 쪽 먼저 · 결정 · 근거는 보고서에)

| 번호 | 걸림 | 선택지 |
|---|---|---|
| ① | 템플릿 androidx.core 1.19.1 → compileSdk 37 요구 · 설치 플랫폼 36.1 까지 · `sdkmanager` 없음 | core 판을 compileSdk 36 이 받는 판으로 내림(사람 손 없음) / 플랫폼 37 설치(두 사람 모두 Android Studio 에서 · 사람 손) |
| ② | Compose 컴파일러 · kotlinx.serialization · KSP(Hilt) 플러그인 판 = AGP 9 내장 Kotlin 2.2.10 | 공식 호환표로 판 고정 · 막히면 보고(수동 DI 로 물러나지 않음) |
| ③ | Vehicle SDK 2.3.2 가 kotlin-reflect 1.9.25 를 끌어옴 | 카탈로그에 Kotlin 판으로 명시 |
| ④ | 에뮬레이터의 Play 서비스 · TTS 한국어 · WebView · UxRestrictions 유무 | 각각 확인 명령 → 결과 · 대응을 보고서에 |
| ⑤ | 맥(arm64) ↔ 윈도우(x86_64) 이미지 ABI 다름 | 동작 같아야 함 · 다르면 보고 |

## 10. 에뮬레이터 설치(두 OS)

SDK Manager → SDK Update Sites → Pleos 주소 추가(맥용 · 윈도우/리눅스용 URL 따로) → SDK Platforms → API 34 의 Pleos Connect 이미지 → AVD(Automotive · Pleos Connect v2.1.0 · RAM 4 GB · Cold boot · 이름 `Pleos_Connect_v2.1.0`).
문서: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator

## 참고 — 전체 문서 차례
시작 https://document.pleos.ai/docs/pleos-only/getting-started · 설계 가이드 차례 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro · 심사 가이드 https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/ · UI 디자인 가이드 https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro
