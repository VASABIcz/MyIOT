package cz.vasabi.myiot.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.nsd.NsdManager
import androidx.room.Room
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cz.vasabi.myiot.backend.connections.DeviceManager
import cz.vasabi.myiot.backend.database.AppDatabase
import cz.vasabi.myiot.backend.discovery.DeviceResolveManager
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
object Injection {
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
    fun provideDeviceManager(
        db: AppDatabase,
        client: HttpClient,
        objectMapper: ObjectMapper
    ): DeviceManager {
        return DeviceManager(db, client, objectMapper)
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(
            ctx,
            AppDatabase::class.java, "app-database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideObjectMapper(): ObjectMapper {
        return ObjectMapper().registerModule(KotlinModule.Builder().build())
    }

    @Provides
    @Singleton
    fun provideDeviceResolveManager(nsdManager: NsdManager): DeviceResolveManager {
        return DeviceResolveManager(nsdManager)
    }
}