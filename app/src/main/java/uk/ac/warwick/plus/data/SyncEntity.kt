package uk.ac.warwick.plus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sync_state")
data class SyncEntity(
    @PrimaryKey var id: Int = SyncSlots.TIMETABLE,
    var userCode: String = "",
    var displayName: String = "",
    @ColumnInfo(defaultValue = "''") var email: String = "",
    var syncedAt: Long = 0
)
