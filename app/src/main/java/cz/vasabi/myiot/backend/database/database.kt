package cz.vasabi.myiot.backend.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeviceEntity::class,
        HttpDeviceCapabilityEntity::class,
        HttpDeviceConnectionEntity::class,
        TcpDeviceCapabilityEntity::class,
        TcpDeviceConnectionEntity::class,
        CapabilityReadingEntity::class], version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun httpConnectionsDao(): HttpConnectionsDao
    abstract fun httpCapabilityDao(): HttpCapabilityDao

    abstract fun tcpConnectionsDao(): TcpConnectionsDao
    abstract fun tcpCapabilityDao(): TcpCapabilityDao
}
