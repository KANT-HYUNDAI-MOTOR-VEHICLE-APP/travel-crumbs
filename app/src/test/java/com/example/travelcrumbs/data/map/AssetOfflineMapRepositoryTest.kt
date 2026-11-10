package com.example.travelcrumbs.data.map

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AssetOfflineMapRepositoryTest {
    @Test
    fun `M12 JSON 형식이나 지역 범위가 깨진 에셋은 빈 지도 묶음으로 대체`() = runTest {
        val malformedAssets = listOf(
            "{",
            """{"areas":{"town":{"label":"강원 테스트시","bounds":[128.0,38.0,128.1]}}}""",
        )
        for (asset in malformedAssets) {
            assertTrue(AssetOfflineMapRepository { asset }.mapPack().areas.isEmpty())
        }
    }
}
