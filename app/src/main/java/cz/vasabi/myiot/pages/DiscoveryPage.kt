package cz.vasabi.myiot.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cz.vasabi.myiot.backend.DeviceInfo
import cz.vasabi.myiot.viewModels.DiscoverViewModel

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun DiscoverPage(viewModel: DiscoverViewModel = hiltViewModel()) {
    val isRefreshing by remember {
        mutableStateOf(false)
    }
    var shouldFetch by remember {
        mutableStateOf(false)
    }

    val refreshState = rememberPullRefreshState(isRefreshing, {
        shouldFetch = !shouldFetch
    })

    Box(modifier = Modifier
        .fillMaxSize()
        .pullRefresh(refreshState)
    ) {
        PullRefreshIndicator(refreshing = isRefreshing, state = refreshState)
        LazyColumn {
            itemsIndexed(viewModel.devices) { i, it ->
                DiscoveredDevice(it) {
                    viewModel.addDevice(it)
                }
            }
        }
    }
}

@Composable
fun DiscoveredDevice(info: DeviceInfo, onAdd: (DeviceInfo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(60.dp)
            .padding(7.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .padding(6.dp)
        ) {
            Text(info.name, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(info.transportLayerInfo, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        OutlinedButton(
            onClick = {
                onAdd(info) }, colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )) {
            Text(text = "Add")
        }
    }
}