package cz.vasabi.myiot.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.rememberDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.vasabi.myiot.BoolWidget
import cz.vasabi.myiot.IntWidget
import cz.vasabi.myiot.StringWidget
import cz.vasabi.myiot.backend.ConnectionState
import cz.vasabi.myiot.backend.DeviceCapability
import cz.vasabi.myiot.backend.DeviceCapabilityState
import cz.vasabi.myiot.backend.DeviceConnectionState
import cz.vasabi.myiot.backend.DeviceState
import cz.vasabi.myiot.viewModels.DevicesViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
fun DevicesPage(viewModel: DevicesViewModel = hiltViewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    ModalDrawer(
        drawerContent = {
            LazyColumn {
                items(viewModel.devices.values.toList()) {
                    DrawDevice(it, {
                        viewModel.selectedDevice.value = it
                    })
                }
            }
        },
        drawerBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        drawerState = drawerState
    ) {
        DevicesPageMain()
    }
}

@Composable
fun DrawDevice(device: DeviceState, onCLick: () -> Unit, viewModel: DevicesViewModel = viewModel()) {
    val connected = if (device.connections.any { it.value.connected.value == ConnectionState.Connected }) {
        Color.Green
    } else if (device.connections.any { it.value.connected.value == ConnectionState.Connected }) {
        Color.Yellow
    } else {
        Color.DarkGray
    }

    Box(modifier = Modifier.padding(6.dp)) {
        if (device == viewModel.selectedDevice.value) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                onClick = {
                    viewModel.selectedDevice.value = device
                    onCLick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row {
                    Icon(Icons.Default.AddCircle, "online status", tint = connected)
                    Column {
                        Text(device.name)
                        Text(text = ("id: " + device.name))
                    }
                }
            }
        }
        else {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                onClick = {
                    onCLick()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
            ) {
                Row {
                    Icon(Icons.Default.AddCircle, "online status", tint = connected)
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
    LaunchedEffect(null) {
        capability.requestValue()
    }

    Column(modifier = Modifier.padding(7.dp)) {
        when (capability.type) {
            "bool" -> {
                BoolWidget(capability = capability)
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
        Text(text = "no device selected")
        return
    }

    viewModel.selectedDevice.value?.let { device ->
        LazyColumn {
            items(device.connections.values.toList()) {
                DrawConnection(it)
            }
        }
    }
}

@Composable
fun DrawConnection(conn: DeviceConnectionState) {
    val t = when (conn.connected.value) {
        ConnectionState.Connected -> {
            Color.Green
        }
        ConnectionState.Disconnected -> {
            Color.DarkGray
        }
        ConnectionState.Loading -> {
            Color.Yellow
        }
    }

    Column {
        Row {
            Icon(Icons.Default.AddCircle, "online status", tint = t)
            Text(text = "${conn.connectionType} ${conn.deviceCapabilities.size}")
        }
        conn.deviceCapabilities.forEach {
            drawCapability(it)
        }
    }
    Divider()
}