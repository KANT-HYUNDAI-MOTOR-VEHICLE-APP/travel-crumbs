package com.example.travelcrumbs.data.gleo

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.travelcrumbs.domain.AppConfig
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
            // 다른 앱이 같은 action 으로 여행 정보를 받지 않도록 Gleo 앱으로만 보냄(#69 Pleos 리뷰).
            setPackage(AppConfig.GLEO_PACKAGE)
            putExtra(EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(EXTRA_RESPONSE, payload)
        }
        runCatching { context.sendBroadcast(intent) }
            .onSuccess { Log.i(TAG, "callback 보냄: ${callback.javaClass.simpleName} · ${payload.length}자") }
            .onFailure { Log.w(TAG, "callback 못 보냄: ${it.javaClass.simpleName}") }
    }

    private companion object {
        const val TAG = "GleoCallback"
        const val ACTION_APP_ACTION_RESPONSE = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_RESPONSE = "response"
    }
}
