"""데이터 표준 v0.55 기준 코드그룹과 경험 태그 검증."""

import asyncio

from sqlalchemy import text

from app.db.session import SessionLocal

EXPECTED_GROUP_COUNTS = {
    "COMPANION_TYPE_CD": 6,
    "CONSENT_TYPE_CD": 2,
    "DATA_SRC_CD": 3,
    "REVIEW_POLARITY_CD": 2,
    "TRIP_STATUS_CD": 5,
    "VISIBILITY_CD": 3,
    "SHARE_STATUS_CD": 2,
    "NAVI_REQ_STATUS_CD": 3,
    "RECO_TYPE_CD": 4,
}

LEGACY_GROUPS = {
    "COMPANION_TYPE",
    "CONSENT_TYPE",
    "DATA_SRC",
    "REVIEW_POLARITY",
    "TRIP_STATUS",
    "VISIBILITY",
    "NAVI_REQ_STATUS",
}


async def main() -> None:
    async with SessionLocal() as session:
        groups = dict(
            (
                await session.execute(
                    text(
                        "SELECT cd_grp_nm, count(*) FROM cd_mst "
                        "WHERE deleted_yn = 'N' GROUP BY cd_grp_nm"
                    )
                )
            ).all()
        )
        tag_count = (
            await session.execute(
                text(
                    "SELECT count(*) FROM tag_mst "
                    "WHERE tag_no BETWEEN 1 AND 13 AND use_yn = 'Y' AND deleted_yn = 'N'"
                )
            )
        ).scalar_one()

    actual = {group: groups.get(group, 0) for group in EXPECTED_GROUP_COUNTS}
    if actual != EXPECTED_GROUP_COUNTS:
        raise SystemExit(
            f"v0.55 코드그룹 불일치: actual={actual}, expected={EXPECTED_GROUP_COUNTS}"
        )
    remaining = sorted(LEGACY_GROUPS.intersection(groups))
    if remaining:
        raise SystemExit(f"이전 코드그룹명이 남아 있음: {remaining}")
    if tag_count != 13:
        raise SystemExit(f"v0.55 경험 태그 불일치: actual={tag_count}, expected=13")
    print("데이터 표준 v0.55 확인 완료: 코드그룹 9 · 경험 태그 13")


if __name__ == "__main__":
    asyncio.run(main())
