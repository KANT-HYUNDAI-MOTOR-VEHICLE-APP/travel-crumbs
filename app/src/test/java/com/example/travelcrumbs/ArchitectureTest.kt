package com.example.travelcrumbs

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/** android-architecture 규칙 중 선언 · import 로 잴 수 있는 것 — 1 · 2 · 3 · 4 · 7 · 8. */
class ArchitectureTest {

    private val files = Konsist.scopeFromProject().files

    @Test
    fun `규칙 1 domain 은 ui data android androidx 를 모름`() {
        files.withPackage("..travelcrumbs.domain..")
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.example.travelcrumbs.ui") ||
                        import.name.startsWith("com.example.travelcrumbs.data") ||
                        import.name.startsWith("android.") ||
                        import.name.startsWith("androidx.")
                }
            }
    }

    @Test
    fun `규칙 2 Screen 파일에는 바깥에 보이는 Composable 화면 함수가 하나`() {
        Konsist.scopeFromProduction().files
            .filter { it.name.endsWith("Screen") }
            .assertTrue { file ->
                file.functions(includeNested = false, includeLocal = false)
                    .count { it.hasAnnotationWithName("Composable") && !it.hasPrivateModifier } == 1
            }
    }

    @Test
    fun `규칙 3 ViewModel 은 Context Activity Resources android view compose 를 들지 않음`() {
        val forbiddenImports = listOf(
            "android.content.Context",
            "android.app.Activity",
            "android.app.Application",
            "android.content.res.Resources",
            "android.view.",
            "androidx.compose.",
            "androidx.lifecycle.AndroidViewModel",
        )
        files.filter { it.name.endsWith("ViewModel") && it.packagee?.name.orEmpty().contains(".ui.") }
            .assertFalse { file ->
                file.hasImport { import -> forbiddenImports.any { import.name.startsWith(it) } }
            }
    }

    @Test
    fun `규칙 4 ui 는 DataSource 를 직접 참조하지 않음`() {
        files.withPackage("..travelcrumbs.ui..")
            .assertFalse { file ->
                file.hasImport { import -> import.name.endsWith("DataSource") }
            }
    }

    @Test
    fun `규칙 4 T78 ui 와 domain 은 Retrofit · AppApi · Dao · fake 를 직접 참조하지 않음`() {
        val forbidden = listOf(
            "retrofit2.",
            "okhttp3.",
            "androidx.room.",
            "com.example.travelcrumbs.data.remote.",
            "com.example.travelcrumbs.data.local.",
        )
        Konsist.scopeFromProduction().files
            .filter { file ->
                file.packagee?.name.orEmpty().let {
                    it.startsWith("com.example.travelcrumbs.ui") || it.startsWith("com.example.travelcrumbs.domain")
                }
            }
            .assertFalse { file ->
                file.hasImport { import ->
                    forbidden.any { import.name.startsWith(it) } || import.name.contains(".testing.")
                }
            }
    }

    @Test
    fun `규칙 8 T69 data remote 의 DTO 에 좌표 · userId 칸 없음`() {
        val forbidden = Regex("""(lat|lng|lon|latitude|longitude|userId|user_id)""", RegexOption.IGNORE_CASE)
        files.withPackage("..travelcrumbs.data.remote..")
            .flatMap { it.classes() }
            .assertFalse { cls -> cls.properties().any { forbidden.matches(it.name) } }
    }

    @Test
    fun `규칙 7 ai pleos import 는 data 의 SDK 경계 패키지에만`() {
        files.assertTrue { file ->
            !file.hasImport { import -> import.name.startsWith("ai.pleos.") } ||
                file.packagee?.name.orEmpty().let { pkg ->
                    pkg == "com.example.travelcrumbs.data.vehicle" ||
                        pkg == "com.example.travelcrumbs.data.navi"
                }
        }
    }

    @Test
    fun `규칙 7 AAOS android car import 는 data vehicle 에만`() {
        files.assertTrue { file ->
            !file.hasImport { import -> import.name.startsWith("android.car.") } ||
                file.packagee?.name == "com.example.travelcrumbs.data.vehicle"
        }
    }

    @Test
    fun `규칙 8 dagger Module 과 retrofit http 는 정해진 패키지에만`() {
        files.assertTrue { file ->
            val pkg = file.packagee?.name.orEmpty()
            val moduleOk = !file.hasImport { it.name == "dagger.Module" } ||
                pkg == "com.example.travelcrumbs.di"
            val retrofitOk = !file.hasImport { it.name.startsWith("retrofit2.http") } ||
                pkg == "com.example.travelcrumbs.data.remote"
            moduleOk && retrofitOk
        }
    }

    @Test
    fun `규칙 3-1 N16 지도 렌더러 MapLibre import 는 추천 화면 부품에만(#59)`() {
        files.assertTrue { file ->
            !file.hasImport { import -> import.name.startsWith("org.maplibre") } ||
                file.packagee?.name.orEmpty().startsWith("com.example.travelcrumbs.ui.recommendations.components")
        }
    }

    @Test
    fun `규칙 3-1 M41 ui 의 ViewModel UiState 는 좌표 모델 · 지도 자산 저장소를 import 하지 않음(#59)`() {
        val forbidden = setOf(
            "com.example.travelcrumbs.domain.model.MapCoord",
            "com.example.travelcrumbs.domain.model.MapPlace",
            "com.example.travelcrumbs.domain.model.KoreaMapAsset",
            "com.example.travelcrumbs.domain.model.MapBounds",
            "com.example.travelcrumbs.data.map.KoreaMapRepository",
        )
        files.filter { file ->
            file.packagee?.name.orEmpty().startsWith("com.example.travelcrumbs.ui") &&
                (file.name.endsWith("ViewModel") || file.name.endsWith("UiState"))
        }.assertFalse { file -> file.hasImport { import -> import.name in forbidden } }
    }
}
