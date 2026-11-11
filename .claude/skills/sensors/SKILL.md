---
name: sensors
description: travel-crumbs 저장소의 계산형 센서 — 앱(Android Lint · 단위 테스트 · Konsist · assembleRelease · 매니페스트 검사) · 서버(ruff · mypy · import-linter · pytest · alembic check · heads · SQL comment · response_model 검사) · 공통(API 계약 ↔ 앱 Retrofit 대조 · 데이터 표준 대조 · 비밀 리터럴 · 저장소 위생 · 하네스 설명 잘림 · 환경 doctor). 명령은 로컬(맥 · 윈도우)과 CI 가 같다. builder 가 넘기기 전, 메인이 PR 전 · 머지 뒤 센서를 돌릴 때, CI 를 쓰거나 고칠 때, 리뷰어가 「센서가 재는 규칙」을 확인할 때 사용한다. 새 센서를 넣는 일(리뷰 반복 지적 → 스크립트)은 harness:evolve 가 이 스킬을 고친다.
---

# 센서 — 계산으로 재는 것

원칙: 사람 · 에이전트 리뷰가 두 번 같은 것을 지적하면 여기로 옮김. 센서는 끄거나 baseline 으로 숨기지 않음. 명령은 두 OS · CI 가 같은 글자.

스크립트 위치: `.claude/skills/sensors/scripts/`(이 폴더가 원본 · CI 도 이 경로를 부름 · 복사본 없음). 전부 Python 3 표준 라이브러리만 · `python <경로> --help`.

## 명령 묶음

| 영역 | 명령(저장소 루트에서 · 윈도우는 `gradlew.bat`) | 막는 것 |
|---|---|---|
| 앱 빌드 · 테스트 | `./gradlew lint testDebugUnitTest assembleRelease` | Lint 오류 · 단위 테스트 · Konsist 규칙(테스트에 들어 있음) · 빌드 |
| 앱 매니페스트 | `python .claude/skills/sensors/scripts/check_manifest.py app/src/main/AndroidManifest.xml --gradle app/build.gradle.kts` | 허용 밖 권한 · minSdk < 28 · cleartext · exported · allowBackup · 네트워크 보안 설정 누락 |
| 서버 정적 · 테스트 | `cd server && uv sync --frozen && uv run ruff check . && uv run ruff format --check . && uv run mypy app && uv run lint-imports && uv run pytest` | 린트 · 포맷 · 타입 · import 방향 · 단위 · 통합 |
| 서버 스키마 | `cd server && uv run alembic check && python ../.claude/skills/sensors/scripts/check_alembic_heads.py --server-dir . && python ../.claude/skills/sensors/scripts/check_sql_comments.py app` | 모델 ↔ 리비전 어긋남 · 갈래 둘 · `comment=` 누락 |
| 서버 라우트 | `python .claude/skills/sensors/scripts/check_routes.py server/app` | `response_model` · 반환 타입 없는 라우트 |
| 계약 생성 | `cd server && uv run python -m app.tools.export_openapi ../docs/api/openapi.json && git diff --exit-code ../docs/api/openapi.json` | 계약 산출물 미갱신 |
| 계약 대조 | `python .claude/skills/sensors/scripts/check_api_contract.py docs/api/openapi.json app/src/main/java` | 앱 Retrofit 경로 · 메서드가 계약에 없음 · DTO 좌표 칸 · 요청에 userId 칸 |
| 데이터 표준 | `python .claude/skills/sensors/scripts/check_data_standard.py server/app app/src/main/java` | 테이블명 끝 유형 단어 · PK 이름 · snake_case · 표준 용어 사전에 없는 컬럼 · 공통 컬럼 누락(원본 `docs/data/standard.md`) |
| 비밀 | `python .claude/skills/sensors/scripts/check_secrets.py` | 키 · 토큰 · 비밀 리터럴 · 앱 폴더의 `clientSecret` · Pleos 토큰 엔드포인트 |
| 저장소 위생 | `python .claude/skills/sensors/scripts/check_repo_hygiene.py` | CRLF · 대소문자만 다른 파일 쌍 · 긴 경로 · `gradlew` 실행 비트 · 심볼릭 링크 |
| 하네스 장치 | `python .claude/guardrails/tests/test_guard.py && python .claude/guardrails/tests/test_trace.py && python .claude/guardrails/tests/test_check_manifest.py` | guard 가 막아야 할 명령을 통과시킴 · 활동 로그가 비밀값을 남기거나 작업을 막음 · 권한 허용 목록에 근거 없는 줄 · 금지 권한이 새어 나감 |
| 하네스 설명 | `python .claude/skills/sensors/scripts/check_skill_frontmatter.py` | 스킬 · 에이전트 frontmatter 깨짐 · description 이 ` #` 또는 `: ` 에서 잘려 트리거 문구가 실리지 않음 |
| 환경 | `python .claude/skills/sensors/scripts/doctor.py` | 맥 · 윈도우의 JDK · SDK · Pleos 이미지 · AVD · adb · Docker · uv · gh · git 설정 차이 |

종료 코드: 0 통과 · 1 위반 · 2 입력 오류. 출력은 `파일:줄 · 규칙 · 내용` 한 줄씩 — 리뷰 표에 바로 옮김.

## 데이터 표준 센서의 심각도는 정의서가 정함

`check_data_standard.py` 는 규칙마다의 심각도를 코드에 박지 않고 `docs/data/standard.md` 의 **상태 칸**에서 읽음 — 「결정」이면 위반(종료 코드 1) · 「제안 · 미결 · 가안」이면 경고(통과). 아직 사람이 확정하지 않은 초안으로 빌드를 막으면 정의서와 센서가 어긋나고, 막힌 쪽이 센서를 끄게 됨. 정의서에서 상태를 「결정」으로 올리면 스크립트를 고치지 않아도 그날부터 위반이 됨 — 규칙의 무게를 정하는 곳이 한 곳(정의서)이어야 함.

같은 이유로 **정의서를 고치는 것은 센서의 일도 builder 의 일도 아님**(사람 · ADR 0016). 센서는 「사전에 없음」까지만 말하고, 단어를 등록할지는 사람이 정함.

## 권한 허용 목록을 늘리는 길

앱 권한의 **허용 목록 원본은 `check_manifest.py` 의 `ALLOWED` 한 곳**(권한 → 왜 쓰는지). 다른 문서는 목록을 베끼지 않고 이 스크립트를 가리킴 — 같은 목록이 여러 곳에 있으면 한 곳만 고쳐져 규칙과 센서가 어긋남.

라이브러리가 끌어온 권한을 `<uses-permission … tools:node="remove" />`(또는 `removeAll`)로 지우는 선언은 **요청 권한으로 세지 않고** 「제거 선언 n개」로 알리기만 함(ADR 0017 · #73). 쓰지 않는 권한은 목록을 늘리지 말고 이렇게 지움. `merge` · `replace` 같은 다른 표시는 권한이 남으므로 지금처럼 셈.

새 권한이 필요해지면 네 가지를 모두 채운 뒤에 목록에 넣음. 하나라도 비면 넣지 않음.

| 기준 | 왜 |
|---|---|
| 승인된 스펙(`docs/spec/<기능명>/artifacts/`)에 그 권한이 왜 필요한지 적혀 있음 | 권한은 기능에서 나옴 — 코드를 쓰다 「필요해서」 늘리면 심사 「최소 권한」이 무너짐 |
| 그 기능을 권한 없이 할 길이 없음 | 대체 수단이 있으면 그쪽. 예: 권한이 필요한 차량 신호 대신 다른 자료로 대신하거나 안 쓰기로 함 |
| 심사 가이드에 어긋나지 않음 — `pleos-review-checklist` 5항목 · 백그라운드 위치 같은 금지 권한은 예외 없음 | 센서를 통과해도 심사에서 반려되면 뜻이 없음 |
| `ALLOWED` 에 **근거를 함께** 적음(스펙 경로 · 이슈 번호) | 다음 사람이 「이게 아직 필요한가」를 판단할 수 있어야 함. 근거 없는 줄은 지울 수도 남길 수도 없게 됨 |

- 목록을 고치는 것은 하네스 변경임 → `[하네스 피드백]` 이슈 → `harness:evolve`. 기능 브랜치에서 슬쩍 늘리지 않음.
- `--allow` 는 **스펙 승인 전에 손으로 한 번 확인해 볼 때만**. 커밋하는 명령 · CI 잡에는 쓰지 않음 — `sensors` 의 전제가 「명령은 로컬과 CI 가 같다」이고, 한쪽에만 `--allow` 가 붙으면 그 전제가 깨짐.
- 쓰지 않게 된 권한은 그 기능을 지우는 변경에서 함께 뺌(근거가 가리키는 스펙이 사라지면 그 줄도 사라짐).

## 결과 기록

메인은 센서 결과를 `_workspace/<이슈>/04_sensors.md` 에 표(명령 · 결과 · 첫 오류 줄)로. 리뷰어는 이 파일을 먼저 읽고 센서가 재는 규칙은 다시 세지 않음.

## 센서가 못 재는 것(리뷰어 몫)

UDF 위반(이벤트 Flow) · 바뀔 값 두 곳 · 로그 마스킹 누락 · LLM 프롬프트의 개인 데이터 · 콘텐츠 안전 · 문구. 이 중 두 번 지적되면 「센서 후보」.

## 새 센서 넣기(`harness:evolve`)

1. 반복 지적의 규칙을 한 문장으로 → 파일 · 문자열 · 선언 유무로 환원되나 확인.
2. 환원되면 스크립트(여기) · Konsist(앱 테스트) · import-linter(서버 계약) 중 가장 가까운 곳에.
3. 위 표에 한 줄 · CI 잡(`team-workflow` 9절)에 같은 명령 · 바꾼 이유는 `docs/architecture_decision_record/` 에 기록.
