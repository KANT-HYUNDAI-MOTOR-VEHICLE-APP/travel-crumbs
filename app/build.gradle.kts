import groovy.json.JsonSlurper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.util.Properties
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// debug 시드 토큰(스펙 navigation U27) — 저장소에 커밋하지 않는 local.properties 에서만 읽음 · release 엔 넣지 않음
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val debugSeedToken: String = localProperties.getProperty("travelcrumbs.debugSeedToken", "")

android {
    namespace = "com.example.travelcrumbs"
    // 설치된 플랫폼이 android-36.1 · android-37.0 뿐 → 36 의 마이너 1 을 그대로 가리킴
    compileSdk = 36
    compileSdkMinor = 1
    buildToolsVersion = "36.0.0"
    // 주행 규제(CarUxRestrictionsManager) — 플랫폼의 optional android.car.jar 로 컴파일 · 기기의 car 라이브러리로 실행
    useLibrary("android.car")

    defaultConfig {
        // 차량 내비 앱 패키지 — 매니페스트 <queries>(placeholder) 와 NaviAppDataSource(BuildConfig) 가 이 값 하나를 씀(#51 리뷰 K5)
        val naviPackage = "ai.umos.maps.android.navigation.app"
        manifestPlaceholders["naviPackage"] = naviPackage
        buildConfigField("String", "NAVI_PACKAGE", "\"$naviPackage\"")
        applicationId = "com.example.travelcrumbs"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        // 지도 렌더러 네이티브 라이브러리(#59 N8) — 에뮬레이터(x86_64) · 차량(arm64-v8a)만 · debug · release 같게
        ndk {
            abiFilters += listOf("x86_64", "arm64-v8a")
        }
    }

    buildTypes {
        debug {
            // 서버 주소는 빌드 타입별로 BuildConfig 한 곳 — 실제 주소는 서버 이슈에서 정함
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:4000/\"")
            buildConfigField("String", "DEBUG_SEED_TOKEN", "\"$debugSeedToken\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://api.invalid/\"")
            buildConfigField("String", "DEBUG_SEED_TOKEN", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric 이 문자열 · 드로어블 자원을 읽게 함(Compose UI 테스트)
        unitTests.isIncludeAndroidResources = true
        // 저장소의 실패 로그(android.util.Log)를 JVM 단위 테스트에서 부를 수 있게 — 값은 기본값(0 · null)
        unitTests.isReturnDefaultValues = true
    }

    // 지도 자산(#59) — 타일이 이미 gzip · APK 에서 다시 압축하지 않아야 MapLibre 가 자산을 부분 읽기(byte range)
    androidResources {
        noCompress += "pmtiles"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

// Room 스키마(핀 표) — 다음 판의 마이그레이션 근거로 커밋
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 추천 지도 렌더러(#59 N1 · N8) — 앱 자산 PMTiles 만 읽음 · 원격 주소 없음
    implementation(libs.maplibre.android.sdk.opengl)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    // Pleos SDK — Vehicle 2.3.2 가 kotlin-reflect 1.9.25 를 끌어오므로 내장 Kotlin 판으로 올려 고정
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi.helper)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.konsist)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ── 한국 지도 자산(#59 · N8 결정) ─────────────────────────────────────────────
// 지도 파일(약 69 MB)은 git 에 넣지 않음 — GitHub 릴리스에 올리고 빌드 때 받아 SHA-256 으로 확인한 뒤 앱 assets/maps/ 로 묶음.
// 만드는 법 · 고정값은 tools/map/build_korea_pmtiles.py · 출처는 app/src/main/assets/maps/SOURCES.txt.
// 받은 파일은 Gradle 사용자 폴더 캐시에 해시 이름으로 둠 → 오프라인이어도 캐시가 있으면 통과 · 해시가 틀리면 실패.
// 릴리스 올리기 전 · 로컬 확인: -PmapAssetFile=<파일> (해시는 똑같이 확인).
val koreaMapRepo = "KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs"
val koreaMapReleaseTag = "map-kr-z12-20261109"
val koreaMapFileName = "korea-z12.pmtiles"
val koreaMapSha256 = "4bc9db5875f8d070fae9bf31e273ff4042fbe43243ecd4a23c426b6738c050f0"

abstract class PrepareKoreaMapAsset : DefaultTask() {
    @get:Input abstract val repo: Property<String>
    @get:Input abstract val releaseTag: Property<String>
    @get:Input abstract val fileName: Property<String>
    @get:Input abstract val sha256: Property<String>
    @get:Internal abstract val localFile: Property<String>
    @get:Internal abstract val cacheDir: DirectoryProperty
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun prepare() {
        val expected = sha256.get()
        val cached = cacheDir.get().file("$expected.pmtiles").asFile
        val local = localFile.orNull?.takeIf { it.isNotBlank() }?.let { File(it) }
        val source = when {
            local != null -> {
                check(local.isFile) { "mapAssetFile 이 없음: $local" }
                verify(local, expected)
                local
            }
            cached.isFile && digest(cached) == expected -> cached
            else -> { download(cached, expected); cached }
        }
        val target = outputDir.get().dir("maps").file(fileName.get()).asFile
        target.parentFile.mkdirs()
        verify(source, expected)
        source.copyTo(target, overwrite = true)
        verify(target, expected)
    }

    private fun download(cached: File, expected: String) {
        cached.parentFile.mkdirs()
        val partial = File.createTempFile("korea-map-", ".part", cached.parentFile)
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
        val token = token()
        val start = if (token != null) {
            assetApiUrl(client, token)
        } else {
            "https://github.com/${repo.get()}/releases/download/${releaseTag.get()}/${fileName.get()}"
        }
        var url = start
        var auth = token
        repeat(MAX_REDIRECTS) {
            val builder = HttpRequest.newBuilder(URI(url)).header("Accept", "application/octet-stream")
            auth?.let { builder.header("Authorization", "Bearer $it") }
            val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofFile(partial.toPath()))
            when (response.statusCode()) {
                200 -> {
                    verify(partial, expected)
                    Files.move(partial.toPath(), cached.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    return
                }
                in 300..399 -> {
                    url = response.headers().firstValue("Location").orElseThrow()
                    // 저장소 밖(서명된 다운로드 주소)으로는 토큰을 보내지 않음
                    auth = null
                }
                else -> error(
                    "지도 자산을 받지 못함(HTTP ${response.statusCode()}) — private 저장소면 GH_TOKEN 또는 gh auth login 필요 · " +
                        "또는 -PmapAssetFile=<파일>",
                )
            }
        }
        error("지도 자산 주소가 너무 많이 넘어감: $start")
    }

    /** 릴리스 자산의 API 주소(private 저장소는 이 길로만 받음). */
    private fun assetApiUrl(client: HttpClient, token: String): String {
        val request = HttpRequest.newBuilder(URI("https://api.github.com/repos/${repo.get()}/releases/tags/${releaseTag.get()}"))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "릴리스 ${releaseTag.get()} 를 찾지 못함(HTTP ${response.statusCode()})" }
        @Suppress("UNCHECKED_CAST")
        val assets = (JsonSlurper().parseText(response.body()) as Map<String, Any?>)["assets"] as List<Map<String, Any?>>
        return assets.firstOrNull { it["name"] == fileName.get() }?.get("url") as String?
            ?: error("릴리스 ${releaseTag.get()} 에 ${fileName.get()} 이 없음")
    }

    /** 토큰은 환경 변수(CI: github.token) → gh CLI 로그인 순 · 파일에 두지 않음. */
    private fun token(): String? {
        System.getenv("GH_TOKEN")?.takeIf { it.isNotBlank() }?.let { return it }
        System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() }?.let { return it }
        return runCatching {
            val process = ProcessBuilder("gh", "auth", "token").start()
            val out = process.inputStream.bufferedReader().readText().trim()
            out.takeIf { process.waitFor() == 0 && it.isNotBlank() }
        }.getOrNull()
    }

    private fun verify(file: File, expected: String) {
        val actual = digest(file)
        check(actual == expected) { "지도 자산 SHA-256 이 다름: $actual (기대 $expected) — $file" }
    }

    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}

val prepareKoreaMapAsset = tasks.register<PrepareKoreaMapAsset>("prepareKoreaMapAsset") {
    repo.set(koreaMapRepo)
    releaseTag.set(koreaMapReleaseTag)
    fileName.set(koreaMapFileName)
    sha256.set(koreaMapSha256)
    localFile.set(providers.gradleProperty("mapAssetFile").orElse(""))
    cacheDir.set(File(gradle.gradleUserHomeDir, "caches/travel-crumbs-map"))
    outputDir.set(layout.buildDirectory.dir("generated/koreaMapAssets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(prepareKoreaMapAsset, PrepareKoreaMapAsset::outputDir)
    }
}
