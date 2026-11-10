"""user_mst 사용자마스터 — ERD v0.3 그대로."""

from sqlalchemy import BigInteger, Identity, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class UserMst(CommonColumns, Base):
    __tablename__ = "user_mst"
    __table_args__ = table_args("사용자마스터 — 자연인 한 사람의 정보(1인 1행)")

    user_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="사용자번호"
    )
    login_id: Mapped[str] = mapped_column(String(50), unique=True, comment="로그인아이디")
    password_hash: Mapped[str] = mapped_column(String(255), comment="비밀번호해시 — 해시만 저장")
    nickname_nm: Mapped[str] = mapped_column(String(100), comment="닉네임명")
