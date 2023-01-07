package cz.vasabi.myiot.backend.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cz.vasabi.myiot.backend.connections.BaseCapabilityReading
import cz.vasabi.myiot.backend.connections.BaseDeviceCapability
import cz.vasabi.myiot.backend.connections.Device
import cz.vasabi.myiot.backend.connections.IpConnectionInfo
import java.time.Instant

@Entity
data class DeviceEntity(
    @PrimaryKey override val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val description: String?
): Device

@Entity(
    /*
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["identifier"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]

     */
)
data class HttpDeviceConnectionEntity(
    @PrimaryKey override val identifier: String,
    @ColumnInfo override val host: String,
    @ColumnInfo override val port: Int,
    @ColumnInfo override val description: String?,
    @ColumnInfo override val name: String
) : IpConnectionInfo

@Entity(
    primaryKeys = ["identifier", "name"],
    /*
    foreignKeys = [
        ForeignKey(
            entity = HttpDeviceConnectionEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["identifier"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]

     */
)
data class HttpDeviceCapabilityEntity(
    @ColumnInfo val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val route: String,
    @ColumnInfo override val description: String,
    @ColumnInfo override val type: String
) : BaseDeviceCapability

@Entity(
    /*
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["identifier"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]

     */
)
data class TcpDeviceConnectionEntity(
    @PrimaryKey override val identifier: String,
    @ColumnInfo override val host: String,
    @ColumnInfo override val port: Int,
    @ColumnInfo override val description: String?,
    @ColumnInfo override val name: String
) : IpConnectionInfo

@Entity(
    primaryKeys = ["identifier", "name"],
    /*
    foreignKeys = [
        ForeignKey(
            entity = TcpDeviceConnectionEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["identifier"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]

     */

)
data class TcpDeviceCapabilityEntity(
    @ColumnInfo val identifier: String,
    @ColumnInfo override val name: String,
    @ColumnInfo override val route: String,
    @ColumnInfo override val description: String,
    @ColumnInfo override val type: String
) : BaseDeviceCapability

@Entity(
    indices = [
        Index(
            value = ["identifier", "capabilityName", "connectionType"]
        )
    ]
)
data class CapabilityReadingEntity(
    @ColumnInfo override val identifier: String,

    @ColumnInfo override val capabilityName: String,
    @ColumnInfo override val connectionType: String,
    @ColumnInfo override val type: String,

    @ColumnInfo override val value: String,

    @PrimaryKey(autoGenerate = true) override val id: Int = 0,

    @ColumnInfo override val timestamp: Long = Instant.now().toEpochMilli()
) : BaseCapabilityReading


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