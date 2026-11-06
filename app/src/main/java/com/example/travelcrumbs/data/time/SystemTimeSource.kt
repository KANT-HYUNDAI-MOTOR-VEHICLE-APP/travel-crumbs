package com.example.travelcrumbs.data.time

import com.example.travelcrumbs.domain.TimeSource
import javax.inject.Inject

class SystemTimeSource @Inject constructor() : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
