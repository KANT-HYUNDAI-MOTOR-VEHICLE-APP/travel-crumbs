package com.example.travelcrumbs.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.navi.DefaultNaviRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.awaitRouteResult
import com.example.travelcrumbs.domain.isHeadingTo
import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.NaviRouteEvent
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteFailReason
import com.example.travelcrumbs.domain.model.RouteRequestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class DemoGuidedDestination(val account: String, val place: Place)

@HiltViewModel
internal class DemoGuidanceViewModel @Inject constructor(
    private val navi: DefaultNaviRepository,
    driving: DrivingSignalRepository,
) : ViewModel() {
    private val drivingState = driving.drivingState.stateIn(viewModelScope, SharingStarted.Eagerly, DrivingState.UNKNOWN)
    private val itinerary = MutableStateFlow<DemoItinerary?>(null)
    val uiState = itinerary.asStateFlow()
    private var pendingDispatch: DemoGuidedDestination? = null
    private var lastAttempt: DemoGuidedDestination? = null
    private var startingRoute: DemoGuidedDestination? = null
    private var activeRoute: DemoGuidedDestination? = null
    private val awaitingSave = MutableStateFlow<DemoGuidedDestination?>(null)
    val pendingSave = awaitingSave.asStateFlow()
    private val arrivedPlaceToSave = MutableStateFlow<DemoGuidedDestination?>(null)
    val arrivalToSave = arrivedPlaceToSave.asStateFlow()
    private val returnAfterNativeEvent = MutableStateFlow<DemoGuidedDestination?>(null)
    val parkedReturn = combine(returnAfterNativeEvent, drivingState) { destination, driving ->
        destination.takeIf { driving == DrivingState.PARKED }
    }

    init {
        viewModelScope.launch {
            navi.routeEvents.collect { event ->
                when (event) {
                    is NaviRouteEvent.Started -> onNativeStart(event.destinationName)
                    is NaviRouteEvent.Arrived -> onNativeArrival(event.destinationName)
                    NaviRouteEvent.Cancelled -> onNativeCancel()
                }
            }
        }
    }

    fun prepareStart(roadmap: DemoRoadmap, account: String): Boolean {
        if (drivingState.value != DrivingState.PARKED) return false
        val current = itinerary.value
        if (current != null) return false
        val places = roadmap.stops.mapNotNull(DemoStop::navigationTarget)
        if (places.isEmpty() || places.size != roadmap.stops.size) return false
        arrivedPlaceToSave.value = null
        itinerary.value = DemoItinerary(account, roadmap.id, roadmap.title, places)
        return prepareNext(account)
    }

    fun prepareNext(account: String): Boolean {
        val current = itinerary.value ?: return false
        val target = current.target ?: return false
        if (current.account != account || !current.canRequest || drivingState.value != DrivingState.PARKED) return false
        val request = DemoGuidedDestination(account, target)
        pendingDispatch = request
        lastAttempt = request
        itinerary.value = current.copy(phase = DemoLegPhase.Requesting, nativeOpenFailed = false, requestDispatched = false)
        return true
    }

    fun dispatch(navigationOpened: Boolean) {
        val request = pendingDispatch ?: return
        pendingDispatch = null
        val current = itinerary.value ?: return
        itinerary.value = current.copy(nativeOpenFailed = !navigationOpened)
        viewModelScope.launch {
            if (navigationOpened) delay(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS)
            if (!isCurrent(request)) return@launch
            if (drivingState.value != DrivingState.PARKED) {
                itinerary.value = current.copy(phase = DemoLegPhase.Ready)
                return@launch
            }
            val result = if (navi.ready.value) {
                awaitRouteResult(navi.ready, AppConfig.ROUTE_START_TIMEOUT_MILLIS) {
                    if (!isCurrent(request)) return@awaitRouteResult RouteRequestResult.Failed(RouteFailReason.REQUEST_FAILED)
                    startingRoute = request
                    itinerary.value = itinerary.value?.copy(requestDispatched = true)
                    navi.requestRoute(request.place)
                }
            } else {
                RouteRequestResult.Failed(RouteFailReason.NOT_CONNECTED)
            }
            if (startingRoute === request) startingRoute = null
            if (!isCurrent(request) || itinerary.value?.phase != DemoLegPhase.Requesting) return@launch
            when (result) {
                RouteRequestResult.Started -> markStarted(request)
                is RouteRequestResult.Failed -> {
                    if (result.reason == RouteFailReason.TIMEOUT && isActive(request.place)) {
                        markStarted(request)
                    } else if (isCurrent(request) && itinerary.value?.phase == DemoLegPhase.Requesting) {
                        itinerary.value = itinerary.value?.let {
                            it.copy(
                                phase = DemoLegPhase.Failed(result.reason),
                                requestDispatched = it.requestDispatched && result.reason == RouteFailReason.TIMEOUT,
                            )
                        }
                    }
                }
            }
        }
    }

    fun end(account: String) {
        val current = itinerary.value ?: return
        if (current.account != account || current.phase == DemoLegPhase.Guiding || current.phase == DemoLegPhase.Requesting) return
        resetSession()
    }

    fun resetSession() {
        itinerary.value = null
        pendingDispatch = null
        lastAttempt = null
        startingRoute = null
        activeRoute = null
        awaitingSave.value = null
        arrivedPlaceToSave.value = null
        returnAfterNativeEvent.value = null
    }

    fun simulateArrival(account: String) {
        val current = itinerary.value ?: return
        val target = current.target ?: return
        if (current.account == account && current.canAcceptArrival && drivingState.value == DrivingState.PARKED) {
            onNativeArrival(target.name, simulated = true)
        }
    }

    fun saveArrival(destination: DemoGuidedDestination) {
        if (awaitingSave.value == null && arrivedPlaceToSave.compareAndSet(destination, null)) {
            awaitingSave.value = destination
        }
    }

    fun dismissSave(destination: DemoGuidedDestination) {
        awaitingSave.compareAndSet(destination, null)
    }

    fun recheck() {
        val request = lastAttempt ?: return
        val current = itinerary.value ?: return
        if (!current.requestDispatched ||
            (current.phase != DemoLegPhase.Requesting && current.phase != DemoLegPhase.Failed(RouteFailReason.TIMEOUT))
        ) return
        viewModelScope.launch {
            delay(AppConfig.NAVI_QUERY_SETTLE_DELAY_MILLIS)
            if (isCurrent(request) && isActive(request.place)) markStarted(request)
        }
    }

    private fun onNativeStart(destinationName: String) {
        val name = destinationName.trim()
        val request = startingRoute
        if (request != null && request.place.name.trim() == name) {
            markStarted(request)
        } else if (activeRoute?.place?.name?.trim()?.let { it != name } == true) {
            val previous = activeRoute
            activeRoute = null
            if (previous != null && isCurrent(previous)) itinerary.value = itinerary.value?.cancel()
        }
    }

    private fun onNativeArrival(destinationName: String, simulated: Boolean = false) {
        val arrived = activeRoute ?: lastAttempt ?: return
        val current = itinerary.value ?: return
        if (!isCurrent(arrived)) return
        val next = current.arrive(destinationName, simulated)
        if (next === current) return
        invalidateAttempt()
        itinerary.value = next
        arrivedPlaceToSave.value = arrived
        returnAfterNativeEvent.value = arrived
    }

    private fun onNativeCancel() {
        val cancelled = activeRoute ?: lastAttempt ?: return
        val current = itinerary.value ?: return
        if (!isCurrent(cancelled)) return
        val next = current.cancel()
        if (next === current) return
        val wasConfirmed = activeRoute === cancelled
        invalidateAttempt()
        itinerary.value = next
        if (wasConfirmed) {
            if (awaitingSave.value == null) awaitingSave.value = cancelled
            returnAfterNativeEvent.value = cancelled
        }
    }

    private fun invalidateAttempt() {
        activeRoute = null
        startingRoute = null
        lastAttempt = null
    }

    private fun isCurrent(request: DemoGuidedDestination): Boolean =
        lastAttempt === request && itinerary.value?.target?.id == request.place.id

    private fun markStarted(request: DemoGuidedDestination) {
        val current = itinerary.value ?: return
        if (!isCurrent(request) || !current.requestDispatched ||
            (current.phase != DemoLegPhase.Requesting && current.phase != DemoLegPhase.Failed(RouteFailReason.TIMEOUT))
        ) return
        if (startingRoute === request) startingRoute = null
        activeRoute = request
        itinerary.value = current.copy(phase = DemoLegPhase.Guiding)
    }

    private suspend fun isActive(place: Place): Boolean {
        if (!navi.ready.value) return false
        return try {
            withTimeoutOrNull(AppConfig.SDK_CALL_TIMEOUT_MS) { navi.activeRoute() }?.isHeadingTo(place) == true
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            false
        }
    }
}
