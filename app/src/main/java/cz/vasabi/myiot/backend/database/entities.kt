package cz.vasabi.myiot.backend.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.vasabi.myiot.backend.connections.BaseDeviceCapability
import cz.vasabi.myiot.backend.connections.Device
import cz.vasabi.myiot.backend.connections.IpConnectionInfo

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
) : IpConnectionInfo

@Entity(primaryKeys = ["identifier", "name"])
data class HttpDeviceCapabilityEntity(
    @ColumnInfo val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val route: String,
    @ColumnInfo override val description: String,
    @ColumnInfo override val type: String
) : BaseDeviceCapability

@Entity
data class TcpDeviceConnectionEntity(
    @PrimaryKey override val identifier: String,
    @ColumnInfo override val host: String,
    @ColumnInfo override val port: Int,
    @ColumnInfo override val description: String?,
    @ColumnInfo override val name: String
) : IpConnectionInfo

@Entity(primaryKeys = ["identifier", "name"])
data class TcpDeviceCapabilityEntity(
    @ColumnInfo val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val route: String,
    @ColumnInfo override val description: String,
    @ColumnInfo override val type: String
) : BaseDeviceCapability

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