package cz.vasabi.myiot.backend.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeviceDao {
    @Insert
    suspend fun insertAll(vararg devices: DeviceEntity)

    @Delete
    suspend fun deleteAll(vararg devices: DeviceEntity)

    @Update
    suspend fun updateAll(vararg devices: DeviceEntity)

    @Query("SELECT * FROM DeviceEntity")
    suspend fun getAll(): List<DeviceEntity>

    @Query("DELETE FROM DeviceEntity")
    suspend fun wipeTable()
}

@Dao
interface HttpConnectionsDao {
    @Insert
    suspend fun insertAll(vararg connections: HttpDeviceConnectionEntity)

    @Delete
    suspend fun deleteAll(vararg connections: HttpDeviceConnectionEntity)

    @Update
    suspend fun updateAll(vararg connections: HttpDeviceConnectionEntity)

    @Query("SELECT * FROM HttpDeviceConnectionEntity")
    suspend fun getAll(): List<HttpDeviceConnectionEntity>

    @Query("SELECT * FROM HttpDeviceConnectionEntity where identifier = :identifier")
    suspend fun findConnection(identifier: String): HttpDeviceConnectionEntity?

    @Query("DELETE FROM HttpDeviceConnectionEntity")
    suspend fun wipeTable()
}

@Dao
interface HttpCapabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg connections: HttpDeviceCapabilityEntity)

    @Delete
    suspend fun deleteAll(vararg connections: HttpDeviceCapabilityEntity)

    @Update
    suspend fun updateAll(vararg connections: HttpDeviceCapabilityEntity)

    @Query("SELECT * FROM HttpDeviceCapabilityEntity where identifier = :identifier")
    suspend fun findCapabilities(identifier: String): List<HttpDeviceCapabilityEntity>

    @Query("DELETE FROM HttpDeviceCapabilityEntity")
    suspend fun wipeTable()
}

@Dao
interface TcpConnectionsDao {
    @Insert
    suspend fun insertAll(vararg connections: TcpDeviceConnectionEntity)

    @Delete
    suspend fun deleteAll(vararg connections: TcpDeviceConnectionEntity)

    @Update
    suspend fun updateAll(vararg connections: TcpDeviceConnectionEntity)

    @Query("SELECT * FROM TcpDeviceConnectionEntity")
    suspend fun getAll(): List<TcpDeviceConnectionEntity>

    @Query("SELECT * FROM TcpDeviceConnectionEntity where identifier = :identifier")
    suspend fun findConnection(identifier: String): TcpDeviceConnectionEntity?

    @Query("DELETE FROM TcpDeviceConnectionEntity")
    suspend fun wipeTable()
}

@Dao
interface TcpCapabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg connections: TcpDeviceCapabilityEntity)

    @Delete
    suspend fun deleteAll(vararg connections: TcpDeviceCapabilityEntity)

    @Update
    suspend fun updateAll(vararg connections: TcpDeviceCapabilityEntity)

    @Query("SELECT * FROM TcpDeviceCapabilityEntity where identifier = :identifier")
    suspend fun findCapabilities(identifier: String): List<TcpDeviceCapabilityEntity>

    @Query("DELETE FROM TcpDeviceCapabilityEntity")
    suspend fun wipeTable()
}
