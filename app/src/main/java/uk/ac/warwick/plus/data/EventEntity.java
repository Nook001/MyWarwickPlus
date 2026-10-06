package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity implements EventContentItem {
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

    @Override public String getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public String getModule() { return module; }
    @Override public String getModuleName() { return moduleName; }
    @Override public String getLocation() { return location; }
    @Override public String getLocationUrl() { return locationUrl; }
    @Override public long getStartMillis() { return startMillis; }
    @Override public long getEndMillis() { return endMillis; }
    @Override public boolean getAllDay() { return allDay; }
    @Override public int getAcademicWeek() { return academicWeek; }
}
