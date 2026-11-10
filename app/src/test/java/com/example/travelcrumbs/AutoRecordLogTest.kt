package com.example.travelcrumbs

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T34(#66 4.3) — 자동 기록 기능 파일의 로그 호출(`Log.*`) 인자에 좌표 · 주소 · 이름 · POI · 도착 시각 · 장소 번호 값이 없음.
 * 허용 목록 밖의 보간은 모두 위반(값을 새로 찍으려면 이 목록을 사람이 고쳐야 함). 허용 = 글자 · 예외 종류(`javaClass.simpleName`) · 오류 코드 이름(`errorCode.name`) · 건수(`size` 계산) · 처리 이름(`what`).
 */
class AutoRecordLogTest {

    private val root = File("src/main/java/com/example/travelcrumbs")
    private val files = listOf(
        "data/autorecord/AutoPlaceRecorder.kt",
        "data/autorecord/AutoRecordLocalDataSource.kt",
        "data/autorecord/AutoRecordEndTrip.kt",
        "data/navi/NaviInfoDataSource.kt",
        "data/navi/NaviRepository.kt",
        "data/navi/NaviListenerSlot.kt",
        "domain/AutoRecordRules.kt",
    )
    private val logCall = Regex("""Log\.[a-z]\((.*)\)""")
    private val interpolation = Regex("""\$\{([^}]*)}|\$([A-Za-z_][A-Za-z0-9_]*)""")
    private val allowed = Regex("""^(.*javaClass\.simpleName|errorCode\.name|what|[A-Za-z_.() ]*size[A-Za-z_.() +\-]*|after\.size|before\.size - after\.size)$""")

    @Test
    fun `T34 자동 기록 파일의 로그 인자에 위치 · 이름 · 시각 · 장소 번호 값 없음`() {
        val violations = files.flatMap { path ->
            val file = File(root, path)
            assertTrue("파일 없음: $path", file.exists())
            file.readLines().withIndex().flatMap { (index, line) ->
                logCall.findAll(line).flatMap { call ->
                    interpolation.findAll(call.groupValues[1]).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
                        .filter { expr -> !allowed.matches(expr) }
                        .map { "$path:${index + 1} · $it" }
                }.toList()
            }
        }
        assertTrue("로그 인자 위반: $violations", violations.isEmpty())
    }

    @Test
    fun `T34 로그 줄은 한 줄 호출 — 여러 줄 인자로 검사를 피하지 않음`() {
        val multiLine = files.flatMap { path ->
            File(root, path).readLines().withIndex()
                .filter { (_, line) -> line.contains("Log.") && line.trimEnd().endsWith("(") }
                .map { "$path:${it.index + 1}" }
        }
        assertTrue("여러 줄 로그 호출: $multiLine", multiLine.isEmpty())
    }
}
