package cz.vasabi.myiot.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.nsd.NsdManager
import androidx.core.content.ContextCompat.getSystemService
import cz.vasabi.myiot.backend.DeviceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object injection {
    @Provides
    @Singleton
    fun provideClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }
    }

    @Provides
    @Singleton
    fun provideDeviceManager(): DeviceManager {
        return DeviceManager()
    }

    @Provides
    @Singleton
    fun provideNsdManager(@ApplicationContext ctx: Context): NsdManager {
        return ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext ctx: Context): BluetoothAdapter {
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }
}