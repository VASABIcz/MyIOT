package cz.vasabi.myiot

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object SingleState {
    val events = SnapshotStateList<String>()
}