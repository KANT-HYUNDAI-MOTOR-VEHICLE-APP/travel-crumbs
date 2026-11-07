# Travel Crumbs 로컬 서버

내 PC에서만 돌아가는 개발용 서버(클라우드 아님). 차량 앱(에뮬레이터) → 이 서버(포트 4000) → Docker PostgreSQL(포트 5432).

## 처음 한 번

```
cd server
copy .env.example .env          # 맥: cp .env.example .env
docker compose up -d db         # PostgreSQL 17 띄우기
uv sync                         # Python 도구 설치
uv run alembic upgrade head     # 테이블 만들기
uv run python -m app.tools.seed_dev   # 샘플 사용자 넣기
```

## 앱을 테스트할 때마다

```
cd server
uv run uvicorn app.main:app --reload --port 4000
```

브라우저에서 http://localhost:4000/docs 를 열면 API 목록을 볼 수 있음.

## 테이블 추가 · 변경

1. `app/<도메인>/models.py` 에 테이블 모델 작성(ERD v0.3 · 표준정의서 v0.35)
2. `uv run alembic revision --autogenerate -m "<테이블명>"` → 생성된 파일 검토
3. `uv run alembic upgrade head`

## 여행 시작(A-04)을 다시 테스트하려면

기록 중인 여행이 있으면 새 여행을 시작할 수 없음(여행 종료 화면 A-08 은 시나리오 1 묶음 6).
그 전까지는 아래 명령으로 기록 중인 여행을 「취소」 상태로 바꿈 — 상태 이력도 함께 남김.

```
docker exec -i travel-crumbs-db psql -U travel -d travel_crumbs -c "WITH t AS (UPDATE trip_mst SET trip_status_cd = 'CANCELED', end_at = now(), updated_at = now(), updated_user_no = user_no WHERE trip_status_cd = 'RECORDING' AND deleted_yn = 'N' RETURNING trip_no, user_no) INSERT INTO trip_status_hist (trip_no, trip_status_cd, created_user_no) SELECT trip_no, 'CANCELED', user_no FROM t"
```
