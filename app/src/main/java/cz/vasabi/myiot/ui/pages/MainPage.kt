package cz.vasabi.myiot.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class NavigationItem(var route: String, var icon: ImageVector, var title: String) {

    object Settings : NavigationItem("settings", Icons.Default.Settings, "Settings")
    object Discover : NavigationItem("discover", Icons.Default.Hub, "Discover")
    object Devices : NavigationItem("devices", Icons.Default.DevicesOther, "Devices")
    object Debug : NavigationItem("debug", Icons.Default.Info, "Logs")

    companion object {
        val pages = listOf(Devices, Discover, Debug, Settings)
        var selected = mutableStateOf(pages[0])
    }
}

@Composable
fun BottomNavigationBar(nav: NavController) {
    val items = NavigationItem.pages
    val haptic = LocalHapticFeedback.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            val isSelected = item == NavigationItem.selected.value
            NavigationBarItem(
                alwaysShowLabel = false,
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        /*
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.5f).clip(
                                RoundedCornerShape(40)).background(Color.Magenta))
                        }

                         */
                        Icon(
                            item.icon,
                            contentDescription = item.title,
                            tint = if (!isSelected) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.clip(RectangleShape)
                        )
                    }
                },
                label = {
                    Text(text = item.title, color = if (!isSelected) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.onPrimary)
                },
                selected = isSelected,
                onClick = {
                    if (nav.currentDestination?.route == item.route) return@NavigationBarItem
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    nav.navigate(item.route) {
                        launchSingleTop = true
                        popUpTo(NavigationItem.selected.value.route) {
                            inclusive = true
                        }
                    }
                    NavigationItem.selected.value = item
                }
                /*        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.inversePrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.inversePrimary
                        )*/
                // selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                // unselectedContentColor = MaterialTheme.colorScheme.inversePrimary
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
