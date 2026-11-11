"""개발용 기초 데이터 — 실행: uv run python -m app.tools.seed_dev  (여러 번 실행해도 같은 결과)

- 시스템 계정(user_no 1): created_user_no 가 NOT NULL(ERD v0.3)이라, 시드 · 자동 생성 데이터의 "만든 사람"으로 씀
- 샘플 사용자 2명: 로그인 대역(.env 의 DEV_LOGIN_ID)으로 쓰는 계정
- password_hash '!' = 비밀번호로는 로그인할 수 없는 계정 표시(평문 아님)
"""

import asyncio

from sqlalchemy import text

from app.db.session import SessionLocal

SEED_SQL = [
    """
    INSERT INTO user_mst (user_no, login_id, password_hash, nickname_nm, created_user_no)
    OVERRIDING SYSTEM VALUE
    VALUES (1, 'system', '!', '시스템', 1)
    ON CONFLICT (login_id) DO NOTHING
    """,
    "SELECT setval(pg_get_serial_sequence('user_mst', 'user_no'), "
    "GREATEST((SELECT max(user_no) FROM user_mst), 1))",
    """
    INSERT INTO user_mst (login_id, password_hash, nickname_nm, created_user_no) VALUES
      ('sample01', '!', '샘플여행자1', 1),
      ('sample02', '!', '샘플여행자2', 1)
    ON CONFLICT (login_id) DO NOTHING
    """,
]


async def main() -> None:
    async with SessionLocal() as session:
        for sql in SEED_SQL:
            await session.execute(text(sql))
        await session.commit()
        rows = (await session.execute(text(
            "SELECT user_no, login_id, nickname_nm FROM user_mst ORDER BY user_no"
        ))).all()
    for row in rows:
        print(f"user_no={row.user_no}  login_id={row.login_id}  nickname_nm={row.nickname_nm}")


if __name__ == "__main__":
    asyncio.run(main())
