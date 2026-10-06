package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "feed_entries", primaryKeys = {"feed", "id"})
public class FeedEntry implements FeedContentItem {
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

    @Override public int getFeed() { return feed; }
    @Override public String getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public String getText() { return text; }
    @Override public String getUrl() { return url; }
    @Override public String getProvider() { return provider; }
    @Override public String getType() { return type; }
    @Override public long getDateMillis() { return dateMillis; }
    @Override public boolean getHtml() { return html; }
    @Override public String getModuleCode() { return moduleCode; }
    @Override public String getAcademicYear() { return academicYear; }
    @Override public int getAnnouncementCount() { return announcementCount; }
    @Override public int getEvaluationCount() { return evaluationCount; }
    @Override public int getPosition() { return position; }
}
