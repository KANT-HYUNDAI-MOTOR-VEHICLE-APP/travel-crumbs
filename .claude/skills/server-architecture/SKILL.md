---
name: server-architecture
description: travel-crumbs 서버(server/ · Python · FastAPI)의 구조 규칙 원본 — 도메인 패키지 배치(router → service → repository → db) · Pydantic 스키마 응답 · 시퀀스 PK 와 외부 노출 UUID · 설정 객체(pydantic-settings) · 로그 마스킹 · Alembic 리비전 · OpenAPI 내보내기 · Pleos 계정 로그인(auth/pleos) · LLM 경계 규칙 7(app/llm) · 데이터 표준 준수(docs/data/standard.md) · 테스트(pytest · testcontainers · respx), 그리고 구조 리뷰 점검표와 import-linter 로 재는 법. server/ 코드를 쓰거나 고치기 전(builder) · 스펙의 데이터 · API 계약 절을 쓰기 전(spec-writer) · 서버 구조 리뷰(structure-reviewer) 때 반드시 읽는다. 앱(app/)에는 쓰지 않는다 — android-architecture.
---

# 서버 구조 — Python · FastAPI

근거: FastAPI 문서 「Bigger Applications」 · 「Settings」 · SQLAlchemy 2.0 · Alembic · Pleos 심사 「개인정보 · 보안」. 규칙마다 재는 법을 적음 — 센서가 재는 규칙은 리뷰어가 다시 세지 않음.

## 도구 묶음(바꾸려면 ADR)

| 용도 | 도구 | 명령(맥 · 윈도우 같음) |
|---|---|---|
| Python · 패키지 | uv(`pyproject.toml` · `uv.lock` · `.python-version` 3.12) | `uv python install 3.12` · `uv sync` · `uv run …` |
| 웹 | FastAPI + uvicorn | `uv run uvicorn app.main:app --reload --port 4000` |
| DTO · 설정 | Pydantic v2 · pydantic-settings(`.env`) | — |
| DB | SQLAlchemy 2.0 async(asyncpg) · Alembic · PostgreSQL 17(Docker) | `docker compose up -d db` · `uv run alembic upgrade head` |
| 세션 | PyJWT(HS256 · 액세스 1h · 리프레시 30d · `token_version`) | — |
| 외부 HTTP | httpx(async · 타임아웃 · 재시도) · 테스트 respx | — |
| 속도 제한 | slowapi | `/auth/pleos/*` · 웹훅 · `/llm/*` |
| LLM | `openai` 패키지(OpenAI 호환 · `base_url` 로 제공자 교체) | — |
| 테스트 | pytest · pytest-asyncio · httpx `AsyncClient(ASGITransport)` · testcontainers(PostgreSQL) · respx | `uv run pytest` |
| 정적 | ruff(lint + format) · mypy --strict · import-linter | `uv run ruff check . && uv run ruff format --check . && uv run mypy app && uv run lint-imports` |
| 로그 | structlog(JSON) · 마스킹 프로세서 하나 | — |

## 폴더

```
server/
├─ pyproject.toml · uv.lock · .python-version · .env.example · docker-compose.yml(postgres) · alembic.ini · .importlinter
├─ app/
│  ├─ main.py                 앱 조립 · 라우터 등록 · 예외 핸들러 · 속도 제한
│  ├─ core/                   settings · security(JWT 발급 · 검증 · current_user) · errors(코드 enum · 핸들러) · logging(마스킹)
│  ├─ db/                     engine · session(Depends) · Base
│  ├─ auth/                   router · service · schemas · models · repository
│  │  └─ pleos/               client(Login URL · Token · Refresh — api.pleos.ai 를 아는 유일한 파일 · base_url 은 설정) · webhook(동의철회 → 삭제)
│  ├─ me/ · consents/ · <도메인>/     도메인 패키지 = router · service · schemas · models · repository
│  ├─ llm/                    client(Protocol) · openai_compatible.py · prompts/*.md · schemas(출력) · service
│  └─ tools/export_openapi.py
├─ alembic/versions/          리비전 파일(커밋)
├─ tests/unit/ · tests/integration/
└─ pleos_auth_stub/           개발 · 시험용 모의 인증 서버(FastAPI 앱 · 로그인 폼 한 장 · 고정 사용자 UUID · 같은 JSON 꼴)
```

## 규칙 9

| 번호 | 규칙 | 재는 법 |
|---|---|---|
| 1 | `router → service → repository → db` 한 방향. router 모듈은 SQLAlchemy · models 를 import 하지 않음 · service 는 Request · Response 를 모름 | import-linter `layers` 계약 |
| 2 | 도메인 패키지는 서로 독립 · 다른 패키지는 service 공개 함수로만 · 순환 금지. `openai` SDK 는 `app.llm` 만 · Pleos 호출(httpx 로 api.pleos.ai)은 `app.auth.pleos` 만 | import-linter `independence` · `forbidden` 계약 |
| 3 | 요청 · 응답은 Pydantic 스키마(`schemas.py`) · 라우트마다 `response_model=` 또는 반환 타입 주석 · ORM 모델 반환 금지 · 칸마다 `Field(...)` 제약 | `check_routes.py` + 리뷰 |
| 4 | 내부 PK 는 시퀀스(`<단어>_no` · 정의서 R02) · **밖으로 나가는 식별자는 `_uuid`**(경로 · 응답 · 공유 링크 · 정의서 R08) — 응답 스키마에 `_no` 가 나오면 위반(연번은 남의 기록 수를 드러냄). user_id 는 `Depends(current_user)` 에서만 · 요청 본문 · 쿼리에 사용자 식별 칸 금지 | mypy + `check_api_contract.py`(계약에 userId 요청 칸 없음) + 통합 테스트(남의 id 로 404/403) |
| 5 | 비밀 · 설정은 `Settings`(환경 변수 · `.env`) 하나 · 코드 리터럴 금지 · `PLEOS_CLIENT_SECRET` 은 `auth/pleos/` 만 · LLM 키는 `llm/` 만 · `PLEOS_AUTH_BASE_URL` · `LLM_BASE_URL` 로 대역 교체 | `check_secrets.py` + import-linter |
| 6 | 로그는 structlog 마스킹 프로세서를 거침 · pleos_user_id · 토큰 · 이름 · LLM 프롬프트 · 응답 원문 금지 | 리뷰 + grep(`print(` · `logging.info(f"` 금지) |
| 7 | 스키마 변경 = Alembic 리비전 동반 · `alembic check` 통과 · `alembic heads` 1개 · 칸마다 `comment=` · `create_all` 금지 | `alembic check` · `check_alembic_heads.py` · `check_sql_comments.py` |
| 8 | 오류 코드는 `ErrorCode` enum(`UPPER_SNAKE`) 한 곳 · 예외 핸들러 하나 · 응답에 한글 문장 금지 · 프롬프트는 `prompts/*.md` 파일(코드 문자열 금지) | grep + 리뷰 |
| 9 | **데이터 표준 준수** — 테이블 · 컬럼 이름은 `docs/data/standard.md` 의 표준 단어 · 용어 사전에 있는 것만 · 테이블명 끝에 유형 단어(`_mst` · `_dtl` · `_hist` · `_log` · `_rel`) · 공통 컬럼(로그는 `created_at` · `created_user_no` 둘) · 사전에 없는 단어는 정의서에 먼저 등록. 정의서에서 **「결정」인 규칙만** 따르고 「제안 · 미결 · 가안」은 스펙의 「미확정」 표로 | `check_data_standard.py`(「결정」은 위반 · 「제안」은 경고) |

## 데이터

- 서버 PostgreSQL 이 **원본**. 여행 기록 · 경로 · 공유도 서버가 가지고, 앱 Room 은 캐시 · 전송 대기 큐(ADR 0015 · 0014 대체). 추천은 여러 사용자의 기록을 모아야 가능하므로 집계만으로는 근거가 모자람.
- 테이블 · 컬럼 이름과 코드값의 원본은 `docs/data/standard.md` · `docs/data/erd.md`(읽는 법 `docs/data/README.md`). **여기에 목록을 베끼지 않음** — 같은 목록이 두 곳이면 한 곳만 고쳐져 규칙과 센서가 어긋남. 새 테이블 · 칸은 그 기능 스펙의 「데이터」 절이 근거.
- 서버에 **없는** 것: 기어 · 속도 · VIN · Pleos 토큰 · 비밀번호. 스키마 · 계약에 나타나면 위반. 비밀번호가 없는 이유는 인증을 Pleos 계정이 하기 때문 — `user_mst` 에는 Pleos 아이디 값만 둠.
- **좌표**는 서버에 있음(원본 경로점 · 간소화 경로). 대신 조건이 붙음 — 위치 동의 뒤에만 수집(정의서 R14) · 보관 기간을 기능 스펙에 적음 · 철회 · 탈퇴 때 지워짐. 심사 「개인정보 · 보안」이 여기를 봄.
- 삭제는 둘로 나뉨: 사용자가 지우는 것(여행 · 장소 …)은 `deleted_yn` 논리 삭제 · **탈퇴와 Pleos 동의철회 웹훅은 하드 삭제**(FK `ON DELETE CASCADE`). 「즉시 삭제」 요구를 논리 삭제로 때우면 위반.
- 응답 코드 · 라벨: 서버는 `UPPER_SNAKE` 코드 → 앱이 한글 문구로.

## 인증 — Pleos 계정(`app/auth/pleos/`)

1. `POST /auth/pleos/start {brand}` → `state`(무작위 · 10분 · 1회 · `pleos_login_states`) → `loginUrl` = `{PLEOS_AUTH_BASE_URL}/v1/auth/login?clientId&brand&redirectUrl&state`.
2. `POST /auth/pleos/exchange {code, state}` → state 검증 · 소비 → `POST {base}/v1/auth/token`(json: clientId · clientSecret · code · redirectUrl · 헤더 `Brand`) → accessToken · refreshToken.
3. 사용자 식별 → `user_mst` upsert(Pleos 아이디 값을 `login_id` 로 보관 · 비밀번호는 받지도 저장하지도 않음 — 인증은 Pleos 가 함) → Pleos 토큰 폐기(저장 안 함) → 우리 토큰 한 벌 발급. 칸 이름은 `docs/data/standard.md` 가 원본.
   - 식별자 출처는 **미확인**(토큰 응답에 칸 없음 · 웹훅엔 계정 UUID 있음). 승인 뒤 실제 accessToken 이 JWT 인지(`sub`) 확인 → 결정은 ADR. 그 전엔 스텁의 고정 UUID.
4. `GET /auth/pleos/callback` — 「앱으로 돌아가세요」 정적 페이지(WebView 가로채기 실패 시).
5. `POST /auth/pleos/consent-withdrawn {userIds, vins}` — 알려진 `pleos_user_id` 만 즉시 삭제 · 속도 제한 · 서명 검증은 Pleos 답을 받으면 추가.
6. `redirectUrl` 은 신청서 값과 글자까지 같음(FAQ: Invalid Request 의 주원인) · 설정 객체에.
- 문서: Login https://document.pleos.ai/api-reference/vehicle-data-api/vehicle-data-api/for-business-users/login-api · Token https://document.pleos.ai/api-reference/vehicle-data-api/authentication-authorization/for-business-users/get-access-token · Refresh https://document.pleos.ai/api-reference/vehicle-data-api/authentication-authorization/for-business-users/get-access-token-refresh · 웹훅 https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/notify-data-sharing-consent-withdraw · 신청 https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/api-access-request

## LLM 경계 7(`app/llm/`)

| 규칙 | 내용 |
|---|---|
| 한 패키지 | 제공자 SDK · 키 · 프롬프트는 `app/llm/` 만 · 밖은 `LlmClient` Protocol |
| 입력에 개인 데이터 없음 | user_id · 닉네임 · 기록 원문 · 위치 금지 · 비식별 값(지역 이름 · 주제 · 연령대)만 — 외부 LLM 으로 개인정보가 가면 심사 「제3자 전송 · 동의」 대상 |
| 주행 경로 밖 | 정차 중 사용자 요청 · 운영자 도구만 · 주행 중 자동으로 도는 경로에 없음 |
| 출력 검증 | Pydantic 스키마로 파싱 · 실패 시 재시도 1회 → `LLM_OUTPUT_INVALID` |
| 한도 | 타임아웃 · 재시도 · 요청당 토큰 상한 · 일일 호출 상한 = `Settings` |
| 시험 | respx 대역 · 실제 호출 0 · 프롬프트 스냅샷 테스트 |
| 프롬프트 = 파일 | `prompts/<기능>.md` · 코드 문자열 금지 · 변경은 diff 로 보임 |

## Alembic(두 사람)

- 변경 → `uv run alembic revision --autogenerate -m "<slug>"` → 생성 파일 검토(autogenerate 가 놓치는 것: 서버 기본값 · 인덱스 이름) → `uv run alembic upgrade head` → `uv run alembic check`(차이 0).
- rebase 뒤 `uv run alembic heads` 가 둘이면 `uv run alembic merge -m "merge" <a> <b>` → 다시 `upgrade head` · `check`.
- 머지된 리비전은 수정 안 함(새 리비전). `Base.metadata.create_all` 은 테스트 밖에서 금지.

## OpenAPI(계약)

- `uv run python -m app.tools.export_openapi ../docs/api/openapi.json` → 커밋. 라우터 · 스키마가 바뀌면 같은 PR 에서 재생성. CI 가 `git diff --exit-code`.
- 앱 `data/remote/AppApi.kt` 는 이 파일과 경로 · 메서드 1:1(`check_api_contract.py`).

## 테스트

- 단위: service(fake repository) · schemas(제약) · llm(출력 파싱 · respx).
- 통합: testcontainers PostgreSQL + Alembic `upgrade head` → 라우트 왕복 · 남의 id 접근 거부 · 웹훅 삭제 · 로그인 전 흐름(respx 로 Pleos 토큰 API).
- 테스트 이름에 시험 케이스 번호. fake 우선 · 외부 호출 0.

## import-linter 초안(`.importlinter`)

```ini
[importlinter]
root_package = app

[importlinter:contract:layers]
name = router → service → repository → db
type = layers
layers =
    app.auth.router | app.me.router | app.consents.router | app.<도메인>.router | app.llm.router
    app.auth.service | app.me.service | app.consents.service | app.<도메인>.service | app.llm.service
    app.auth.repository | app.me.repository | app.consents.repository | app.<도메인>.repository
    app.db

[importlinter:contract:domains]
name = 도메인 패키지 독립
type = independence
modules = app.me app.consents app.<도메인> app.llm

[importlinter:contract:llm-sdk]
name = openai 는 app.llm 만
type = forbidden
source_modules = app.auth app.me app.consents app.<도메인> app.core app.db
forbidden_modules = openai
```

## 구조 리뷰 점검표(structure-reviewer · server)

1. 센서 결과(`04_sensors.md`) 먼저 — import-linter · ruff · mypy · alembic check 가 빨간 규칙은 「센서 실패 상태로 넘어옴」.
2. 바뀐 파일마다: 어느 패키지 · 어느 층인가 → import 가 층을 거슬러 오르나(규칙 1) · 다른 도메인 내부(models · repository)를 import 하나(규칙 2).
3. 새 라우트: `response_model` · 반환 타입 · 경로 파라미터와 응답에 `_uuid`(연번 `_no` 가 나가면 위반 · 규칙 4) · `Depends(current_user)` · 요청 스키마에 사용자 식별 칸.
4. 새 모델 · 스키마: VIN · 기어 · 속도 · 토큰 · 비밀번호 칸(위반) · 좌표는 동의 · 보관 기간 · 삭제 경로가 스펙에 있나 · `comment=` · 리비전 파일 동반.
5. 로그: `print(` · f-string 로그 · 마스킹 안 거친 사용자 값.
6. LLM: 프롬프트가 코드 문자열인가 · 입력에 개인 데이터 · 호출 위치가 주행 중 자동 경로인가.
7. 데이터 표준(규칙 9): 테이블명 끝 유형 단어 · 컬럼이 `docs/data/standard.md` 사전에 있나 · 공통 컬럼 · 삭제 방식(`deleted_yn` ↔ CASCADE)이 맞나. 센서가 「제안」으로 경고한 줄은 스펙의 「미확정」으로 갔는지 확인.
8. 표로: 파일:줄 · 규칙 번호 · 고칠 방향 · 린트로 만들 수 있나 · 반복 여부.

## 참고
FastAPI https://fastapi.tiangolo.com/tutorial/bigger-applications/ · 설정 https://fastapi.tiangolo.com/advanced/settings/ · OpenAPI https://fastapi.tiangolo.com/how-to/extending-openapi/ · uv https://docs.astral.sh/uv/ · SQLAlchemy async https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html · Alembic check https://alembic.sqlalchemy.org/en/latest/autogenerate.html#running-alembic-check-to-test-for-new-upgrade-operations · import-linter https://import-linter.readthedocs.io/ · PyJWT https://pyjwt.readthedocs.io/ · respx https://lundberg.github.io/respx/ · slowapi https://slowapi.readthedocs.io/ · testcontainers https://testcontainers-python.readthedocs.io/ · Ollama OpenAI 호환 https://docs.ollama.com/openai
