"""Real HTTP and PostgreSQL checks for the #25 place replay contract."""

import asyncio
import json
import os
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4

from sqlalchemy import func, select, text, update
from sqlalchemy.engine import make_url
from uvicorn import Config, Server


@unittest.skipUnless(
    os.getenv("GLEO_IDEMPOTENCY_TEST_DATABASE_URL"), "dedicated PostgreSQL required"
)
class PlaceIdempotencyTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        db_url = os.environ["GLEO_IDEMPOTENCY_TEST_DATABASE_URL"]
        url = make_url(db_url)
        self.assertEqual(url.drivername, "postgresql+asyncpg")
        self.assertIn(url.host, {"127.0.0.1", "localhost"})
        self.assertEqual(url.database, "gleo_idem_test")
        self.enterContext(patch.dict(os.environ, {"DATABASE_URL": db_url}))

        from app.core.settings import settings
        from app.db.session import SessionLocal, engine
        from app.main import app
        from app.users.models import UserMst

        self.assertEqual(engine.url, url)
        self.addAsyncCleanup(engine.dispose)
        self.sessions = SessionLocal
        self.settings = settings
        self.owner = f"test_{uuid4().hex}"
        self.other = f"test_{uuid4().hex}"
        self.enterContext(patch.object(settings, "dev_login_id", self.owner))
        async with self.sessions() as session:
            self.assertEqual(
                await session.scalar(text("SELECT current_database()")), url.database
            )
            session.add_all(
                UserMst(
                    login_id=login,
                    password_hash="!",
                    nickname_nm="Gleo test",
                    created_user_no=1,
                )
                for login in (self.owner, self.other)
            )
            await session.commit()

        self.server = Server(Config(app, host="127.0.0.1", port=0, log_level="warning"))
        self.server_task = asyncio.create_task(self.server.serve())
        self.addAsyncCleanup(self.stop_server)
        async with asyncio.timeout(5):
            while not self.server.started:
                await asyncio.sleep(0.01)
        port = self.server.servers[0].sockets[0].getsockname()[1]
        self.base_url = f"http://127.0.0.1:{port}"

    async def stop_server(self):
        self.server.should_exit = True
        await self.server_task

    async def request(self, method, path, status, body=None, key=None):
        def fetch():
            headers = {"Content-Type": "application/json"}
            if key is not None:
                headers["Idempotency-Key"] = key
            data = json.dumps(body).encode() if body is not None else None
            req = Request(
                self.base_url + path, data=data, headers=headers, method=method
            )
            try:
                with urlopen(req, timeout=15) as response:
                    return response.status, json.load(response)
            except HTTPError as error:
                with error:
                    return error.code, json.load(error)

        code, result = await asyncio.to_thread(fetch)
        self.assertEqual(code, status)
        return result

    async def new_trip(self):
        return await self.request(
            "POST",
            "/trips",
            201,
            {"trip_nm": "Gleo test", "companion_type_cds": ["SOLO"]},
        )

    async def test_place_replay_contract(self):
        from app.trips.models import TripMst, TripPlaceDtl

        schema = await self.request("GET", "/openapi.json", 200)
        post = schema["paths"]["/trips/{trip_uuid}/places"]["post"]
        header = next(
            param for param in post["parameters"] if param["name"] == "Idempotency-Key"
        )
        self.assertEqual((header["in"], header["required"]), ("header", False))
        self.assertIn({"type": "string", "format": "uuid"}, header["schema"]["anyOf"])
        self.assertIn(
            "IDEMPOTENCY_KEY_CONFLICT", post["responses"]["409"]["description"]
        )
        self.assertIn("422", post["responses"])

        trip = await self.new_trip()
        trip_path = f"/trips/{trip['trip_uuid']}"
        path = f"{trip_path}/places"
        key = str(uuid4())
        body = {"lat": 37.12345671, "lng": 127.0, "recorded_at": "2026-11-10T01:00:00Z"}
        invalid = await self.request("POST", path, 422, body, "bad-uuid")
        self.assertEqual(invalid["detail"][0]["loc"], ["header", "Idempotency-Key"])
        await self.request("POST", f"{trip_path}/pause", 200)

        async with self.sessions() as blocker:
            await blocker.execute(
                select(TripMst.trip_no)
                .where(TripMst.trip_no == trip["trip_no"])
                .with_for_update()
            )
            both_posts = asyncio.gather(
                *(self.request("POST", path, 201, body, key) for _ in range(2))
            )
            async with asyncio.timeout(10):
                while True:
                    async with self.sessions() as observer:
                        waiting = await observer.scalar(
                            text("""
                            SELECT count(*) FROM pg_stat_activity
                            WHERE datname = current_database()
                              AND wait_event_type = 'Lock'
                              AND query LIKE '%FOR NO KEY UPDATE%'
                            """)
                        )
                    if waiting == 2:
                        break
                    await asyncio.sleep(0.02)
        first, replay = await both_posts
        self.assertEqual(first, replay)
        self.assertEqual(first["place_cnt"], 1)
        self.assertEqual(first["lat"], 37.1234567)

        equivalent = dict(body, recorded_at="2026-11-10T10:00:00+09:00")
        self.assertEqual(
            await self.request("POST", path, 201, equivalent, key.upper()), first
        )
        for changed in (
            dict(body, lat=37.12345672),
            dict(body, lng=127.00000001),
            dict(body, recorded_at="2026-11-10T01:00:00.000001Z"),
        ):
            self.assertEqual(
                await self.request("POST", path, 409, changed, key),
                {"detail": "IDEMPOTENCY_KEY_CONFLICT"},
            )

        distinct = await self.request("POST", path, 201, body, str(uuid4()))
        manual, other_manual = [
            await self.request("POST", path, 201, body) for _ in range(2)
        ]
        self.assertEqual(
            {r["place_cnt"] for r in (first, distinct, manual, other_manual)},
            {1, 2, 3, 4},
        )
        self.assertEqual(
            len({r["trip_place_no"] for r in (first, distinct, manual, other_manual)}),
            4,
        )

        with patch.object(self.settings, "dev_login_id", self.other):
            self.assertEqual(
                await self.request("POST", path, 404, body, key),
                {"detail": "TRIP_NOT_FOUND"},
            )
            other_trip = await self.new_trip()
            other_path = f"/trips/{other_trip['trip_uuid']}/places"
            self.assertEqual(
                (await self.request("POST", other_path, 201, body, key))["place_cnt"], 1
            )

        await self.request("POST", f"{trip_path}/end", 200)
        place_no = first["trip_place_no"]
        await self.request(
            "PUT", f"{path}/{place_no}", 200, {"user_place_nm": "Edited"}
        )
        self.assertEqual(
            await self.request("POST", path, 201, body, key),
            dict(first, draft_yn="N", place_cnt=4),
        )
        self.assertEqual(
            await self.request("POST", path, 409, body, str(uuid4())),
            {"detail": "TRIP_NOT_RECORDING"},
        )
        next_trip = await self.new_trip()
        next_path = f"/trips/{next_trip['trip_uuid']}/places"
        self.assertEqual(
            (await self.request("POST", next_path, 201, body, key))["place_cnt"], 1
        )

        async with self.sessions() as session:
            row_count = await session.scalar(
                select(func.count())
                .select_from(TripPlaceDtl)
                .where(TripPlaceDtl.trip_no == trip["trip_no"])
            )
            self.assertEqual(row_count, 4)
            await session.execute(
                update(TripPlaceDtl)
                .where(TripPlaceDtl.trip_place_no == place_no)
                .values(deleted_yn="Y")
            )
            await session.commit()
        self.assertEqual(
            await self.request("POST", path, 404, body, key),
            {"detail": "TRIP_PLACE_NOT_FOUND"},
        )


if __name__ == "__main__":
    unittest.main()
