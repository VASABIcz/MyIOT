package cz.vasabi.myiot.ui.pages

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TestPage() {
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded)
    ModalBottomSheetLayout(
        sheetContent = {
            Button(onClick = { }) {
                Text(text = "hello")
            }
            Button(onClick = { }) {
                Text(text = "UwU")
            }
            Button(onClick = { }) {
                Text(text = "AraAra")
            }
        },
        sheetState = sheetState
    ) {
        val scope = rememberCoroutineScope()
        Button(onClick = {
            scope.launch {
                sheetState.show()
            }
        }) {

        }
    }
}