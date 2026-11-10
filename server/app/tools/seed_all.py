"""팀 공통 PostgreSQL 샘플 데이터를 정해진 순서로 모두 적재."""

import asyncio

from app.tools.seed_demo import main as seed_demo
from app.tools.seed_dev import main as seed_dev
from app.tools.seed_navigation import main as seed_navigation
from app.tools.seed_recommendation import main as seed_recommendation
from app.tools.seed_share_prototype import main as seed_share_prototype


async def main() -> None:
    await seed_dev()
    await seed_demo()
    await seed_recommendation()
    await seed_share_prototype()
    await seed_navigation()


if __name__ == "__main__":
    asyncio.run(main())
