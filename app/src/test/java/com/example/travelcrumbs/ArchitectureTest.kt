package com.example.travelcrumbs

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/** android-architecture 규칙 중 import 로 잴 수 있는 것 — 1 · 4 · 7 · 8. */
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
    fun `규칙 4 ui 는 DataSource 를 직접 참조하지 않음`() {
        files.withPackage("..travelcrumbs.ui..")
            .assertFalse { file ->
                file.hasImport { import -> import.name.endsWith("DataSource") }
            }
    }

    @Test
    fun `규칙 7 ai pleos import 는 data 의 SDK 경계 패키지에만`() {
        files.assertTrue { file ->
            !file.hasImport { import -> import.name.startsWith("ai.pleos.") } ||
                file.packagee?.name.orEmpty().let { pkg ->
                    pkg == "com.example.travelcrumbs.data.vehicle" ||
                        pkg == "com.example.travelcrumbs.data.navi" ||
                        pkg == "com.example.travelcrumbs.data.llm"
                }
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
}
