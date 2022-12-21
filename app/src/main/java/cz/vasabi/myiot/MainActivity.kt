package cz.vasabi.myiot

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.ResolveListener
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalDrawer
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fasterxml.jackson.annotation.JsonFormat
import cz.vasabi.myiot.ui.theme.MyIOTTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

fun intToIp(ip: Long): String {
    return "${(ip shr 24) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 8) and 0xff}.${ip and 0xff}"
}

object SingleState {
    val events = SnapshotStateList<String>()
    val currentDevice: MutableState<WholeFufuDevice?> = mutableStateOf(null)
}

class MainActivity : ComponentActivity() {
    companion object {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }
    }

    @SuppressLint("ServiceCast", "MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        val deviceManager = DeviceManager(client)
        super.onCreate(savedInstanceState)
        setContent {
            MyIOTTheme {
                // A surface container using the 'background' color from the theme
                val nav = rememberNavController()
                MainScreen(nav, deviceManager)
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
            is Data.B -> "bool"
            is Data.F -> "float"
            is Data.I -> "int"
            is Data.S -> "string"
        }

    val value: String
        get() = when(this) {
            is B -> this.b.toString()
            is F -> this.f.toString()
            is I -> this.i.toString()
            is S -> "\"${this.s}\""
        }
}

abstract class BetterCapability(val capability: FufuCapability) {
    val responses = Channel<Data>()

    abstract suspend fun setValue(value: Data)

    abstract suspend fun requestValue()
}

open class FufuCapability(val route: String, val name: String, val description: String, val type: String)

data class FufuDevice(val name: String, val capabilities: List<BetterCapability>)

data class WholeFufuDevice(val fufuDevice: FufuDevice, val info: NsdServiceInfo)

data class GenericResponse(@JsonFormat(shape = JsonFormat.Shape.STRING) val value: String, val type: String) {
    fun toData(): Data {
        return when(type) {
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

class DeviceManager(val httpClient: HttpClient) {
    val scope = CoroutineScope(Dispatchers.IO)
    companion object {
        val devices = SnapshotStateList<WholeFufuDevice>()
    }

    fun newDevice(info: NsdServiceInfo, onSuccess: (WholeFufuDevice) -> Unit = {}) {
        if (info.host.hostAddress == null) {
            return
        }
        scope.launch {
            // httpClient.get("http://${info.host.hostAddress}:${info.port}/api/info")
            val x: List<FufuCapability> = httpClient.get("http://${info.host.hostAddress}:${info.port}/api/capabilities").body()
            SingleState.events.add(x.toString())

            val new = x.map {
                object: BetterCapability(it) {
                    override suspend fun setValue(value: Data) {
                        try {
                            val res =
                                MainActivity.client.post("http://${info.host.hostAddress}:${info.port}${capability.route}") {
                                    setBody("{\"type\": \"${value.type}\", \"value\": ${value.value}}")
                                }
                            SingleState.events.add(res.toString())

                            val resValue: GenericResponse = res.body()
                            SingleState.events.add("received $resValue")
                            responses.send(resValue.toData())
                        }
                        catch (t: Throwable) {
                            t.printStackTrace()
                            SingleState.events.add(t.message ?: "error occured")
                        }
                    }

                    override suspend fun requestValue() {
                        try {
                            val res =
                                MainActivity.client.get("http://${info.host.hostAddress}:${info.port}${capability.route}")
                            val value: GenericResponse = res.body()
                            SingleState.events.add("received $value")
                            responses.send(value.toData())
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            SingleState.events.add(t.message ?: "error occured")
                        }
                    }
                }
            }
            val d = FufuDevice(info.serviceName, new)

            val e = WholeFufuDevice(d, info)
            devices.add(e)
            onSuccess(e)
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

@Composable
fun DebugPage() {
    LazyColumn {
        items(SingleState.events) { row ->
            Text(row)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun DiscoverPage(manager: DeviceManager) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val devices = remember {
        SnapshotStateList<NsdServiceInfo>()
    }
    var isRefreshing by remember {
        mutableStateOf(false)
    }
    var shouldFetch by remember {
        mutableStateOf(false)
    }

    val refreshState = rememberPullRefreshState(isRefreshing, {
        shouldFetch = !shouldFetch
    })

    LaunchedEffect(shouldFetch) {
        devices.clear()
        isRefreshing = true
        delay(300)
        val nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
        val manager = object: DeviceSearch(nsdManager) {
            override fun onServiceFound(service: NsdServiceInfo) {
                nsdManager.resolveService(service, object: ResolveListener {
                    override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
                        SingleState.events.add("failed to resolve service $p0")
                    }

                    override fun onServiceResolved(p0: NsdServiceInfo?) {
                        SingleState.events.add("resolved service $p0")
                        if (p0 == null) return
                        if (devices.find { it.serviceName == p0.serviceName } != null) return
                        devices.add(p0)
                    }

                })
                super.onServiceFound(service)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isRefreshing = false
                super.onDiscoveryStopped(serviceType)
            }
        }
        launch {
            delay(800)
            isRefreshing = false
        }
        // nsdManager.discoverServices("_services._dns-sd._udp", NsdManager.PROTOCOL_DNS_SD, manager)
        nsdManager.discoverServices("_iot._tcp", NsdManager.PROTOCOL_DNS_SD, manager)
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .pullRefresh(refreshState)
    ) {
        PullRefreshIndicator(refreshing = isRefreshing, state = refreshState)
        LazyColumn {
            itemsIndexed(devices) { i, it ->
                // Text("$i. Device: $it")
                if (DeviceManager.devices.find { dev ->
                        // TODO replace with UUID
                        dev.info.serviceName == it.serviceName
                    } != null) return@itemsIndexed

                DiscoveredDevice(it) { it ->
                    manager.newDevice(it) {
                        scope.launch {
                            Toast.makeText(ctx.applicationContext, "added device: ${it.fufuDevice.name} ${it.fufuDevice.capabilities.size} capabilities", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DiscoveredDevice(info: NsdServiceInfo, onAdd: (NsdServiceInfo) -> Unit) {
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
            Text(info.serviceName ?: "untitled", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("${info.host.hostAddress}:${info.port}", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        OutlinedButton(onClick = { onAdd(info) }, colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )) {
            Text(text = "Add")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawFufFuDevice(device: WholeFufuDevice, onCLick: () -> Unit) {
    Box(modifier = Modifier.padding(6.dp)) {
        if (device == SingleState.currentDevice.value) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                onClick = {
                    SingleState.currentDevice.value = device
                    onCLick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                // enabled = device == SingleState.currentDevice.value
            ) {
                Column {
                    Text(device.fufuDevice.name)
                    Text(text = ("id: " + device.info.attributes["identifier"]?.decodeToString()) ?: "failed to read")
                }
            }
        }
        else {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                onClick = {
                    SingleState.currentDevice.value = device
                    onCLick()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                // enabled = device == SingleState.currentDevice.value
            ) {
                Column {
                    Text(device.fufuDevice.name)
                    Text(text = ("id: " + device.info.attributes["identifier"]?.decodeToString()) ?: "failed to read")
                }
            }
        }
    }
}

@Composable
fun drawFufuCapability(capability: BetterCapability, device: WholeFufuDevice) {
    LaunchedEffect(null) {
        capability.requestValue()
    }

    Column(modifier = Modifier.padding(7.dp)) {
        when (capability.capability.type) {
            "bool" -> {
                BoolWidget(capability = capability, device = device)
                /*
                var isChecked by remember {
                    mutableStateOf(false)
                }

                LaunchedEffect(capability.responses, this) {
                    capability.responses.consumeAsFlow().collect {
                        when (it) {
                            is Data.B -> {
                                isChecked = it.b
                            }
                            else -> TODO()
                        }
                    }
                }

                Text(capability.capability.name)
                Text(capability.capability.description)
                Switch(checked = isChecked, onCheckedChange = {
                    isChecked = it
                    scope.launch {
                        capability.setValue(Data.B(it))
                        // device.setBool(it, capability.capability.name)
                    }
                })

                 */
            }
            "int" -> {
                IntWidget(capability, device)
                /*
                var value by remember {
                    mutableStateOf(0)
                }
                var unCommited by remember {
                    mutableStateOf("")
                }

                LaunchedEffect(this) {
                    // isChecked = device.getBool(capability.capability.name)
                    capability.responses.receiveAsFlow().collect {
                        when (it) {
                            is Data.I -> value = it.i
                            else -> TODO()
                        }
                    }
                }

                Text(capability.capability.name)
                Text(capability.capability.description)

                OutlinedTextField(
                    value = unCommited,
                    onValueChange = { unCommited = it },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
                )
                Button(onClick = {
                    scope.launch {
                        val num = unCommited.toIntOrNull() ?: return@launch
                        capability.setValue(Data.I(num))
                    }
                }) {
                    Text(text = "update")
                }
                Text(text = "value $value")
                 */
            }
            "string" -> {
                StringWidget(capability, device)
                /*
                var value by remember {
                    mutableStateOf("")
                }
                var unCommited by remember {
                    mutableStateOf("")
                }

                LaunchedEffect(this) {
                    // isChecked = device.getBool(capability.capability.name)
                    capability.responses.consumeAsFlow().collect {
                        when (it) {
                            is Data.S -> value = it.s
                            else -> TODO()
                        }
                    }
                }

                Text(capability.capability.name)
                Text(capability.capability.description)

                TextField(value = unCommited, onValueChange = {
                    unCommited = it
                }, textStyle = TextStyle(color = Color.White))
                Button(onClick = {
                    scope.launch {
                        capability.setValue(Data.S(unCommited))
                    }
                }) {
                    Text(text = "update")
                }
                Text(text = "value $value")
                */
            }
        }
    }
}

@Composable
fun DevicesPage() {
    if (SingleState.currentDevice.value == null) {
        Text(text = "no device selected")
        return
    }

    Column {
        SingleState.currentDevice.value?.let {device ->
            device.fufuDevice.capabilities.forEach {
                drawFufuCapability(it, device)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(nav: NavHostController, deviceManager: DeviceManager) {
    val scope = rememberCoroutineScope()
    Scaffold(
        bottomBar = {
            BottomNavigationBar(nav)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { it ->
        Box(Modifier.padding(it)) {
            NavHost(navController = nav, startDestination = "devices") {
                composable("devices") {
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    ModalDrawer(
                        drawerContent = {
                            LazyColumn {
                                items(DeviceManager.devices) {
                                    DrawFufFuDevice(it) {
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    }
                                }
                            }
                        },
                        drawerBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawerState = drawerState
                    ) {
                        DevicesPage()
                    }
                }
                composable("discover") {
                    DiscoverPage(deviceManager)
                }
                composable("settings") {
                    Text("settings")
                }
                composable("debug") {
                    DebugPage()
                }
            }
        }
    }
}

@Preview
@Composable
fun MainPreview() {
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    val nav = rememberNavController()
    BottomNavigationBar(nav)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyIOTTheme {
        Greeting("Android")
    }
}

open class DeviceSearch(private val nsdManager: NsdManager): NsdManager.DiscoveryListener {
    override fun onDiscoveryStarted(regType: String) {
        Log.d(TAG, "Service discovery started")
        SingleState.events.add("Service discovery started")
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        SingleState.events.add("Service found $service")
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        Log.e(TAG, "service lost: $service")
        SingleState.events.add("service lost: $service")
    }

    override fun onDiscoveryStopped(serviceType: String) {
        Log.i(TAG, "Discovery stopped: $serviceType")
        SingleState.events.add("Discovery stopped: $serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        SingleState.events.add("Discovery failed: Error code:$errorCode $serviceType")
        try {
            nsdManager.stopServiceDiscovery(this)
        }
        catch (e: Exception) {
            SingleState.events.add("nsdManager.onStartDiscoveryFailed ${e.message}")
        }
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        SingleState.events.add("Discovery failed: Error code:$errorCode $serviceType")
        try {
            nsdManager.stopServiceDiscovery(this)
        }
        catch (e: Exception) {
            SingleState.events.add("nsdManager.stopServiceDiscovery ${e.message}")
        }
    }
}