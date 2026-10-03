package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "coursework")
public class CourseworkEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String title = "";
    @NonNull public String description = "";
    @NonNull public String url = "";
    public long dueMillis;
}
