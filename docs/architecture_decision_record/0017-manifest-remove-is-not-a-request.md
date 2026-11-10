# ADR 0017 — 매니페스트의 `tools:node="remove"` 권한 선언은 요청 권한이 아님(2026-11-10 · 이슈 #73)

> 원본 규칙: `sensors` 「권한 허용 목록을 늘리는 길」 · `.claude/skills/sensors/scripts/check_manifest.py` · 관련 PR #71 · 이슈 #59.

## 무엇이 있었나

- #59(추천 지도)에서 MapLibre Native Android 13.7.1 을 넣자 라이브러리 매니페스트가 `ACCESS_WIFI_STATE` · `ACCESS_COARSE_LOCATION` · `ACCESS_FINE_LOCATION` 을 앱에 끌어옴. 앱은 이 권한을 쓰지 않아 `app/src/main/AndroidManifest.xml` 에 `tools:node="remove"` 3줄로 병합 결과에서 지움.
- `check_manifest.py` 는 `<uses-permission>` 을 모두 요청 권한으로 세서 이 「지우기」 선언에 위반 3 을 냄. release 병합 매니페스트에는 세 권한이 없었음(심사관 리뷰 확인).
- 통과시키는 길이 ① 지우기 선언을 빼기(위치 권한이 실제로 들어감) ② `ALLOWED` 에 위치 권한 추가(센서 완화)뿐이라, 센서가 오히려 잘못된 쪽으로 몰았음.

## 결정

- `tools:node` 가 `remove` · `removeAll` 인 `<uses-permission>` 은 요청 권한에서 뺌. 위반이 아니라 「제거 선언 n개」 한 줄로 알림.
- `merge` · `replace` 등 다른 표시는 권한이 남으므로 지금처럼 요청 권한으로 셈.
- `ALLOWED` · 백그라운드 위치 금지 · 그 밖의 검사는 그대로. 지운 백그라운드 위치도 위반이 아님(결과 앱에 권한이 없음).
- 시험: `.claude/guardrails/tests/test_check_manifest.py` 의 `RemovedByMerge`.

## 이유

- 센서가 재려는 것은 「앱이 요청하는 권한이 기능과 1:1 인가」(심사 5항목 · 최소 권한). 지우는 선언은 그 수를 줄이는 쪽이라 막을 이유가 없고, 막으면 권한을 늘리거나 목록을 넓히는 쪽으로 사람을 밀어 규칙의 뜻과 반대가 됨.
- 병합 매니페스트를 재는 방법도 있으나 SDK 가 붙이는 권한(VEHICLE_BINDING 등)까지 걸려 `ALLOWED` 가 SDK 사정으로 커짐 — 소스 매니페스트에서 표시를 읽는 쪽이 원본 한 곳 원칙에 맞음.
- `remove` 만 특별 취급하고 나머지 표시는 그대로 셈으로써 「지우는 것처럼 보이지만 실제로는 남는」 선언이 새지 않게 함.
