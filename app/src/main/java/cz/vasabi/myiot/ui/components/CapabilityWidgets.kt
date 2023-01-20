package cz.vasabi.myiot.ui.components

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cz.vasabi.myiot.SingleState
import cz.vasabi.myiot.backend.api.Data
import cz.vasabi.myiot.backend.connections.DeviceCapabilityState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class BoolWidgetType {
    Switch,
    Button
}

enum class StringWidgetType {
    Realtime,
    Form
}

@Composable
fun BoolWidget(capability: DeviceCapabilityState, bottomDrawerRegister: (@Composable ColumnScope.() -> Unit) -> Unit) {
    var selectedStyle by rememberSaveable {
        mutableStateOf(BoolWidgetType.Switch)
    }

    LaunchedEffect(null) {
        bottomDrawerRegister {
            Button(onClick = { selectedStyle = BoolWidgetType.Button }) {
                Text(text = "button")
            }
            Button(onClick = { selectedStyle = BoolWidgetType.Switch }) {
                Text(text = "switch")
            }
        }
    }

    val scope = rememberCoroutineScope()
    val inputChannel = remember {
        Channel<Boolean>()
    }

    val interactionSource = remember {
        MutableInteractionSource()
    }

    val isChecked by remember {
        merge(capability.responses.receiveAsFlow().map {
            when (it) {
                is Data.B -> {
                    SingleState.events.add("received new state pog $it")
                    Log.d(TAG, "received new state pog $it")
                    it.b
                }

                else -> TODO()
            }
        }, inputChannel.receiveAsFlow(), interactionSource.interactions.mapNotNull {
            return@mapNotNull when (it) {
                is PressInteraction.Press -> {
                    scope.launch {
                        capability.setValue(Data.B(true))
                    }
                    true
                }

                is PressInteraction.Release -> {
                    scope.launch {
                        capability.setValue(Data.B(false))
                    }
                    false
                }

                is PressInteraction.Cancel -> {
                    scope.launch {
                        capability.setValue(Data.B(false))
                    }
                    false
                }

                else -> null
            }
        })
    }.collectAsState(initial = false)

    when (selectedStyle) {
        BoolWidgetType.Switch -> {
            Switch(checked = isChecked, onCheckedChange = {
                scope.launch {
                    inputChannel.send(it)
                }
                capability.setValue(Data.B(it))
            })
        }

        BoolWidgetType.Button -> {
            LaunchedEffect(key1 = interactionSource) {
                capability.setValue(Data.B(isChecked))
                scope.launch {
                    inputChannel.send(isChecked)
                }
            }

            Button(interactionSource = interactionSource, onClick = {}) {
                Text("$isChecked")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringWidget(capability: DeviceCapabilityState, bottomDrawerRegister: (@Composable ColumnScope.() -> Unit) -> Unit) {
    val inputFlow = Channel<String>()

    val scope = rememberCoroutineScope()

    var selectedStyle by rememberSaveable {
        mutableStateOf(StringWidgetType.Form)
    }

    LaunchedEffect(null) {
        bottomDrawerRegister {
            Button(onClick = {
                selectedStyle = StringWidgetType.Realtime
            }) {
                Text(text = "realtime")
            }
            Button(onClick = {
                selectedStyle = StringWidgetType.Form
            }) {
                Text(text = "form style")
            }
        }
    }

    var unCommited by rememberSaveable {
        mutableStateOf("")
    }

    val value by remember {
        merge(capability.responses.receiveAsFlow().map {
            when (it) {
                is Data.S -> it.s
                else -> TODO()
            }
        }, inputFlow.receiveAsFlow())
    }.collectAsState(initial = "")

    when (selectedStyle) {
        StringWidgetType.Realtime -> {
            OutlinedTextField(value = unCommited, onValueChange = {
                unCommited = it
                capability.setValue(Data.S(it))
                scope.launch {
                    inputFlow.send(it)
                }
            }, textStyle = TextStyle(color = Color.White))
        }
        StringWidgetType.Form -> {
            formTextField(text = unCommited, onValueChanged = {unCommited = it}) {
                scope.launch {
                    capability.setValue(Data.S(unCommited))
                }
            }
            Text(text = "value $value", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun IntWidget(capability: DeviceCapabilityState, bottomDrawerRegister: (@Composable ColumnScope.() -> Unit) -> Unit) {
    val value by remember {
        capability.responses.receiveAsFlow().map {
            when (it) {
                is Data.I -> it.i
                else -> TODO()
            }
        }
    }.collectAsState(initial = 0)
    var unCommited by rememberSaveable {
        mutableStateOf("")
    }

    formTextField(modifier = Modifier.fillMaxWidth(), unCommited, {
        unCommited = it
    }) {
        val num = unCommited.toIntOrNull() ?: return@formTextField
        capability.setValue(Data.I(num))
    }
    Text(text = "value $value")
    Spacer(modifier = Modifier.height(6.dp))
    if (capability.readings.size > 2) {
        Chart(
            readings = capability.readings, timeMilis = 60_000, modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.secondary
        )
    }
}