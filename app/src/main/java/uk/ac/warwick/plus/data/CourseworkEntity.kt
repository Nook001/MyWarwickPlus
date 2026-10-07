package uk.ac.warwick.plus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coursework")
data class CourseworkEntity(
    @PrimaryKey override var id: String = "",
    override var title: String = "",
    override var description: String = "",
    override var url: String = "",
    override var dueMillis: Long = 0
) : CourseworkContentItem
