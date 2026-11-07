package com.example.travelcrumbs.ui.home

import com.lemonappdev.konsist.api.Konsist
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T134 · 심사 A1 — 더미 경로 문구 · 부품은 debug 소스 세트에만 · 「안내 요청 중…」 문자열 하나 · #6 앱 틀은 이 기능이 고치지 않음. */
class GuideScreenSourceTest {

    private val production = Konsist.scopeFromProduction().files

    @Test
    fun `T134 더미 경로 문구를 부르는 코드는 debug 소스 세트에만`() {
        val users = production
            .filter { file -> DEMO_STRINGS.any { file.text.contains("R.string.$it") } }
            .map { it.path.replace(File.separatorChar, '/') }
        assertTrue(users.isNotEmpty())
        assertTrue("debug 밖에서 부름: $users", users.all { "/src/debug/" in it })
    }

    @Test
    fun `T134 release 소스 세트와 main 자원에 더미 경로 글자 없음`() {
        val root = projectRoot()
        val releaseText = File(root, "app/src/release").walkTopDown().filter { it.isFile }.joinToString("\n") { it.readText() }
        val mainStrings = File(root, "app/src/main/res/values/strings.xml").readText()
        listOf("더미 경로", "시연용", "연동 실패").forEach { word ->
            assertTrue("release 에 「$word」", word !in releaseText)
            assertTrue("main strings.xml 에 「$word」", word !in mainStrings)
        }
        DEMO_STRINGS.forEach { assertTrue("main strings.xml 에 $it", "name=\"$it\"" !in mainStrings) }
    }

    @Test
    fun `T134 「안내 요청 중…」 문자열은 한 곳`() {
        val mainStrings = File(projectRoot(), "app/src/main/res/values/strings.xml").readText()
        assertEquals(1, Regex("안내 요청 중…").findAll(mainStrings).count())
    }

    private fun projectRoot(): File {
        var dir = File("").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir
    }

    private companion object {
        val DEMO_STRINGS = listOf("guide_dummy_route", "guide_dummy_notice", "guide_fallback_label")
    }
}
