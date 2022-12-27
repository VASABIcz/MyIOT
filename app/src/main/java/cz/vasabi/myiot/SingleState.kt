package cz.vasabi.myiot

import androidx.compose.runtime.snapshots.SnapshotStateList

object SingleState {
    val events = SnapshotStateList<String>()
}