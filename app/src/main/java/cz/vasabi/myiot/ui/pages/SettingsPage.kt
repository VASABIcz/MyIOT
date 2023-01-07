package cz.vasabi.myiot.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import cz.vasabi.myiot.viewModels.SettingsViewModel

@Composable
fun SettingsPage(viewModel: SettingsViewModel = hiltViewModel()) {
    Column {
        Button(onClick = {
            viewModel.clearDatabase()
        }) {
            Text(text = "wipe Database")
        }
    }
}