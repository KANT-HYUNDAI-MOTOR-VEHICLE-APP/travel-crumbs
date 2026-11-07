"""data_change_hist 데이터변경이력 — 값이 바뀌면 공통으로 남기는 이력(결정 로그 D17 · D26)."""

from sqlalchemy import BigInteger, Identity, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class DataChangeHist(CommonColumns, Base):
    __tablename__ = "data_change_hist"
    __table_args__ = table_args(
        "데이터변경이력 — 어느 테이블 · 컬럼 · 행의 값이 언제 누구에 의해 바뀌었는지(변경 일시 = created_at)"
    )

    data_change_hist_no: Mapped[int] = mapped_column(
        BigInteger,
        Identity(always=False),
        primary_key=True,
        comment="데이터변경이력번호",
    )
    table_nm: Mapped[str] = mapped_column(
        String(100), comment="테이블명 — 예: trip_mst"
    )
    column_nm: Mapped[str] = mapped_column(String(100), comment="컬럼명 — 예: trip_nm")
    row_no: Mapped[int] = mapped_column(
        BigInteger, comment="행번호 — 바뀐 행의 PK 번호"
    )
    before_value: Mapped[str | None] = mapped_column(String(500), comment="변경전값")
    after_value: Mapped[str | None] = mapped_column(String(500), comment="변경후값")
