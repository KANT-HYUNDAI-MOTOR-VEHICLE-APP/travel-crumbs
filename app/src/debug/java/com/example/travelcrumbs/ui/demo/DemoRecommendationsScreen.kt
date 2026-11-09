package com.example.travelcrumbs.ui.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.travelcrumbs.R
import java.util.Locale

@Composable
internal fun DemoRecommendationsScreen(
    state: DemoUiState,
    demo: DemoViewModel,
    canStartItinerary: Boolean,
    onStart: (DemoRoadmap) -> Unit,
    itineraryCard: @Composable () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val roadmaps = demoRoadmaps + state.sharedRoadmaps
    val selected = roadmaps.firstOrNull { it.id == selectedId }
    val backToRankings = {
        selectedId = null
    }
    BackHandler(enabled = selected != null, onBack = backToRankings)
    if (selected != null) {
        val firstStop = selected.stops.first()
        DemoPage(selected.title, "${selected.city} ${selected.district} · ${selected.ratingText(state)}") {
            DemoAction("랭킹으로 돌아가기", backToRankings, primary = false)
            itineraryCard()
            DemoCard {
                DemoTitle("로드맵 · 장소 ${selected.stops.size}곳")
                DemoRoute(selected.stops)
                if (selected.tag.isNotBlank()) Text("태그 · ${selected.tag}")
                if (selected.memo.isNotBlank()) Text(selected.memo)
                if (canStartItinerary) {
                    Text(stringResource(R.string.demo_navi_first_stop, firstStop.name))
                    if (selected.stops.all { it.navigationTarget != null }) {
                        DemoAction(stringResource(R.string.demo_navi_start), { onStart(selected) })
                    } else {
                        Text(stringResource(R.string.demo_navi_no_destination))
                    }
                }
            }
            DemoCard {
                DemoTitle("이 로드맵에 별점 주기")
                if (selected.owner == state.account) {
                    Text("내 로드맵에는 별점을 줄 수 없어요.")
                } else {
                    val rating = state.ratings[state.account to selected.id]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { stars ->
                            FilterChip(selected = rating == stars, onClick = { demo.rate(selected.id, stars) }, label = { Text("$stars 점") })
                        }
                    }
                }
            }
        }
    } else {
        DemoPage("추천", "시 · 도와 시 · 군 · 구를 고르면 별점이 높은 로드맵부터 볼 수 있어요.") {
            itineraryCard()
            DemoCard {
                DemoTitle("1. 시 · 도 고르기")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    demoRegions.keys.forEach { city ->
                        FilterChip(selected = state.city == city, onClick = { demo.selectCity(city) }, label = { Text(city) })
                    }
                }
                state.city?.let { city ->
                    DemoTitle("2. 시 · 군 · 구 고르기")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        demoRegions.getValue(city).forEach { district ->
                            FilterChip(selected = state.district == district, onClick = { demo.selectDistrict(district) }, label = { Text(district) })
                        }
                    }
                }
            }
            state.district?.let { district ->
                DemoTitle("$district 로드맵 랭킹")
                roadmaps.filter { it.city == state.city && it.district == district }
                    .sortedByDescending { it.averageRating(state) }
                    .forEachIndexed { index, roadmap ->
                        DemoCard {
                            DemoTitle("${index + 1}위 · ${roadmap.title}")
                            Text("${roadmap.ratingText(state)} · 장소 ${roadmap.stops.size}곳")
                            DemoAction("${roadmap.title} 상세 보기", { selectedId = roadmap.id }, primary = false)
                        }
                    }
            }
        }
    }
}

private fun DemoRoadmap.userRatings(state: DemoUiState) = state.ratings.filterKeys { it.second == id }.values
private fun DemoRoadmap.averageRating(state: DemoUiState): Double {
    val added = userRatings(state)
    val total = count + added.size
    return if (total == 0) 0.0 else (stars * count + added.sum()) / total
}

private fun DemoRoadmap.ratingText(state: DemoUiState): String {
    val total = count + userRatings(state).size
    return if (total == 0) "별점 없음" else "★ ${String.format(Locale.KOREA, "%.1f", averageRating(state))} ($total)"
}
