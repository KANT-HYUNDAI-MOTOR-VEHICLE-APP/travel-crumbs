package com.example.travelcrumbs.di

import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.GleoPlaceWriteInterceptor
import com.example.travelcrumbs.data.remote.PLACE_IDEMPOTENCY_KEY_HEADER
import com.example.travelcrumbs.domain.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** 네트워크 배선 — Retrofit · 로컬 서버 API(AppApi). */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(GleoPlaceWriteInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                addInterceptor { chain ->
                    val request = chain.request()
                    if (request.header(PLACE_IDEMPOTENCY_KEY_HEADER) == null) {
                        logger.intercept(chain)
                    } else {
                        chain.proceed(request)
                    }
                }
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAppApi(retrofit: Retrofit): AppApi = retrofit.create(AppApi::class.java)
}
