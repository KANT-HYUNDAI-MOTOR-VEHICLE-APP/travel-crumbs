"""user_consent_hist 사용자동의이력 — ERD v0.3 그대로. 이력이라 추가만 함(수정 · 삭제 없음)."""

from sqlalchemy import CHAR, BigInteger, CheckConstraint, ForeignKey, Identity, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class UserConsentHist(CommonColumns, Base):
    __tablename__ = "user_consent_hist"
    __table_args__ = table_args(
        "사용자동의이력 — 동의 · 철회 변경 기록(R14)",
        CheckConstraint("consent_yn IN ('Y', 'N')", name="consent_yn"),
    )

    user_consent_hist_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="사용자동의이력번호"
    )
    user_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("user_mst.user_no"), comment="사용자번호"
    )
    consent_type_cd: Mapped[str] = mapped_column(String(30), comment="동의유형코드 (CONSENT_TYPE)")
    consent_yn: Mapped[str] = mapped_column(CHAR(1), comment="동의여부 — Y 동의 / N 철회")
