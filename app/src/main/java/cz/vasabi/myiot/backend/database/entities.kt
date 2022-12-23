package cz.vasabi.myiot.backend.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DeviceEntity (
    @PrimaryKey val id: String,
    @ColumnInfo val description: String?
)
