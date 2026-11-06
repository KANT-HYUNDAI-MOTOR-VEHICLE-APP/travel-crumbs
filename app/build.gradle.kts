plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.travelcrumbs"
    // 설치된 플랫폼이 android-36.1 · android-37.0 뿐 → 36 의 마이너 1 을 그대로 가리킴
    compileSdk = 36
    compileSdkMinor = 1
    buildToolsVersion = "36.0.0"
    // 주행 규제(CarUxRestrictionsManager) — 플랫폼의 optional android.car.jar 로 컴파일 · 기기의 car 라이브러리로 실행
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.example.travelcrumbs"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // 서버 주소는 빌드 타입별로 BuildConfig 한 곳 — 실제 주소는 서버 이슈에서 정함
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:4000/\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://api.invalid/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric 이 문자열 · 드로어블 자원을 읽게 함(Compose UI 테스트)
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
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

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.datastore.preferences)

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
