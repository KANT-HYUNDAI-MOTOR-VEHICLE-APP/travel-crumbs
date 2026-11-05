# ADR 0012 — CI 에 app 잡을 넣음(2026-11-05 제안)

> 원본 규칙: `team-workflow` 9절(CI 잡 정의) · `.github/workflows/ci.yml`. 이슈 #8.

## 무엇이 있었나

- 스펙 PR #7(#6) · #5(#4)에서 `ci / changes` 가 `Resource not accessible by integration` 으로 실패.
- 권한(`pull-requests: read`)은 이미 develop 에 있었음(6fb9607 · PR #1). 기능 브랜치 `app-shell/main` · `navigation/main` 이 그 전에 따져서 빠졌던 것.
- 같은 조사에서 `ci.yml` 에 app 잡이 아예 없음을 확인 — 첫머리 메모 「app 잡은 앱 쪽 작업에서 더함」만 남아 있었음. `team-workflow` 9절은 app 잡을 정의해 두었지만 실제로는 앱 코드 PR 이 CI 에서 빌드 · 린트 · 테스트 없이 들어갈 수 있었음.
- #6 · #4 구현 PR 이 곧 올라옴.

## 결정

- `ci.yml` 에 `app` 잡을 넣음 — 경로 필터 `app/**` · `gradle/**` · `*.gradle.kts` · `gradle.properties` · `check_manifest.py` · `ci.yml`.
- 명령은 로컬과 같게: `./gradlew lint testDebugUnitTest assembleRelease` → 서명 없는 release APK 아티팩트 → `check_manifest.py`.
- JDK 는 로컬 Android Studio 내장 JBR 과 같은 메이저(25 · temurin). compileSdk 36.1 · buildTools 36.0.0 은 `sdkmanager` 로 설치(러너 기본 SDK 에 없을 수 있음).
- `check_manifest.py` 는 빌드 · APK 업로드 뒤 마지막 단계 — 빨강이어도 빌드 · 테스트 결과와 APK 가 먼저 남음.
- 매니페스트 센서의 지금 빨강(`NAVI_ROUTE` 허용 목록 밖)은 숨기지 않음 — 허용 목록 · DO 는 하네스 피드백 #2 몫.

## 이유

- 두 사람이 같은 홈 · 매니페스트를 고치는 구현 PR(#6 · #4)이 곧 들어옴 — 사람 확인 전에 빌드 · 테스트가 기계로 돌아야 함.
- 로컬 센서와 CI 가 같은 명령이라는 `sensors` · `team-workflow` 약속.
- 로컬(윈도우 · JBR 25)에서 같은 Gradle 명령이 녹색임을 확인(2026-11-05).

## 버린 안

| 안 | 버린 이유 |
|---|---|
| 첫 구현 이슈에서 builder 가 app 잡을 넣게 둠 | #6 · #4 두 구현이 동시에 와서 둘 다 `ci.yml` 을 고치면 충돌 · 그사이 앱 PR 이 검사 없이 들어감 |
| 매니페스트 센서를 #2 반영 전까지 빼 둠 | 센서를 끄는 것은 금지(`sensors`) · 빨강을 보이는 채로 두고 #2 에서 고침 |
| JDK 17(소스 · 타깃 판)로 맞춤 | 로컬 빌드 JDK(JBR 25)와 달라 「로컬과 같은 명령」이 깨짐 |

## 바뀌는 파일

| 파일 | 바뀌는 것 |
|---|---|
| `.github/workflows/ci.yml` | `changes` 에 `app` 필터 · `app` 잡 추가 · 첫머리 메모 정리 |
| `.claude/skills/team-workflow/SKILL.md` | 9절 app 행(트리거 · JDK · SDK 설치 · 단계 순서) · 절 제목(판을 바꾸면 이 잡도) |
