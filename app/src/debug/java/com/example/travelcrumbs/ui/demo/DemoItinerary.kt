package com.example.travelcrumbs.ui.demo

import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason

internal sealed interface DemoLegPhase {
    data object Ready : DemoLegPhase
    data object Requesting : DemoLegPhase
    data object Guiding : DemoLegPhase
    data object Cancelled : DemoLegPhase
    data object Complete : DemoLegPhase
    data class Failed(val reason: RouteFailReason) : DemoLegPhase
}

internal data class DemoItinerary(
    val account: String,
    val roadmapId: String,
    val title: String,
    val places: List<Place>,
    val arrivedCount: Int = 0,
    val phase: DemoLegPhase = DemoLegPhase.Ready,
    val nativeOpenFailed: Boolean = false,
    val requestDispatched: Boolean = false,
    val lastArrivalSimulated: Boolean = false,
) {
    val target: Place? get() = places.getOrNull(arrivedCount)
    val lastArrival: Place? get() = places.getOrNull(arrivedCount - 1)
    val canRequest: Boolean get() = target != null &&
        (phase == DemoLegPhase.Ready || phase == DemoLegPhase.Cancelled || phase is DemoLegPhase.Failed)

    val canAcceptArrival: Boolean get() = phase == DemoLegPhase.Guiding || outstandingRequest

    private val outstandingRequest: Boolean get() = requestDispatched &&
        (phase == DemoLegPhase.Requesting || phase == DemoLegPhase.Failed(RouteFailReason.TIMEOUT))

    fun arrive(destinationName: String, simulated: Boolean = false): DemoItinerary {
        if (!canAcceptArrival || target?.name?.trim() != destinationName.trim()) return this
        val next = arrivedCount + 1
        return copy(
            arrivedCount = next,
            phase = if (next == places.size) DemoLegPhase.Complete else DemoLegPhase.Ready,
            nativeOpenFailed = false,
            requestDispatched = false,
            lastArrivalSimulated = simulated,
        )
    }

    fun cancel(): DemoItinerary =
        if (canAcceptArrival) copy(phase = DemoLegPhase.Cancelled, requestDispatched = false) else this
}
