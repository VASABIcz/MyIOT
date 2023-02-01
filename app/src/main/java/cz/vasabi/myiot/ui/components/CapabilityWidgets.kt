package cz.vasabi.myiot.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cz.vasabi.myiot.backend.connections.DeviceCapabilityState
import kotlinx.coroutines.channels.Channel
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
            Column(Modifier.padding(4.dp)) {
                Text(text = "interaction type")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedStyle == BoolWidgetType.Switch,
                        onClick = { selectedStyle = BoolWidgetType.Switch },
                    )
                    Text(text = "Switch")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedStyle == BoolWidgetType.Button,
                        onClick = { selectedStyle = BoolWidgetType.Button },
                    )
                    Text(text = "Button")
                }
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
        merge(capability.responses.receiveAsFlow().mapNotNull {
            it.getBool()
        }, inputChannel.receiveAsFlow(), interactionSource.interactions.mapNotNull {
            return@mapNotNull when (it) {
                is PressInteraction.Press -> {
                    scope.launch {
                        capability.setValue(true, "bool")
                    }
                    true
                }

                is PressInteraction.Release -> {
                    scope.launch {
                        capability.setValue(false, "bool")
                    }
                    false
                }

                is PressInteraction.Cancel -> {
                    scope.launch {
                        capability.setValue(false, "bool")
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
                capability.setValue(it, "bool")
            })
        }

        BoolWidgetType.Button -> {
            LaunchedEffect(key1 = interactionSource) {
                capability.setValue(isChecked, "bool")
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
        merge(capability.responses.receiveAsFlow().mapNotNull {
            it.getString()
        }, inputFlow.receiveAsFlow())
    }.collectAsState(initial = "")

    when (selectedStyle) {
        StringWidgetType.Realtime -> {
            OutlinedTextField(value = unCommited, onValueChange = {
                unCommited = it
                capability.setValue(it, "string")
                scope.launch {
                    inputFlow.send(it)
                }
            }, textStyle = TextStyle(color = Color.White))
        }
        StringWidgetType.Form -> {
            formTextField(text = unCommited, onValueChanged = {unCommited = it}) {
                scope.launch {
                    capability.setValue(unCommited, "string")
                }
            }
            Text(text = "value $value", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun IntWidget(capability: DeviceCapabilityState, bottomDrawerRegister: (@Composable ColumnScope.() -> Unit) -> Unit) {
    var showGraph by rememberSaveable {
        mutableStateOf(false)
    }

    val value by remember {
        capability.responses.receiveAsFlow().mapNotNull {
            it.getInt()
        }
    }.collectAsState(initial = 0)
    var unCommited by rememberSaveable {
        mutableStateOf("")
    }

    LaunchedEffect(null) {
        bottomDrawerRegister {
            Column(Modifier.padding(4.dp)) {
                Text(text = "interaction type")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = false,
                        onClick = { },
                    )
                    Text(text = "realtime")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = true,
                        onClick = { },
                    )
                    Text(text = "form-style")
                }
                Divider()
                Text(text = "graph")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = showGraph, onCheckedChange = { showGraph = !showGraph })
                    Text(text = "show graph")
                }
            }
        }
    }

    formTextField(modifier = Modifier.fillMaxWidth(), unCommited, {
        unCommited = it
    }) {
        val num = unCommited.toIntOrNull() ?: return@formTextField
        capability.setValue(num, "int")
    }
    Text(text = "value $value")
    Spacer(modifier = Modifier.height(6.dp))
    if (capability.readings.size > 2 && showGraph) {
        /* FIXME
        Chart(
            readings = capability.readings, timeMilis = 60_000, modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.secondary
        )

         */
    }
}