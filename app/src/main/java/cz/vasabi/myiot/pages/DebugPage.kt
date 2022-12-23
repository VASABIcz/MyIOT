package cz.vasabi.myiot.pages

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cz.vasabi.myiot.SingleState

@Composable
fun DebugPage() {
    LazyColumn {
        items(SingleState.events) { row ->
            Text(row)
        }
    }
}