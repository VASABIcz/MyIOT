package cz.vasabi.myiot

import android.annotation.SuppressLint
import android.app.Application
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fasterxml.jackson.annotation.JsonFormat
import cz.vasabi.myiot.pages.BottomNavigationBar
import cz.vasabi.myiot.pages.DevicesPage
import cz.vasabi.myiot.pages.DiscoverPage
import cz.vasabi.myiot.pages.MainPage
import cz.vasabi.myiot.ui.theme.MyIOTTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.channels.Channel


@HiltAndroidApp
class CoreApplication: Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("ServiceCast", "MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyIOTTheme {
                val nav = rememberNavController()
                MainPage(nav)
            }
        }
    }
}

sealed interface Data {
    class B(val b: Boolean): Data
    class F(val f: Float): Data
    class I(val i: Int): Data
    class S(val s: String): Data

    val type: String
        get() = when (this) {
            is B -> "bool"
            is F -> "float"
            is I -> "int"
            is S -> "string"
        }

    val value: String
        get() = when(this) {
            is B -> this.b.toString()
            is F -> this.f.toString()
            is I -> this.i.toString()
            is S -> "\"${this.s}\""
        }

    val jsonBody: String
        get() = "{\"type\": \"${type}\", \"value\": ${value}}"
}

data class GenericResponse(@JsonFormat(shape = JsonFormat.Shape.STRING) val value: String, val type: String) {
    fun toData(): Data {
        return when (type) {
            "bool" -> Data.B(value.toBoolean())
            "int" -> Data.I(value.toInt())
            "float" -> Data.F(value.toFloat())
            "string" -> Data.S(value)
            else -> {
                throw Exception("unknow data $this")
            }
        }
    }
}

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