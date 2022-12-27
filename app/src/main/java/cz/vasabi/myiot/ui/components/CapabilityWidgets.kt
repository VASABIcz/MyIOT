package cz.vasabi.myiot.ui.components

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
    Button
}

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun BoolWidget(capability: DeviceCapabilityState) {
    val scope = rememberCoroutineScope()
    var showEdit by rememberSaveable {
        mutableStateOf(false)
    }
    val inputChannel = remember {
        Channel<Boolean>()
    }

    var selectedStyle by rememberSaveable {
        mutableStateOf(BoolWidgetType.Switch)
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
                is PressInteraction.Press -> true
                is PressInteraction.Release -> false
                is PressInteraction.Cancel -> false
                else -> null
            }
        })
    }.collectAsState(initial = false)

    if (showEdit) {
        AlertDialog({
            showEdit = false
        }, {
            Text(text = "UwU")
        }, Modifier, {
            Text(text = "UwU")
        }, {
            Text(text = "Widget Style")
        }, {
            Column {
                Button(onClick = {
                    selectedStyle = BoolWidgetType.Switch
                }) {
                    Text("switch")
                }
                Button(onClick = {
                    selectedStyle = BoolWidgetType.Button
                }) {
                    Text("button")
                }
            }
        })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.Text(capability.name)
        androidx.compose.material3.Text(capability.description)

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
                }

                Button(interactionSource = interactionSource, onClick = {}) {
                    Text("$isChecked")
                }
            }
        }

        Button(
            onClick = {
                showEdit = true
            }, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "edit")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun StringWidget(capability: DeviceCapabilityState) {
    val inputFlow = Channel<String>()

    val scope = rememberCoroutineScope()
    var showEdit by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedStyle by rememberSaveable {
        mutableStateOf(StringWidgetType.Button)
    }


    var unCommited by rememberSaveable {
        mutableStateOf("")
    }


    val value = remember {
        merge(capability.responses.receiveAsFlow().map {
            when (it) {
                is Data.S -> it.s
                else -> TODO()
            }
        }, inputFlow.receiveAsFlow())
    }.collectAsState(initial = "")

    if (showEdit) {
        AlertDialog({
            showEdit = false
        }, {
            Text(text = "UwU")
        }, Modifier, {
            Text(text = "UwU")
        }, {
            Text(text = "Widget Style")
        }, {
            Column {
                StringWidgetType.values().forEach {
                    Button(onClick = {
                        selectedStyle = it
                    }) {
                        Text(it.toString())
                    }
                }
            }
        })
    }

    androidx.compose.material3.Text(capability.name)
    androidx.compose.material3.Text(capability.description)


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
        StringWidgetType.Button -> {
            OutlinedTextField(
                value = unCommited,
                onValueChange = { unCommited = it },
                textStyle = TextStyle(color = Color.White)
            )
            Button(onClick = {
                scope.launch {
                    capability.setValue(Data.S(unCommited))
                }
            }) {
                androidx.compose.material3.Text(text = "update")
            }
            Text(text = "value $value", color = MaterialTheme.colorScheme.onBackground)
        }
    }

    Button(onClick = {
        showEdit = true
    }) {
        Icon(Icons.Filled.Edit, contentDescription = "edit")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntWidget(capability: DeviceCapabilityState) {
    var value by rememberSaveable {
        mutableStateOf(0)
    }
    var unCommited by rememberSaveable {
        mutableStateOf("")
    }

    LaunchedEffect(null) {
        capability.responses.receiveAsFlow().collect {
            when (it) {
                is Data.I -> value = it.i
                else -> TODO()
            }
        }
    }

    Text(capability.name)
    Text(capability.description)

    OutlinedTextField(
        value = unCommited,
        onValueChange = { unCommited = it },
        textStyle = TextStyle(color = Color.White)
        // colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
    )
    Button(onClick = {
        val num = unCommited.toIntOrNull() ?: return@Button
        capability.setValue(Data.I(num))
    }) {
        androidx.compose.material3.Text(text = "update")
    }
    androidx.compose.material3.Text(text = "value $value")
}