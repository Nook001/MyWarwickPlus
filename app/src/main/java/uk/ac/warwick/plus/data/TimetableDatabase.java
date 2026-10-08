package uk.ac.warwick.plus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EventEntity.class, SyncEntity.class, CourseworkEntity.class, FeedEntry.class, FeedMeta.class,
        ServiceSummaryEntity.class, CampusEventEntity.class, ServiceMeta.class}, version = 6, exportSchema = true)
public abstract class TimetableDatabase extends RoomDatabase {
    public abstract TimetableDao timetable();
}
