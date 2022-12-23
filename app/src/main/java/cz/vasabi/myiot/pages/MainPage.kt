package cz.vasabi.myiot.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class NavigationItem(var route: String, var icon: ImageVector, var title: String) {
    object Settings: NavigationItem("settings", Icons.Filled.Settings, "Settings")
    object Discover: NavigationItem("discover", Icons.Filled.Add, "Discover")
    object Devices: NavigationItem("devices", Icons.Filled.Phone, "Devices")
    object Debug: NavigationItem("debug", Icons.Filled.Info, "Logs")

    companion object {
        val pages = listOf(Devices, Discover, Debug, Settings)
        var selected = mutableStateOf(pages[0])
    }
}

@Composable
fun BottomNavigationBar(nav: NavController) {
    val items = NavigationItem.pages

    BottomNavigation(
        backgroundColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            val isSelected = item == NavigationItem.selected.value
            BottomNavigationItem(
                alwaysShowLabel = false,
                icon = {
                    Icon(item.icon, contentDescription = item.title, tint = if (!isSelected) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.onPrimary)
                },
                label = {
                    Text(text = item.title, color = if (!isSelected) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.onPrimary)
                },
                selected = isSelected,
                onClick = {
                    nav.navigate(item.route) {
                        launchSingleTop = true
                        popUpTo(NavigationItem.selected.value.route) {
                            inclusive = true
                        }
                    }
                    NavigationItem.selected.value = item
                },
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                unselectedContentColor = MaterialTheme.colorScheme.inversePrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(nav: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(nav)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.padding(it)) {
            NavHost(navController = nav, startDestination = "devices") {
                composable("devices") {
                    DevicesPage()
                }
                composable("discover") {
                    DiscoverPage()
                }
                composable("settings") {
                    SettingsPage()
                }
                composable("debug") {
                    DebugPage()
                }
            }
        }
    }
}