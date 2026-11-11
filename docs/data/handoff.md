# 인계서 — 데이터 CRUD(시나리오 1)

작성: 2026-11-08

**사람 · AI 모두 이 문서부터 읽는다.** 순서대로 따라 하면 DB 를 띄우고, 디자인 시스템이 입혀진 화면을 직접 눌러 보고, Gleo 가 들어갈 자리를 찾을 수 있다.

목차
1. 브랜치 만들기
2. DB · 서버 설치 (처음 한 번)
3. 시나리오 1 화면 (작업 기준)
4. 앱 실행 · 화면 둘러보기 (디자인 시스템 적용)
5. 시나리오 1 지도 — 화면 → API → 테이블
6. Gleo AI 참고 사항
7. 파일 범위 · DB 변경
8. 기준 문서
9. 알려진 한계 · 문제 해결

---

## 1. 브랜치 만들기

`data-crud/main` 은 아직 develop 에 병합되지 않았다(팀 리드가 월요일 병합 예정). Gleo 작업은 여기서 갈라져 나온다.

```
git fetch origin
git switch -c gleo-ai/main origin/data-crud/main
git push -u origin gleo-ai/main
```

1. Gleo AI 입출력 작업과 push 는 `gleo-ai/main` 에서 진행한다.
2. DB 를 변경한 경우 7장 절차(표준 준수 · 변경 기록)를 따른다.
3. 작업이 끝나면 데이터 담당에게 확인을 요청한다. DB 변경과 신규 단어 기록도 확인 대상이다.
4. 데이터 담당이 확인한 뒤 팀 리드에게 develop 병합을 요청한다. **develop 으로 직접 PR 을 열거나 병합하지 않는다.**
5. 팀 리드가 `data-crud/main` 을 develop 에 먼저 병합한 경우, `git merge origin/develop` 으로 `gleo-ai/main` 을 최신화한다.
6. 데이터 담당은 이 문서 push 이후 `data-crud/main` 을 수정하지 않는다.

## 2. DB · 서버 설치 (처음 한 번 · 약 10분)

DB 는 클라우드 없이 **각자 PC 의 Docker** 로 띄운다. 아래 명령은 Windows PowerShell 기준(저장소 맨 위에서 시작).

### 2-1. 준비물

| 도구 | 설치 | 확인 명령 |
|---|---|---|
| Docker Desktop | https://www.docker.com/products/docker-desktop/ 설치 후 실행해 둠 | `docker --version` |
| uv (파이썬 실행 도구) | `powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 \| iex"` → 터미널 다시 열기 | `uv --version` |
| Python 3.12 이상 | uv 가 알아서 받음(따로 설치 안 해도 됨) | — |

### 2-2. 설치

```
cd server
copy .env.example .env                  # DB 주소 · 로그인 대역 계정(sample01). .env 는 git 에 안 올라감
docker compose up -d db                 # PostgreSQL 17 컨테이너 travel-crumbs-db (사용자 travel / 비밀번호 travel)
uv sync                                 # 서버 패키지 설치
uv run alembic upgrade head             # 테이블 14개 + 공통 코드 · 장소 유형 · 경험 태그
uv run python -m app.tools.seed_dev     # 샘플 사용자 sample01(지현) · sample02(준호)
uv run python -m app.tools.seed_demo    # 시연 데이터 — 완료된 여행 2건(아이와 강릉 1박 · 정동진 혼자 드라이브) · 장소 · 경로
```

### 2-3. 잘 됐는지 확인

```
docker exec -it travel-crumbs-db psql -U travel -d travel_crumbs -c "SELECT trip_nm, trip_status_cd, place_cnt FROM trip_mst ORDER BY trip_no;"
```

여행 2건(COMPLETED)이 나오면 성공.

### 2-4. 서버 켜기 (앱을 쓸 때마다)

```
cd server
uv run uvicorn app.main:app --reload --port 4000
```

- API 문서(Swagger): http://localhost:4000/docs — 여기서 API 를 직접 눌러 볼 수 있다
- 앱(에뮬레이터)은 `http://10.0.2.2:4000/` 으로 붙는다(`app/build.gradle.kts` 의 `API_BASE_URL`)
- 로그인은 아직 Pleos 연동 전이라 `.env` 의 `DEV_LOGIN_ID`(sample01 = 지현) 로 로그인된 것으로 친다

### 2-5. 자주 쓰는 것

| 하고 싶은 것 | 명령 |
|---|---|
| DB 직접 보기 | `docker exec -it travel-crumbs-db psql -U travel -d travel_crumbs` |
| 기록 중 여행이 남아 새 여행이 안 만들어짐 | `server/README.md` 「여행 시작을 다시 테스트하려면」 SQL |
| 테이블 추가 · 변경 | `server/README.md` 「테이블 추가 · 변경」 — Alembic 리비전으로만. 절차는 7장 |
| DB 를 처음 상태로 | `docker compose down -v` → 2-2 를 다시(데이터 전부 지워짐 · 조심) |

## 3. 시나리오 1 화면 (작업 기준)

Gleo AI 작업은 아래 화면 흐름을 기준으로 한다. 기존 와이어프레임은 구현 과정에서 많이 바뀌어 참고하지 않는다.

- 범위: 로그인 → 여행 시작 → 기록 → 장소 경험 입력 → 여행 종료 · 상세 (공유 제외)
- 데모 사용자: 지현(sample01 · 아이와 여행하는 부모) · 준호(sample02 · 혼자 드라이브)
- 레일 메뉴: 여행 기록 · 여행 목록 · 찍힌 장소(준비 중) · 설정(준비 중)

| 화면 | 목적 | 보이는 것 | 사용자 행동 | 저장 데이터 | 규칙 |
|---|---|---|---|---|---|
| A-02 로그인 | 사용자 확인 | (화면 없음) | — | — | Pleos 로그인은 다른 팀원 담당. 현재는 `.env` 의 샘플 계정으로 로그인된 것으로 처리 |
| A-03 홈(레일 「여행 기록」) | 현재 상태와 다음 행동 안내 | 인사말 · 경로 그림 · 여행 상태 카드 · 주요 버튼 · 최근 여행 카드 | 새 여행 시작 · 장소 기록 · 장소 경험 입력 · 여행 종료 · 최근 여행 열기 | 조회만 | 경로 그림은 기록 중이면 현재 여행, 아니면 최근 완료 여행. 지도 대신 점 · 점선 |
| A-03-1 위치정보 동의 | 위치 수집 동의 | 동의 창 | 「동의하고 시작」 · 「동의 안 함」 | `user_consent_hist`(LOCATION_COLLECT) | 첫 여행 시작 때 1회 |
| A-04 여행 시작 설정 | 여행 만들기 | 여행명 · 「수정」 · 동행자 칩 · 「여행 시작」 | 여행명 확인 · 수정 · 동행자 선택 · 시작 | `trip_mst` · `trip_companion_dtl` · `trip_status_hist` | 여행명 기본값은 날짜만 · 「수정」은 Gleo 활용 아이디어 G1 참고 · 동행자 1개 이상 · 혼자는 단독 선택 · 기록 중 여행은 1건 |
| A-05 기록 중 | 이동 경로 자동 저장 | 여행 상태 카드(기록 중 · 경과) | 운전 | `trip_route_point_dtl` | 5초마다 위치 확인 · 20m 이상 이동 시 저장 |
| A-05 장소 기록 | 지금 위치를 장소로 남김 | 「이 장소 기록」 | 버튼 한 번(주행 중 가능) | `trip_place_dtl`(임시 · draft_yn = Y) · `trip_mst.place_cnt` | 주행 중에는 위치만 저장 · 이름 · 후기는 정차 후 입력 |
| A-05 일시정지 | 기록 잠시 멈춤 | 주행 화면 「기록 일시정지」 · 「기록 재개」 | 일시정지 · 재개 | `trip_mst.trip_status_cd` · `trip_status_hist` | 일시정지 중 경로 저장 안 함 · 장소 기록은 가능 · 기록 시간에서 제외 |
| 주행 화면 | 주행 중 핵심 행동만 | 여행 상태 · 큰 버튼 3개(장소 기록 · 음성 입력 · 일시정지) | 버튼 | 위와 같음 | 기어가 P 가 아닐 때만 표시 · 「음성 입력」은 Gleo 활용 아이디어 G3 참고 |
| A-06 장소 경험 입력 | 임시 기록 완성 | 장소명(내비 후보 · 직접 입력) · 장소 유형(카테고리 → 유형) · 좋았어요/별로예요 · 경험 태그 · 메모 | 입력 후 「저장」 또는 「나중에 입력」 | `trip_place_dtl`(draft_yn = N) · `trip_place_tag_rel` · `place_mst` | 정차 중에만 · 내비 후보는 기록 위치 300m 안 · 후보를 고르면 공식 장소(`place_mst`)에 연결 |
| A-08 여행 종료 | 여행 마치기 | 「여행 종료」 → 확인 창 | 「종료」 · 「계속 기록」 | `trip_mst`(COMPLETED · 거리 · 기록 시간) · `trip_route_dtl` · `trip_status_hist` | 남은 경로점 전송 후 종료 · 입력 안 한 임시 기록은 남김 |
| A-08 여행 상세 | 여행 돌아보기 | 여행명 · 요약 · 경로 그림 · 장소 경험 카드 | 「제목 수정」 · 「닫기」 | `trip_mst.trip_nm` · `data_change_hist` | 「제목 수정」은 Gleo 활용 아이디어 G2 참고 · 제목 변경은 변경 이력에 기록 |
| A-07 여행 목록 | 지난 여행 찾기 | 여행 카드(여행명 · 날짜 · 거리 · 장소 수) | 카드 → 상세 | 조회만 | 취소 여행 제외 · 최신순 |

## 4. 앱 실행 · 화면 둘러보기 (디자인 시스템 적용)

### 4-1. 실행

1. 2-4 로 서버를 켠다
2. Android Studio 에서 저장소를 열고 Pleos 에뮬레이터(landscape)를 고른 뒤 ▶ Run
3. 처음 실행 시 위치정보 동의 창이 뜬다 → 「동의하고 시작」

### 4-2. 디자인 시스템이 어디 있나

- 지시서 원문: `docs/design/design-system.md`
- 앱 반영:
  - 색 · 글자 · 간격: `ui/theme/Color.kt` · `Type.kt`(서체 Asta Sans · `res/font/asta_sans.ttf`) · `Dimens.kt` · `Theme.kt`
  - 공통 컴포넌트: `ui/components/` — `TcButton`(Primary · Secondary · Danger) · `TcChip`(선택 칩) · `TcCard.kt`(여행 상태 카드 · 여행 카드 · 장소 경험 카드) · `CrumbRoute`(경로 그림)
  - 레일: `ui/shell/components/AppNavigationRail.kt` — 여행 기록 · 여행 목록 · 찍힌 장소 · 설정
- 지시서 px 는 dp · sp 로 1:1 옮겼다. 화면 비율은 레일 12 · 지도 자리 43 · 정보 45

### 4-3. 화면 둘러보기 순서 (시연 흐름과 같음)

| 순서 | 어디서 | 무엇을 | 보이는 것 |
|---|---|---|---|
| 1 | 레일 「여행 기록」(홈) | 처음 화면 | 왼쪽: 최근 여행(데모) 경로 그림 · 오른쪽: 인사말 · 여행 상태 카드 · 「새 여행 시작」(Primary) · 최근 여행 카드 2개 |
| 2 | 홈 | 최근 여행 카드 누르기 | 여행 상세(A-08) — 경로 그림 + 장소 경험 카드 타임라인 · 「제목 수정」 |
| 3 | 홈 | 「새 여행 시작」 | A-04 — 여행명(날짜 기본값) · 「수정」(Gleo 활용 아이디어 G1 참고) · 동행자 칩 · 「여행 시작」 |
| 4 | 홈 | 「이 장소 기록」(Primary) 몇 번 | 장소 수가 늘고 「장소 경험 입력 · 남은 기록 N」 버튼이 생김 · 경로 그림에 장소 핀 |
| 5 | 홈 | 「장소 경험 입력」 | A-06 — 내비 후보 · 장소명 · 장소 유형(카테고리 → 유형) · 좋았어요/별로예요 · 경험 태그 · 메모 |
| 6 | 홈 | 「여행 종료」(Danger) → 「종료」 | A-08 여행 상세 — 거리 · 시간 · 장소 수 · 경로 그림 · 「제목 수정」(Gleo 활용 아이디어 G2 참고) |
| 7 | 레일 「여행 목록」 | 목록 | 여행 카드 — 누르면 상세 |
| 8 | 레일 「찍힌 장소」 · 「설정」 | — | 「준비 중」 |

- 주행 화면(큰 버튼 3개 · 「음성 입력」 · 「기록 일시정지」)은 기어가 P 가 아닐 때만 뜬다. 지금 에뮬레이터는 일반 빌드라 기어를 바꿀 수 없다(9장).
- 경로 그림은 지도 대신 좌표를 점 · 점선으로 그린 것(지도는 나중에). 출발 = 베이지 링 · 장소 = 베이지 핀(입력 안 한 장소 = 보라 링) · 지금 위치/도착 = 주황 큰 핀.

## 5. 시나리오 1 지도 — 화면 → API → 테이블

| 화면 | 앱 코드(ui/) | API | 테이블 |
|---|---|---|---|
| A-03 홈 · 인사말 · 경로 그림 · 최근 여행 | `home/` | `GET /me` · `GET /trips/current` · `GET /trips` · `GET /trips/{uuid}/route` | user_mst · trip_mst · trip_route_point_dtl · trip_place_dtl |
| A-03-1 위치정보 동의 | `home/components/LocationConsentDialog` | `GET /consents/latest` · `POST /consents` | user_consent_hist |
| A-04 여행 시작 설정 | `tripsetup/` | `GET /codes/COMPANION_TYPE` · `POST /trips` | trip_mst · trip_companion_dtl · trip_status_hist |
| A-05 기록 중(경로 자동 저장) | `data/trip/RouteRecorder` | `POST /trips/{uuid}/route-points` | trip_route_point_dtl |
| A-05 「이 장소 기록」 | `home/` · `driving/` | `POST /trips/{uuid}/places` | trip_place_dtl(draft_yn = Y) · trip_mst.place_cnt |
| A-05 일시정지 · 재개 | `driving/` · `data/trip/TripPauseSync` | `POST /trips/{uuid}/pause` · `/resume` | trip_mst · trip_status_hist |
| A-06 장소 경험 입력 | `placeinput/` | `GET /trips/{uuid}/places?draft_yn=Y` · `GET /place-types` · `GET /tags` · `GET /codes/REVIEW_POLARITY` · `PUT /trips/{uuid}/places/{no}` | trip_place_dtl · trip_place_tag_rel · place_mst |
| A-08 여행 종료 · 상세 · 제목 수정 | `home/` · `tripdetail/` | `POST /trips/{uuid}/end` · `GET /trips/{uuid}` · `PUT /trips/{uuid}` | trip_mst · trip_route_dtl · data_change_hist |
| A-07 여행 목록(간단판) | `trips/` | `GET /trips` | trip_mst |

- 앱 코드 경로는 `app/src/main/java/com/example/travelcrumbs/` 아래. 서버는 `server/app/<도메인>/`(models · repository · service · router).
- 저장 규칙(왜 이렇게 저장하는지)은 `docs/data/data-decision-log.md` D01–D27.

## 6. Gleo AI 참고 사항

데이터 CRUD 작업 중 검토한 Gleo AI 관련 내용(참고용): `docs/data/gleo-handoff.md`

- 코드 위치 예: 여행명 「수정」(A-04) · 「제목 수정」(A-08)은 `TripNameSuggester`, 주행 화면 「음성 입력」은 `StartVoiceInputUseCase` 를 호출한다(현재는 임시 구현).

## 7. 파일 범위 · DB 변경

- **고쳐도 되는 곳:** `data/gleo/` · 새로 만드는 Gleo 구현 클래스 · `di/TripModule.kt` · `di/GleoModule.kt` 바인딩 · `domain/` 의 Gleo 인터페이스 · `assets/schema.json`
- **되도록 피할 곳:** `ui/home/HomeViewModel.kt` · `ui/tripsetup/TripSetupViewModel.kt` · `ui/tripdetail/TripDetailViewModel.kt` · `data/trip/TripRepository.kt` · `server/app/trips/` · `ui/theme/`
- **DB 변경:** 필요한 테이블 · 컬럼 · 코드는 직접 추가 · 변경할 수 있다.
  1. `데이터표준정의서_v0.4.md` 의 규칙(명명 · 단어 · 도메인 · 코드)을 먼저 따른다.
  2. 표준 단어 사전에 없는 단어는 쓰지 않는다. 필요하면 R06-1 에 따라 단어를 먼저 등록한다.
  3. 변경은 Alembic 리비전으로 한다(`server/README.md` 「테이블 추가 · 변경」).
  4. 변경 내용(테이블 · 컬럼 · 코드 · 신규 단어)과 이유를 `docs/data/data-decision-log.md` 에 「검토 대기」로 기록한다.
  5. 최종 검토는 데이터 담당이 하며, 검토 후 표준정의서와 변경이력에 반영한다.
- 팀 하네스 규칙(`CLAUDE.md` · `.claude/skills/`)을 따른다 — 예: `ai.pleos` import 는 `data.navi` · `data.vehicle` 에만, Hilt 모듈은 `di/` 에만, ViewModel 은 Context 금지(`ArchitectureTest`)

## 8. 기준 문서

| 문서 | 내용 |
|---|---|
| `데이터표준정의서_v0.4.md`(저장소 맨 위) | 데이터 표준 — 명명 규칙 · 단어 · 도메인 · 용어 · 코드 |
| `데이터표준정의서_변경이력.md`(저장소 맨 위) | 표준정의서 버전별 변경 내용 |
| `docs/data/data-decision-log.md` | 구현 중 결정 D01–D27(왜 그렇게 했는지) |
| `docs/data/gleo-handoff.md` | Gleo AI 참고 사항 |
| `docs/design/design-system.md` | 디자인 시스템 지시서 |
| `server/README.md` | 서버 실행 · 테이블 변경 |
| `CLAUDE.md` · `.claude/skills/` | 팀 하네스 규칙 |

## 9. 알려진 한계 · 문제 해결

| 증상 | 원인 · 해결 |
|---|---|
| 홈에 「사용자 정보를 불러오지 못했어요」 | 서버가 꺼져 있음 → 2-4 |
| 「이미 기록 중인 여행이 있어요」 | 기록 중 여행이 남음 → 끝내거나 2-5 SQL |
| `docker compose up` 실패 | Docker Desktop 실행 확인 · 5432 포트를 다른 PostgreSQL 이 쓰는지 확인 |
| 주행 화면을 못 띄움 | 에뮬레이터가 일반 빌드라 기어 변경 불가. 일시정지 연결은 `TripPauseSyncTest` 로 확인 |
| 내비 위치가 성남 근처로 고정 | 에뮬레이터 내비 기본값(Extended Controls 위치와 무관) |
| 지도 없음 | 경로는 점 · 점선 그림(`ui/components/CrumbRoute.kt`). 지도는 나중에 |
| CI 서버 잡(ruff · mypy · pytest · openapi) 실패 | 팀 리드가 하네스에서 정리 예정 |
| 화면 · 테스트가 깨져 보임 | 디자인 시스템 적용분(2026-11-08)은 빌드만 확인하였고 단위 테스트와 화면 전체 확인은 하지 않았다. Gleo 작업 전에 `.\gradlew testDebugUnitTest` 로 기준 상태를 먼저 확인한다 |
