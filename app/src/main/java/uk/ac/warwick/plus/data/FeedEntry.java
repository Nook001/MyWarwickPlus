package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "feed_entries", primaryKeys = {"feed", "id"})
public class FeedEntry {
    public int feed;
    @NonNull public String id = "";
    @NonNull public String title = "";
    @NonNull public String text = "";
    @NonNull public String url = "";
    @NonNull public String provider = "";
    @NonNull public String type = "";
    public long dateMillis;
    public boolean html;
    @NonNull public String moduleCode = "";
    @NonNull public String academicYear = "";
    public int announcementCount;
    public int evaluationCount;
    public int position;
}
