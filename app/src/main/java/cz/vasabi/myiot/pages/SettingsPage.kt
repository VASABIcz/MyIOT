package cz.vasabi.myiot.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
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