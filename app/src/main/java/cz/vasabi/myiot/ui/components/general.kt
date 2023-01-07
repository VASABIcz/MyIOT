package cz.vasabi.myiot.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.vasabi.myiot.backend.connections.ConnectionState
import kotlin.math.min

@Composable
fun SelectableButton(
    modifier: Modifier,
    onCLick: () -> Unit,
    isSelected: Boolean,
    body: @Composable RowScope.() -> Unit
) {
    if (isSelected) {
        Button(
            modifier = modifier,
            onClick = onCLick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            content = body
        )
    } else {
        OutlinedButton(
            modifier = modifier,
            onClick = onCLick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            content = body
        )
    }
}

@Composable
fun DrawConnectionState(modifier: Modifier = Modifier, connectionState: ConnectionState) {
    val primaryPaint = Paint()
    val secondaryPaint = Paint()

    when (connectionState) {
        ConnectionState.Connected -> {
            primaryPaint.color = android.graphics.Color.GREEN
            secondaryPaint.color = android.graphics.Color.DKGRAY
        }

        ConnectionState.Disconnected -> {
            primaryPaint.color = android.graphics.Color.GRAY
            secondaryPaint.color = android.graphics.Color.DKGRAY
        }

        ConnectionState.Loading -> {
            primaryPaint.color = android.graphics.Color.YELLOW
            secondaryPaint.color = android.graphics.Color.DKGRAY
        }
    }

    Canvas(modifier = modifier.size(10.dp)) {
        val s = min(size.height, size.width)
        drawContext.canvas.nativeCanvas.apply {
            drawCircle(center.x, center.y, s / 2, primaryPaint)
            drawCircle(center.x, center.y, s / 3, secondaryPaint)
        }
    }
}

@Preview
@Composable
fun previewConnectionStyle() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        DrawConnectionState(connectionState = ConnectionState.Disconnected, modifier = Modifier)
        DrawConnectionState(connectionState = ConnectionState.Connected)
        DrawConnectionState(connectionState = ConnectionState.Loading)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun formTextField(
    modifier: Modifier = Modifier,
    text: String,
    onValueChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = onValueChanged,
        trailingIcon = {
            Icon(
                Icons.Filled.Send,
                contentDescription = "send button",
                modifier = Modifier.clickable {
                    onSend()
                })
        })
}

@Preview
@Composable
fun PreviewformTextField() {
    var text by remember {
        mutableStateOf("")
    }
    formTextField(text = text, onValueChanged = { text = it }) {

    }
}