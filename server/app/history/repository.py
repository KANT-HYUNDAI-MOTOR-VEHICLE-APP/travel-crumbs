"""data_change_hist 쓰기 — SQL 은 이 파일에만."""

from sqlalchemy.ext.asyncio import AsyncSession

from app.history.models import DataChangeHist


def _text(value: object) -> str | None:
    return None if value is None else str(value)[:500]


async def add_change(
    session: AsyncSession,
    user_no: int,
    *,
    table_nm: str,
    column_nm: str,
    row_no: int,
    before: object,
    after: object,
) -> None:
    """값이 실제로 바뀌었을 때만 1행."""
    if before == after:
        return
    session.add(
        DataChangeHist(
            table_nm=table_nm,
            column_nm=column_nm,
            row_no=row_no,
            before_value=_text(before),
            after_value=_text(after),
            created_user_no=user_no,
        )
    )
    await session.flush()
