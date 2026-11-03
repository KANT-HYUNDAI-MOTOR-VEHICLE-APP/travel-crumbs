package com.example.travelcrumbs.data.llm

import ai.pleos.playground.llm.LLM
import ai.pleos.playground.llm.constant.Mode
import ai.pleos.playground.llm.listener.ResultListener
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Pleos LLM SDK 를 감싸는 유일한 파일 — `ai.pleos.*` 타입은 이 패키지 밖으로 나가지 않음.
 * 기기 안 모델(Mode.ON_DEVICE)만 씀 — 서버 모드는 앱 등록 키가 필요해 서버 이슈 몫.
 */
@Singleton
class LlmDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val llm = LLM(context.applicationContext, Mode.ON_DEVICE)
    private val initialized = AtomicBoolean(false)

    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            runCatching { llm.initialize() }
                .onFailure { Log.w(TAG, "LLM initialize 실패: ${it.javaClass.simpleName}") }
        }
    }

    fun release() {
        if (initialized.compareAndSet(true, false)) {
            runCatching { llm.release() }
                .onFailure { Log.w(TAG, "LLM release 실패: ${it.javaClass.simpleName}") }
        }
    }

    /** 프롬프트 한 번 보내고 답 한 번 받기. */
    suspend fun generate(prompt: String): String = suspendCancellableCoroutine { continuation ->
        val listener = object : ResultListener {
            override fun onResponse(response: String, isCompleted: Boolean) {
                if (continuation.isActive) continuation.resume(response)
            }

            override fun onError(error: String) {
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }
        runCatching { llm.generateContent(prompt, listener) }
            .onFailure { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    private companion object {
        const val TAG = "LlmDataSource"
    }
}
