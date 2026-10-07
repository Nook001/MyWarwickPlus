package uk.ac.warwick.plus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey override var id: String = "",
    override var title: String = "",
    override var module: String = "",
    @ColumnInfo(defaultValue = "''") override var moduleName: String = "",
    override var location: String = "",
    override var locationUrl: String = "",
    override var startMillis: Long = 0,
    override var endMillis: Long = 0,
    override var allDay: Boolean = false,
    override var academicWeek: Int = 0
) : EventContentItem
