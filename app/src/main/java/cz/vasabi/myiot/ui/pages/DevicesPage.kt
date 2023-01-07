package cz.vasabi.myiot.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.vasabi.myiot.backend.connections.Device
import cz.vasabi.myiot.backend.connections.DeviceCapabilityState
import cz.vasabi.myiot.backend.connections.DeviceConnectionState
import cz.vasabi.myiot.backend.connections.DeviceState
import cz.vasabi.myiot.ui.components.BoolWidget
import cz.vasabi.myiot.ui.components.DrawConnectionState
import cz.vasabi.myiot.ui.components.IntWidget
import cz.vasabi.myiot.ui.components.SelectableButton
import cz.vasabi.myiot.ui.components.StringWidget
import cz.vasabi.myiot.viewModels.DevicesViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesPage(viewModel: DevicesViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn {
                    items(viewModel.devices.values.toList()) {
                        DrawDevice(it, {
                            viewModel.selectedDevice.value = it
                            scope.launch {
                                viewModel.drawerState.close()
                            }
                        })
                    }
                }
            }
        },
        // FIXME drawerBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        drawerState = viewModel.drawerState
    ) {
        DevicesPageMain()
    }
}

@Composable
fun DrawDevice(device: DeviceState, onCLick: () -> Unit, viewModel: DevicesViewModel = viewModel()) {
    Box(modifier = Modifier.padding(6.dp)) {
        SelectableButton(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            onCLick = {
                viewModel.selectedDevice.value = device
                onCLick()
            },
            isSelected = device == viewModel.selectedDevice.value
        ) {
            DrawConnectionState(
                connectionState = device.connectionState, modifier = Modifier
                    .size(26.dp)
                    .padding(5.dp)
            )
            // Icon(Icons.Default.AddCircle, "online status", tint = connected)
            Column {
                Text(device.name)
                Text(text = ("id: " + device.identifier))
            }
        }
    }
}

class MockDevice : Device {
    override val name: String = "device name"
    override val description: String? = "very interesting device description"
    override val identifier: String = "unqiue-identifier"

}

class MockDeviceState(device: Device) : DeviceState(device) {

}

@Composable
@Preview
fun PreviewDrawDevice() {
    val device = MockDeviceState(MockDevice())
    Column {
        repeat(5) {
            Box(modifier = Modifier.padding(6.dp)) {
                SelectableButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    onCLick = {
                    },
                    isSelected = false
                ) {
                    DrawConnectionState(
                        connectionState = device.connectionState, modifier = Modifier
                            .size(26.dp)
                            .padding(5.dp)
                    )
                    // Icon(Icons.Default.AddCircle, "online status", tint = connected)
                    Column {
                        Text(device.name)
                        Text(text = ("id: " + device.identifier))
                    }
                }
            }
        }
    }
}

@Composable
fun drawCapability(capability: DeviceCapabilityState) {
    // FIXME it runs on activity restart
    LaunchedEffect(null) {
        capability.requestValue()
    }
    Column {
        when (capability.type) {
            "bool" -> {
                BoolWidget(capability)
            }

            "int" -> {
                IntWidget(capability)
            }

            "string" -> {
                StringWidget(capability)
            }
        }
    }
}

@Composable
fun DevicesPageMain(viewModel: DevicesViewModel = viewModel()) {
    if (viewModel.selectedDevice.value == null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "no device selected")
        }
        return
    }
    if (viewModel.selectedDevice.value == null) {
        Text(text = "No devices added try adding some")
    }
    viewModel.selectedDevice.value?.let { device ->
        LazyColumn {
            items(device.connections.values.toList()) {
                DrawConnection(it, {})
            }
        }
    }
}

@Composable
fun DrawConnection(
    conn: DeviceConnectionState,
    onConfigure: () -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier
            .padding(7.dp)
            .clip(RoundedCornerShape(7.dp)),
        //elevation = CardDefaults.cardElevation(10.dp,10.dp,10.dp,10.dp,10.dp,10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfigure()
                        },
                        onPress = {
                            awaitRelease()
                        },
                        onTap = {
                            conn.invertShow()
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawConnectionState(
                    connectionState = conn.connected.value, modifier = Modifier
                        .size(26.dp)
                        .padding(5.dp)
                )
                Text(
                    text = "${conn.connectionType} connection, ${conn.deviceCapabilities.size} capabilities",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Button(onClick = { conn.updateCapabilities() }) {
                Icon(Icons.Default.Refresh, contentDescription = "refresh capabilities")
            }
        }
        AnimatedVisibility(conn.drawCapabilities.value) {
            Column {
                conn.deviceCapabilities.forEach {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        drawCapability(it)
                    }
                }
            }
        }
    }
    Divider()
}