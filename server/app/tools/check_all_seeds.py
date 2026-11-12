"""팀 공통 PostgreSQL 샘플 데이터 전체 검증."""

import asyncio

from app.tools.check_navigation_seed import main as check_navigation
from app.tools.check_data_standard_v055 import main as check_data_standard_v055
from app.tools.check_demo_seed import main as check_demo
from app.tools.check_recommendation_seed import main as check_recommendation
from app.tools.check_share_prototype_seed import main as check_share_prototype


async def main() -> None:
    await check_data_standard_v055()
    await check_demo()
    await check_recommendation()
    await check_share_prototype()
    await check_navigation()


if __name__ == "__main__":
    asyncio.run(main())
