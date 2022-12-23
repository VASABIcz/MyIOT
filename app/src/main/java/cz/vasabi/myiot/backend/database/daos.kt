package cz.vasabi.myiot.backend.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Optional

@Dao
interface DeviceDao {
    @Insert
    fun insertAll(vararg devices: DeviceEntity)

    @Delete
    fun deleteAll(vararg devices: DeviceEntity)

    @Update
    fun updateAll(vararg devices: DeviceEntity)

    @Query("SELECT * FROM DeviceEntity")
    fun getAll(): List<DeviceEntity>

    @Query("DELETE FROM DeviceEntity")
    fun wipeTable()
}

@Dao
interface HttpConnectionsDao {
    @Insert
    fun insertAll(vararg connections: HttpDeviceConnectionEntity)

    @Delete
    fun deleteAll(vararg connections: HttpDeviceConnectionEntity)

    @Update
    fun updateAll(vararg connections: HttpDeviceConnectionEntity)

    @Query("SELECT * FROM HttpDeviceConnectionEntity")
    fun getAll(): List<HttpDeviceConnectionEntity>

    @Query("SELECT * FROM HttpDeviceConnectionEntity where identifier = :identifier")
    fun findConnection(identifier: String): HttpDeviceConnectionEntity?

    @Query("DELETE FROM HttpDeviceConnectionEntity")
    fun wipeTable()
}

@Dao
interface HttpCapabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg connections: HttpDeviceCapabilityEntity)

    @Delete
    fun deleteAll(vararg connections: HttpDeviceCapabilityEntity)

    @Update
    fun updateAll(vararg connections: HttpDeviceCapabilityEntity)

    @Query("SELECT * FROM HttpDeviceCapabilityEntity where identifier = :identifier")
    fun findCapabilities(identifier: String): List<HttpDeviceCapabilityEntity>

    @Query("DELETE FROM HttpDeviceCapabilityEntity")
    fun wipeTable()
}
