package com.example.travelcrumbs.ui.recommendations.components

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.travelcrumbs.R
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecommendationMapRules
import com.example.travelcrumbs.domain.model.KoreaMapAsset
import com.example.travelcrumbs.domain.model.LocationResultKind
import com.example.travelcrumbs.domain.model.MapBounds
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapFitBasis
import com.example.travelcrumbs.domain.model.MapMarker
import com.example.travelcrumbs.domain.model.MapPinResult
import com.example.travelcrumbs.domain.model.MapPlace
import com.example.travelcrumbs.domain.model.ScreenPlace
import com.example.travelcrumbs.ui.recommendations.RecommendationMapUi
import com.example.travelcrumbs.ui.recommendations.map.MapCamera
import com.example.travelcrumbs.ui.recommendations.map.RecommendationMapEntryPoint
import com.example.travelcrumbs.ui.recommendations.map.RecommendationMapHolder
import com.example.travelcrumbs.ui.recommendations.map.RecommendationMapRuntime
import com.example.travelcrumbs.ui.recommendations.map.MapAccess
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlin.math.cos
import com.example.travelcrumbs.ui.theme.Dimens
import dagger.hilt.android.EntryPointAccessors
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** 지도 부품이 화면(ViewModel)으로 올리는 것 — 장소 번호 · 개수 · 결과 종류만(좌표 없음 · 스펙 3-1). */
class RecommendationMapCallbacks(
    val onReady: () -> Unit,
    val onFailed: () -> Unit,
    val onFirstFitDone: () -> Unit,
    val onMovedByUser: () -> Unit,
    val onPins: (MapPinResult) -> Unit,
    val onSelectPlace: (Long) -> Unit,
    val onSelectCluster: (List<Long>) -> Unit,
    val onLocation: (Boolean, Boolean, LocationResultKind) -> Unit = { _, _, _ -> },
    val onDataChanged: () -> Unit = {},
)

/**
 * B-01 한국 전국 지도(#59) — 앱 자산 PMTiles 를 MapLibre 로 그림 · 원격 요청 없음(N1 · M31).
 * 좌표 · 카메라 · 화면 위치는 이 부품과 [RecommendationMapHolder] 에만 · 위로는 [RecommendationMapCallbacks] 의 번호 · 개수만.
 * 정차 화면에서만 보임(주행 · 모름이면 추천 칸 자체가 주행 화면으로 바뀜).
 * 회전 · 기울이기 없음 · 한국 범위 밖으로 안 밀림 · 확대는 [AppConfig.RECO_MAP_MAX_ZOOM] 까지(N7 · N8 · M37).
 */
@Composable
fun RecommendationMap(
    entryKey: Int,
    mapUi: RecommendationMapUi,
    selectedSidoNm: String?,
    callbacks: RecommendationMapCallbacks,
    modifier: Modifier = Modifier,
    holder: RecommendationMapHolder? = rememberRecommendationMapHolder(),
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val radiusPx = with(LocalDensity.current) { AppConfig.RECO_MAP_CLUSTER_RADIUS_DP.dp.toPx() }
    val latestCallbacks by rememberUpdatedState(callbacks)
    val latestUi by rememberUpdatedState(mapUi)
    val latestSido by rememberUpdatedState(selectedSidoNm)
    val runtime = remember(context) {
        runCatching { EntryPointAccessors.fromApplication(context.applicationContext, RecommendationMapEntryPoint::class.java).recommendationMapRuntime() }.getOrNull()
    }
    val access by (runtime?.access ?: kotlinx.coroutines.flow.flowOf(MapAccess())).collectAsStateWithLifecycle(MapAccess())
    var ownLocation by remember { mutableStateOf<MapCoord?>(null) }
    var ownPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // 보관 객체 · 렌더러를 못 얻으면(Hilt 없는 실행 · 네이티브 라이브러리 없음) 지도 못 그림(N2)
    val mapView = remember(holder) { holder?.let { createMapView(context) } }
    var asset by remember { mutableStateOf<KoreaMapAsset?>(null) }
    var places by remember { mutableStateOf<List<MapPlace>>(emptyList()) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var markers by remember { mutableStateOf<List<MapMarker>>(emptyList()) }
    // 첫 맞춤 · 복원 전 카메라(스타일 읽는 중 기본 위치)를 보관 객체에 남기지 않게
    var cameraSettled by remember { mutableStateOf(false) }

    if (holder == null || mapView == null) {
        LaunchedEffect(Unit) { latestCallbacks.onFailed() }
        return
    }

    fun recompute(report: Boolean) {
        val current = map ?: return
        if (!styleReady || mapView.width == 0 || mapView.height == 0) return
        val projection = current.projection
        ownPoint = ownLocation?.let { coord -> projection.toScreenLocation(LatLng(coord.lat, coord.lng)).let { it.x to it.y } }
        val width = mapView.width.toFloat()
        val height = mapView.height.toFloat()
        val onScreen = places.mapNotNull { place ->
            val point = projection.toScreenLocation(LatLng(place.coord.lat, place.coord.lng))
            if (point.x in 0f..width && point.y in 0f..height) ScreenPlace(place.placeNo, point.x, point.y, place.reviewCnt) else null
        }
        val next = RecommendationMapRules.markers(onScreen, radiusPx, AppConfig.RECO_MAP_MARKER_LIMIT)
        markers = next
        if (report) latestCallbacks.onPins(RecommendationMapRules.pinResult(next, onScreen.size))
    }

    // 공개 자료 변경은 표시 중에만 구독. 핀 원본 좌표는 렌더러 쪽에서만 다시 읽음.
    LaunchedEffect(runtime, holder, lifecycle) {
        val source = runtime ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            source.publicChanges.collectLatest {
                places = holder.places()
                latestCallbacks.onDataChanged()
                recompute(report = true)
            }
        }
    }

    // 화면 수명·정차·동의를 함께 확인. 취소 뒤 늦은 결과는 폐기하고 내 위치 점도 즉시 지움.
    LaunchedEffect(runtime, access, styleReady, entryKey, cameraSettled) {
        val source = runtime ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val allowed = access.consent && access.parked
            if (!allowed || !styleReady || !cameraSettled) {
                ownLocation = null
                ownPoint = null
                latestCallbacks.onLocation(access.consent, false, LocationResultKind.NONE)
                return@repeatOnLifecycle
            }
            try {
                latestCallbacks.onLocation(true, true, LocationResultKind.LOCATING)
                val here = try { source.locate(access) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { null }
                val ready = asset ?: return@repeatOnLifecycle
                val domestic = here?.takeIf { ready.countryBounds.contains(it) }
                ownLocation = domestic
                if (domestic != null && !latestUi.firstFitDone) {
                    val delta = AppConfig.RECO_MAP_FIRST_FIT_RADIUS_KM / 111.195
                    val longitude = delta / cos(Math.toRadians(domestic.lat))
                    map?.moveCamera(CameraUpdateFactory.newLatLngBounds(MapBounds(domestic.lng - longitude, domestic.lat - delta, domestic.lng + longitude, domestic.lat + delta).toLatLngBounds(), fitPaddingPx(mapView)))
                    latestCallbacks.onFirstFitDone()
                }
                latestCallbacks.onLocation(true, true, when { here == null -> LocationResultKind.FAILED; domestic == null -> LocationResultKind.ABROAD; else -> LocationResultKind.DOMESTIC })
                recompute(report = true)
                kotlinx.coroutines.awaitCancellation()
            } finally {
                ownLocation = null
                ownPoint = null
                latestCallbacks.onLocation(access.consent, false, LocationResultKind.NONE)
            }
        }
    }

    // 자산 · 핀 원본 — 못 읽으면 지도 못 그림(N2 · M30)
    LaunchedEffect(holder) {
        val loaded = holder.mapAsset()
        if (loaded == null) {
            latestCallbacks.onFailed()
            return@LaunchedEffect
        }
        places = holder.places()
        asset = loaded
    }

    // 렌더러 준비 · 스타일(앱 자산) 읽기
    LaunchedEffect(asset) {
        val ready = asset ?: return@LaunchedEffect
        mapView.addOnDidFailLoadingMapListener { latestCallbacks.onFailed() }
        mapView.getMapAsync { libreMap ->
            libreMap.uiSettings.apply {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isCompassEnabled = false
                // 기본 로고 · (i) 출처 버튼은 외부 웹 링크를 엶 → 끄고 자체 「© OpenStreetMap contributors」 상시 표시(M32)
                isLogoEnabled = false
                isAttributionEnabled = false
            }
            libreMap.setMinZoomPreference(AppConfig.RECO_MAP_MIN_ZOOM)
            libreMap.setMaxZoomPreference(AppConfig.RECO_MAP_MAX_ZOOM)
            libreMap.setLatLngBoundsForCameraTarget(ready.cameraBounds.toLatLngBounds())
            libreMap.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) latestCallbacks.onMovedByUser()
            }
            libreMap.addOnCameraMoveListener { recompute(report = false) }
            libreMap.addOnCameraIdleListener {
                if (cameraSettled) libreMap.cameraPosition.target?.let { target ->
                    holder.saveCamera(MapCamera(MapCoord(lng = target.longitude, lat = target.latitude), libreMap.cameraPosition.zoom))
                }
                recompute(report = true)
            }
            libreMap.setStyle(Style.Builder().fromUri(ready.styleUri)) {
                map = libreMap
                styleReady = true
                latestCallbacks.onReady()
            }
        }
    }

    // 첫 맞춤 또는 같은 진입 안의 카메라 복원(B-02 · B-04 다녀옴 · M38)
    LaunchedEffect(styleReady, entryKey) {
        val current = map ?: return@LaunchedEffect
        val ready = asset ?: return@LaunchedEffect
        mapView.post {
            val needsFit = holder.beginEntry(entryKey)
            val saved = holder.camera
            if (!needsFit && saved != null) {
                current.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(saved.center.lat, saved.center.lng))
                    .zoom(saved.zoom)
                    .build()
                holder.markFocusApplied(latestUi.focusSeq)
            } else {
                val bounds = when (latestUi.fitBasis) {
                    MapFitBasis.SIDO -> latestSido?.let { ready.sidoBounds[it] } ?: ready.countryBounds
                    // 내 위치 10km 는 #55 문구 반영 뒤 · 그 전엔 이 값이 오지 않음
                    MapFitBasis.MY_LOCATION, MapFitBasis.COUNTRY -> ready.countryBounds
                }
                current.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.toLatLngBounds(), fitPaddingPx(mapView)))
                holder.markFocusApplied(latestUi.focusSeq)
            }
            cameraSettled = true
            if (!access.consent || saved != null) latestCallbacks.onFirstFitDone()
            recompute(report = true)
        }
    }

    // 시도 칩 → 그 시도가 다 들어오게 이동(N5 · M06)
    LaunchedEffect(mapUi.focusSeq, styleReady) {
        val current = map ?: return@LaunchedEffect
        val ready = asset ?: return@LaunchedEffect
        val seq = mapUi.focusSeq
        if (seq == 0 || seq == holder.appliedFocusSeq) return@LaunchedEffect
        holder.markFocusApplied(seq)
        val bounds = mapUi.focusSidoNm?.let { ready.sidoBounds[it] } ?: return@LaunchedEffect
        current.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.toLatLngBounds(), fitPaddingPx(mapView)), FOCUS_ANIMATION_MILLIS)
    }

    // 렌더러 수명 — 화면 수명을 따르고 화면에서 빠지면 카메라를 보관 객체에 남긴 뒤 정리
    DisposableEffect(lifecycle) {
        val forwarder = MapViewLifecycle(mapView)
        val observer = LifecycleEventObserver { _, event -> forwarder.on(event) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (cameraSettled) map?.cameraPosition?.target?.let { target ->
                holder.saveCamera(MapCamera(MapCoord(lng = target.longitude, lat = target.latitude), map?.cameraPosition?.zoom ?: 0.0))
            }
            forwarder.destroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        ownPoint?.let { (x, y) ->
            Surface(modifier = Modifier.offset { IntOffset(x.toInt(), y.toInt()) }.size(Dimens.MinTouchTarget / 3), shape = CircleShape, color = MaterialTheme.colorScheme.tertiary) {}
        }
        val selected = mapUi.selectedPlaceNo
        markers.forEach { marker ->
            MapMarkerView(
                marker = marker,
                selected = selected != null && selected in marker.placeNos,
                onClick = {
                    if (marker.isCluster) latestCallbacks.onSelectCluster(marker.placeNos) else latestCallbacks.onSelectPlace(marker.placeNos.single())
                },
            )
        }
    }
}

/** 화면 부품이 보관 객체를 받음(Hilt) — Hilt 앱이 아니면(JVM 화면 시험) null → 지도 못 그림 한 줄. */
@Composable
private fun rememberRecommendationMapHolder(): RecommendationMapHolder? {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(context, RecommendationMapEntryPoint::class.java).recommendationMapHolder()
        }.getOrNull()
    }
}

/** 낱개 핀(장소) · 숫자 묶음 핀 — 화면 위치는 지도 렌더러 화면 픽셀 그대로. 터치 칸 48dp 이상. */
@Composable
private fun MapMarkerView(marker: MapMarker, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val size = if (marker.isCluster) Dimens.RecoMapClusterSize else Dimens.RecoMapPinSize
    val description = if (marker.isCluster) {
        stringResource(R.string.reco_map_cluster_desc, marker.placeNos.size)
    } else {
        stringResource(R.string.reco_map_pin_desc)
    }
    val half = with(LocalDensity.current) { (size / 2).roundToPx() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .offset { IntOffset(marker.x.toInt() - half, marker.y.toInt() - half) }
            .size(size)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = if (selected || marker.isCluster) colors.secondary else colors.primary,
        contentColor = if (selected || marker.isCluster) colors.onSecondary else colors.onPrimary,
        border = BorderStroke(if (selected) Dimens.RecoMapPinSelectedBorder else Dimens.RecoMapPinBorder, colors.surface),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (marker.isCluster) {
                Text(text = marker.placeNos.size.toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** MapLibre 시작 · MapView 만들기 — 네이티브 라이브러리를 못 읽는 기기면 null(지도 못 그림 · N2). */
private fun createMapView(context: Context): MapView? = runCatching {
    MapLibre.getInstance(context)
    // 지도는 앱 자산만 — 렌더러가 네트워크를 쓰지 않게 끔(M31)
    MapLibre.setConnected(false)
    MapView(context).also { it.onCreate(null) }
}.getOrNull()

private fun fitPaddingPx(view: View): Int =
    (Dimens.RecoMapFitPaddingFraction * minOf(view.width, view.height)).toInt()

private fun MapBounds.toLatLngBounds(): LatLngBounds =
    LatLngBounds.from(north, east, south, west)

/** MapView 수명 전달 — 같은 단계를 두 번 부르지 않게 지금 단계를 기억. */
private class MapViewLifecycle(private val view: MapView) {
    private var started = false
    private var resumed = false
    private var destroyed = false

    fun on(event: Lifecycle.Event) {
        if (destroyed) return
        when (event) {
            Lifecycle.Event.ON_START -> if (!started) { view.onStart(); started = true }
            Lifecycle.Event.ON_RESUME -> if (!resumed) { view.onResume(); resumed = true }
            Lifecycle.Event.ON_PAUSE -> if (resumed) { view.onPause(); resumed = false }
            Lifecycle.Event.ON_STOP -> if (started) { view.onStop(); started = false }
            else -> Unit
        }
    }

    fun destroy() {
        if (destroyed) return
        if (resumed) view.onPause()
        if (started) view.onStop()
        view.onDestroy()
        destroyed = true
    }
}

private const val FOCUS_ANIMATION_MILLIS = 600
