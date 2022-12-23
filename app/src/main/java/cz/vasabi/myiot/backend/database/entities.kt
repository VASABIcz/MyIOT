package cz.vasabi.myiot.backend.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.vasabi.myiot.backend.BaseDeviceCapability
import cz.vasabi.myiot.backend.Device
import cz.vasabi.myiot.backend.HttpConnectionInfo

@Entity
data class DeviceEntity(
    @PrimaryKey override val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val description: String?
): Device

@Entity
data class HttpDeviceConnectionEntity(
    @PrimaryKey override val identifier: String,
    @ColumnInfo override val host: String,
    @ColumnInfo override val port: Int,
    @ColumnInfo override val description: String?,
    @ColumnInfo override val name: String
): HttpConnectionInfo

@Entity(primaryKeys = ["identifier", "name"])
data class HttpDeviceCapabilityEntity(
    @ColumnInfo val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val route: String,
    @ColumnInfo override val description: String,
    @ColumnInfo override val type: String
): BaseDeviceCapability

/*

            DEVICE
           /      \
          /        \
         /          \
     HttpConn      BleConn
    /      \              \
   /        \              \
HttpCap1   HttpCap2       BleCap1

 */