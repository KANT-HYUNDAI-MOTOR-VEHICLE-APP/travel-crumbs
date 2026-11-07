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
