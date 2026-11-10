package com.example.travelcrumbs.di

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T110 · 심사 A2 — release 에서 「더미 경로」가 꺼지는 것은 실제 모듈의 한 식(`BuildConfig.DEBUG`)에 달림.
 * fake `DemoFeatures(false)` 시험만으로는 이 식이 `true` 로 바뀌어도 녹색이라 본문을 고정함.
 */
class BuildFeaturesModuleTest {

    private val production = Konsist.scopeFromProduction()

    @Test
    fun `T110 provideDemoFeatures 본문은 dummyRoute = BuildConfig DEBUG 한 식`() {
        val providers = production.functions().filter { it.name == "provideDemoFeatures" }
        assertEquals(1, providers.size)
        val body = providers.single().text.substringAfter("provideDemoFeatures()").replace(Regex("""\s+"""), " ").trim()
        assertEquals(": DemoFeatures = DemoFeatures(dummyRoute = BuildConfig.DEBUG)", body)
    }

    @Test
    fun `T110 제품 코드에서 DemoFeatures 를 만드는 곳은 BuildFeaturesModule 하나`() {
        val makers = production.files
            .filter { file -> file.name != "DemoFeatures" && file.text.contains("DemoFeatures(") }
            .map { it.name }
        assertEquals(listOf("BuildFeaturesModule"), makers)
        assertTrue(production.files.none { it.text.contains("dummyRoute = true") })
    }
}
