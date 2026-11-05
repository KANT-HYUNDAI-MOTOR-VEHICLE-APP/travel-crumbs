package com.example.travelcrumbs

import android.app.Application
import com.example.travelcrumbs.data.pin.PinUploadScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TravelCrumbsApplication : Application() {

    @Inject
    lateinit var pinUploadScheduler: PinUploadScheduler

    override fun onCreate() {
        super.onCreate()
        // 앱이 뜰 때 대기 핀을 다시 보냄(스펙 navigation 4.6 · T66) — 이미 예약이 있으면 그대로
        pinUploadScheduler.scheduleOnAppStart()
    }
}
