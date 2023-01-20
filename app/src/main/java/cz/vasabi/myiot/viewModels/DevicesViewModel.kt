package cz.vasabi.myiot.viewModels

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.vasabi.myiot.backend.connections.DeviceManager
import cz.vasabi.myiot.backend.connections.DeviceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(private val deviceManager: DeviceManager): ViewModel() {

    // TODO query db for last selected device
    var selectedDevice = mutableStateOf(deviceManager.devices.values.firstOrNull())

    @OptIn(ExperimentalMaterial3Api::class)
    val sideDrawerState = DrawerState(DrawerValue.Closed)

    val bottomDrawerEnabled = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    val bDrawerState = BottomDrawerState(
        initialValue = BottomDrawerValue.Closed,
        confirmStateChange = {
            when (it) {
                BottomDrawerValue.Closed -> {
                    bottomDrawerEnabled.value = false
                }
                else -> {}
            }
            true
        }
    )

    val bottomDrawerContent: MutableState<@Composable ColumnScope.() -> Unit> = mutableStateOf({Text("")})

    @OptIn(ExperimentalMaterialApi::class)
    val bottomSheetState = ModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        animationSpec = SwipeableDefaults.AnimationSpec,
        isSkipHalfExpanded = true,
        confirmStateChange = { request ->
            true
        }
    )

    @OptIn(ExperimentalMaterial3Api::class)
    fun closeSideDrawer() {
        viewModelScope.launch {
            sideDrawerState.close()
        }
    }

    val devices: SnapshotStateMap<String, DeviceState>
        get() = deviceManager.devices

    @OptIn(ExperimentalMaterialApi::class)
    suspend fun openBottomSheet(body: @Composable ColumnScope.() -> Unit) {
        bottomDrawerContent.value = body
        bottomDrawerEnabled.value = true
        // bottomSheetState.show()
        bDrawerState.open()
    }

    @OptIn(ExperimentalMaterialApi::class)
    suspend fun closeBottomSheet() {
        // bottomSheetState.hide()
        bDrawerState.close()
    }
}