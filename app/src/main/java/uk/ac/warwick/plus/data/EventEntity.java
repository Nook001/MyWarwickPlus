package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String title = "";
    @NonNull public String module = "";
    @NonNull @ColumnInfo(defaultValue = "''") public String moduleName = "";
    @NonNull public String location = "";
    @NonNull public String locationUrl = "";
    public long startMillis;
    public long endMillis;
    public boolean allDay;
    public int academicWeek;
}
