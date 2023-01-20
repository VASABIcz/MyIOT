package cz.vasabi.myiot.viewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.vasabi.myiot.SingleState
import cz.vasabi.myiot.backend.connections.DeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor() : ViewModel() {
    private var lastJob: Job? = null

    val filteredList = mutableStateListOf<String>()

    init {
        filteredList.addAll(SingleState.events)
    }

    fun filter(term: String) {
        lastJob?.cancel()
        lastJob = viewModelScope.launch(Dispatchers.Default) {
            val regX = try {
                Regex(term)
            } catch (_: Throwable) {
                return@launch
            }
            filteredList.clear()
            filteredList.addAll(SingleState.events.filter {
                it.contains(regX)
            })
        }
    }
}