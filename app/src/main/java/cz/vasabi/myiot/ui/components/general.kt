package cz.vasabi.myiot.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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