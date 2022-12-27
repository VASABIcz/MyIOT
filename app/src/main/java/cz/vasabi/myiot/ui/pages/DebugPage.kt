package cz.vasabi.myiot.ui.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import cz.vasabi.myiot.viewModels.DebugViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPage(viewModel: DebugViewModel = hiltViewModel()) {
    var search by remember {
        mutableStateOf("")
    }

    Scaffold(
        bottomBar = {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    viewModel.filter(search)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(viewModel.filteredList) { row ->
                Text(row)
            }
        }
    }
}