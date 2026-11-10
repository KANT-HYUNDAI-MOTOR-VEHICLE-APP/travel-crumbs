package com.example.travelcrumbs.data.remote

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/** 키가 있는 Gleo 장소 POST만 전송 후 자동 재전송을 제한. */
internal class GleoPlaceWriteInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body
        if (request.header(PLACE_IDEMPOTENCY_KEY_HEADER) == null || body == null) return chain.proceed(request)

        val oneShotBody = object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
            override fun isOneShot() = true
        }
        return chain.proceed(request.newBuilder().method(request.method, oneShotBody).build())
    }
}
