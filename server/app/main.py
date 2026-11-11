"""서버 조립 — 실행: uv run uvicorn app.main:app --reload --port 4000"""

from fastapi import FastAPI

from app.codes.router import router as codes_router
from app.consents.router import router as consents_router
from app.places.router import router as places_router
from app.trips.router import router as trips_router
from app.users.router import router as users_router

app = FastAPI(title="Travel Crumbs 로컬 서버")
app.include_router(users_router)
app.include_router(consents_router)
app.include_router(codes_router)
app.include_router(trips_router)
app.include_router(places_router)
