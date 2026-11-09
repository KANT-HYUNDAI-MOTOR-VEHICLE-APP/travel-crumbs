package com.example.travelcrumbs.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.model.RouteFailReason

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DemoItineraryCard(
    itinerary: DemoItinerary,
    arrivedPlaceToSave: DemoGuidedDestination?,
    onContinue: () -> Unit,
    onSimulateArrival: () -> Unit,
    onSaveArrival: (DemoGuidedDestination) -> Unit,
    onEnd: () -> Unit,
) {
    DemoCard {
        DemoTitle(stringResource(R.string.demo_itinerary_title, itinerary.title))
        Text(stringResource(R.string.demo_itinerary_progress, itinerary.arrivedCount, itinerary.places.size))
        itinerary.lastArrival?.let {
            val label = if (itinerary.lastArrivalSimulated) R.string.demo_itinerary_arrived_simulated else R.string.demo_itinerary_arrived
            Text(stringResource(label, it.name))
        }
        itinerary.target?.let { target ->
            val label = when (itinerary.phase) {
                DemoLegPhase.Guiding, DemoLegPhase.Requesting -> R.string.demo_itinerary_current_stop
                else -> R.string.demo_itinerary_next_stop
            }
            Text(stringResource(label, itinerary.arrivedCount + 1, target.name))
        }
        when (val phase = itinerary.phase) {
            DemoLegPhase.Requesting -> Text(stringResource(R.string.demo_navi_requesting))
            DemoLegPhase.Guiding -> Text(stringResource(R.string.demo_navi_started))
            DemoLegPhase.Cancelled -> Text(stringResource(R.string.demo_itinerary_cancelled))
            DemoLegPhase.Complete -> Text(stringResource(R.string.demo_itinerary_complete))
            is DemoLegPhase.Failed -> Text(stringResource(phase.reason.messageRes()))
            DemoLegPhase.Ready -> Unit
        }
        if (itinerary.nativeOpenFailed) Text(stringResource(R.string.demo_navi_open_unavailable))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (itinerary.canRequest) {
                val label = when (itinerary.phase) {
                    DemoLegPhase.Cancelled, is DemoLegPhase.Failed -> R.string.demo_navi_retry
                    else -> R.string.demo_itinerary_continue
                }
                DemoAction(stringResource(label), onContinue)
            }
            if (itinerary.canAcceptArrival) {
                DemoAction(stringResource(R.string.demo_itinerary_simulate_arrival), onSimulateArrival, primary = false)
            }
            arrivedPlaceToSave?.let {
                DemoAction(stringResource(R.string.demo_itinerary_save_arrival), { onSaveArrival(it) }, primary = false)
            }
            if (itinerary.phase != DemoLegPhase.Guiding && itinerary.phase != DemoLegPhase.Requesting) {
                val label = if (itinerary.phase == DemoLegPhase.Complete) R.string.demo_itinerary_done else R.string.demo_itinerary_end
                DemoAction(stringResource(label), onEnd, primary = false)
            }
        }
    }
}

private fun RouteFailReason.messageRes(): Int = when (this) {
    RouteFailReason.NOT_CONNECTED -> R.string.demo_navi_not_connected
    RouteFailReason.TOO_CLOSE -> R.string.demo_navi_too_close
    RouteFailReason.TOO_FAR -> R.string.demo_navi_too_far
    RouteFailReason.LOW_BATTERY -> R.string.demo_navi_low_battery
    RouteFailReason.ROUTE_NOT_FOUND -> R.string.demo_navi_route_not_found
    RouteFailReason.TIMEOUT -> R.string.demo_navi_timeout
    RouteFailReason.REQUEST_FAILED -> R.string.demo_navi_failed
}
