package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "coursework")
public class CourseworkEntity implements CourseworkContentItem {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String title = "";
    @NonNull public String description = "";
    @NonNull public String url = "";
    public long dueMillis;

    @Override public String getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public String getDescription() { return description; }
    @Override public String getUrl() { return url; }
    @Override public long getDueMillis() { return dueMillis; }
}
