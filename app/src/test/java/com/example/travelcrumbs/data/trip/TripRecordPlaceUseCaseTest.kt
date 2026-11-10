package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.GleoPlaceWriteInterceptor
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.NaviLocation
import com.example.travelcrumbs.domain.model.Trip
import com.example.travelcrumbs.testing.FakeNaviRepository
import com.example.travelcrumbs.testing.FakeTimeSource
import com.example.travelcrumbs.testing.FakeTripRepository
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TripRecordPlaceUseCaseTest {

    private val recording = Trip(1L, "uuid-1", "2026.11.07 여행", "RECORDING", listOf("SOLO"))
    private val here = NaviLocation(37.7543, 128.899, "강원특별자치도 강릉시 성내동")

    private fun useCase(trips: TripRepository, navi: FakeNaviRepository) =
        TripRecordPlaceUseCase(trips, navi, FakeTimeSource(now = 1_000L))

    @Test
    fun `기록 중이면 지금 위치를 임시 저장하고 장소 수가 오름`() = runTest {
        val trips = FakeTripRepository(current = recording)
        val result = useCase(trips, FakeNaviRepository(location = Result.success(here)))()

        assertEquals(ActionResult.DONE, result)
        assertEquals(1, trips.places.size)
        assertEquals(37.7543, trips.places.single().lat, 0.0)
        assertEquals(1, trips.activeTrip.value?.placeCount)
    }

    @Test
    fun `기록 중인 여행이 없으면 실패`() = runTest {
        val trips = FakeTripRepository(current = null)
        assertEquals(ActionResult.FAILED, useCase(trips, FakeNaviRepository(location = Result.success(here)))())
    }

    @Test
    fun `T13 위치를 못 받거나 0,0 이면 저장하지 않음`() = runTest {
        val trips = FakeTripRepository(current = recording)
        val key = UUID.randomUUID()
        assertEquals(ActionResult.FAILED, useCase(trips, FakeNaviRepository())(key))
        val zero = FakeNaviRepository(location = Result.success(NaviLocation(0.0, 0.0, null)))
        assertEquals(ActionResult.FAILED, useCase(trips, zero)(key))
        assertEquals(0, trips.places.size)
    }

    @Test
    fun `T14 T17 알려진 HTTP 거절은 실패 응답 유실은 미확인`() = runTest {
        val trips = FakeTripRepository(current = recording)
        val write = useCase(trips, FakeNaviRepository(location = Result.success(here)))
        val key = UUID.randomUUID()
        for (code in listOf(404, 409, 422, 500)) {
            trips.failWith = HttpException(retrofit2.Response.error<Unit>(code, "{}".toResponseBody()))
            val expected = if (code == 500) ActionResult.UNCONFIRMED else ActionResult.FAILED
            assertEquals(expected, write(key))
        }
        trips.failWith = IOException("response lost")
        assertEquals(ActionResult.UNCONFIRMED, write(key))
        assertEquals(ActionResult.FAILED, write())
    }

    @Test
    fun `T31 T36 키와 본문 전달 및 수동 호환 지연 응답이 장소 수를 되돌리지 않음`() = runTest {
        val postHeaders = mutableListOf<String?>()
        val postBodies = mutableListOf<String>()
        val postOneShot = mutableListOf<Boolean>()
        val firstWriteStarted = CompletableDeferred<Unit>()
        val releaseFirstResponse = CountDownLatch(1)
        val jsonMediaType = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(GleoPlaceWriteInterceptor())
            .addInterceptor { chain ->
                val request = chain.request()
                val json = if (request.method == "GET") {
                    """{"trip":{"trip_no":1,"trip_uuid":"uuid-1","trip_nm":"여행","trip_status_cd":"RECORDING","visibility_cd":"PRIVATE","start_at":"2026-11-07T09:00:00Z","companion_type_cds":[]}}"""
                } else {
                    assertEquals("/trips/uuid-1/places", request.url.encodedPath)
                    val body = requireNotNull(request.body)
                    val bytes = Buffer()
                    body.writeTo(bytes)
                    postHeaders += request.header("Idempotency-Key")
                    postBodies += bytes.readUtf8()
                    postOneShot += body.isOneShot()
                    val placeCount = if (postHeaders.size == 1) {
                        firstWriteStarted.complete(Unit)
                        check(releaseFirstResponse.await(5, TimeUnit.SECONDS))
                        1
                    } else {
                        2
                    }
                    val placeNo = if (request.header("Idempotency-Key") == null) 2 else 1
                    """{"trip_place_no":$placeNo,"lat":37.7543,"lng":128.899,"recorded_at":"2026-11-07T09:00:00Z","draft_yn":"Y","place_cnt":$placeCount}"""
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (request.method == "GET") 200 else 201)
                    .message("OK")
                    .body(json.toResponseBody(jsonMediaType))
                    .build()
            }
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://fixture.invalid/")
            .client(client)
            .addConverterFactory(Json.asConverterFactory(jsonMediaType))
            .build()
            .create(AppApi::class.java)
        val trips = DefaultTripRepository(api)
        trips.restoreCurrentTrip()
        val write = useCase(trips, FakeNaviRepository(location = Result.success(here)))
        val key = UUID.randomUUID()

        val firstWrite = async { write(key) }
        try {
            firstWriteStarted.await()
            assertEquals(ActionResult.DONE, write())
            assertEquals(2, trips.activeTrip.value?.placeCount)
        } finally {
            releaseFirstResponse.countDown()
        }
        assertEquals(ActionResult.DONE, firstWrite.await())
        assertEquals(2, trips.activeTrip.value?.placeCount)
        assertEquals(ActionResult.DONE, write(key))
        assertEquals(listOf(key.toString(), null, key.toString()), postHeaders)
        assertEquals(listOf(true, false, true), postOneShot)
        assertEquals(1, postBodies.distinct().size)
    }
}
