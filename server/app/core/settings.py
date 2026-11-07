"""설정 한 곳 — 값은 server/.env 에서 읽음(.env.example 참고). 코드에 비밀번호를 쓰지 않음."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # 로컬 Docker PostgreSQL 접속 주소
    database_url: str

    # 로그인 대역 — Pleos 로그인(기보님) 전까지 "로그인된 사용자"로 쓸 샘플 계정의 login_id
    dev_login_id: str = "sample01"


settings = Settings()
