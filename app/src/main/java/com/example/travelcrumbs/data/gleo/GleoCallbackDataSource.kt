package com.example.travelcrumbs.data.gleo

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.travelcrumbs.domain.model.GleoCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Gleo AI 에 답을 돌려보내는 유일한 자리 — Broadcast 하나. */
@Singleton
class GleoCallbackDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun send(callback: GleoCallback) {
        val payload = GleoCallbackFormat.toJson(callback)
        val intent = Intent(ACTION_APP_ACTION_RESPONSE).apply {
            putExtra(EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(EXTRA_RESPONSE, payload)
        }
        runCatching { context.sendBroadcast(intent) }
            .onSuccess { Log.i(TAG, "callback 전송 시도 완료") }
            .onFailure { Log.w(TAG, "callback 못 보냄: ${it.javaClass.simpleName}") }
    }

    private companion object {
        const val TAG = "GleoCallback"
        const val ACTION_APP_ACTION_RESPONSE = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_RESPONSE = "response"
    }
}
