package com.example.travelcrumbs.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcrumbs.data.demo.DemoMapPack
import com.example.travelcrumbs.data.demo.DemoMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface DemoMapState {
    data object Loading : DemoMapState
    data object Unavailable : DemoMapState
    data class Ready(val maps: DemoMapPack) : DemoMapState
}

@HiltViewModel
internal class DemoMapViewModel @Inject constructor(repository: DemoMapRepository) : ViewModel() {
    private val state = MutableStateFlow<DemoMapState>(DemoMapState.Loading)
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            state.value = try {
                DemoMapState.Ready(repository.load())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                DemoMapState.Unavailable
            }
        }
    }
}
